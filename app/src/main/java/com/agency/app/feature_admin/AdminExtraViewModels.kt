package com.agency.app.feature_admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.data.auth.SecureStore
import com.agency.app.core.domain.model.BuyerBalance
import com.agency.app.core.domain.model.Invoice
import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.Order
import com.agency.app.core.domain.model.OrderStatus
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.PermissionRequest
import com.agency.app.core.domain.model.Receipt
import com.agency.app.core.domain.model.User
import com.agency.app.core.domain.model.UserRole
import com.agency.app.core.domain.repository.BuyerRepository
import com.agency.app.core.domain.repository.InvoiceRepository
import com.agency.app.core.domain.repository.OrderRepository
import com.agency.app.core.domain.repository.PermissionRepository
import com.agency.app.core.domain.repository.ReceiptRepository
import com.agency.app.core.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * سجل التدقيق.
 *
 * قرار مهم: لا يوجد جدول تدقيق في القاعدة، ولم أُنشئ واحداً الآن.
 * شرط أي سجل تدقيق أن يكون **غير قابل للتعديل من التطبيق نفسه**، وإلا
 * استطاع من يملك الحذف أن يمحو أثره. الجدول في Room قابل للمسح من الشاشة،
 * فبناؤه الآن كان سيُنتج سجلاً يُوهم بالرقابة وهو قابل للتحرير.
 *
 * المعروض هنا إذن **مشتق من البيانات نفسها**: من أنشأ الفاتورة ومن اعتمدها
 * ومتى، ومن حرّك الطلبية، ومن جمع السند. هذه آثار حقيقية محفوظة في الجداول
 * الفعلية، ولا يمكن محوها دون محو العمل نفسه — وهو تعريف أدق للتدقيق.
 *
 * المرحلة التالية (خارج هذه الدفعة): جدول append-only مع تصدير خارج الجهاز.
 */
data class AuditEvent(
    val at: kotlinx.datetime.Instant,
    val actor: String,
    val action: String,
    val subject: String,
    val detail: String?,
    val category: AuditCategory,
)

enum class AuditCategory(val label: String) {
    INVOICE("فواتير"),
    ORDER("طلبيات"),
    RECEIPT("سندات"),
    PERMISSION("صلاحيات"),
}

data class AuditLogUiState(
    val events: List<AuditEvent> = emptyList(),
    val category: AuditCategory? = null,
    val isLoading: Boolean = true,
) {
    val visible: List<AuditEvent>
        get() = events.filter { category == null || it.category == category }

    val isEmpty: Boolean get() = !isLoading && visible.isEmpty()
}

@HiltViewModel
class AuditLogViewModel @Inject constructor(
    invoiceRepository: InvoiceRepository,
    orderRepository: OrderRepository,
    receiptRepository: ReceiptRepository,
    permissionRepository: PermissionRepository,
) : ViewModel() {

    private val categoryFilter = MutableStateFlow<AuditCategory?>(null)
    private val _ui = MutableStateFlow(AuditLogUiState())
    val ui: StateFlow<AuditLogUiState> = _ui.asStateFlow()

    private val invoices = invoiceRepository.observeInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val orders = orderRepository.observeOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val receipts = receiptRepository.observeReceipts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val permissions = permissionRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            combine(invoices, orders, receipts, permissions) { inv, ord, rec, per ->
                buildEvents(inv, ord, rec, per)
            }.combine(categoryFilter) { events, filter ->
                AuditLogUiState(events = events, category = filter, isLoading = false)
            }.collect { _ui.value = it }
        }
    }

    private fun buildEvents(
        invoiceRows: List<Invoice>,
        orderRows: List<Order>,
        receiptRows: List<Receipt>,
        permissionRows: List<PermissionRequest>,
    ): List<AuditEvent> {
        val events = mutableListOf<AuditEvent>()

        invoiceRows.forEach { invoice ->
            events += AuditEvent(
                at = invoice.createdAt,
                actor = invoice.createdByUserId,
                action = "أنشأ فاتورة",
                subject = invoice.invoiceNumber,
                detail = "${invoice.buyerName} · ${invoice.grandTotal}",
                category = AuditCategory.INVOICE,
            )
            if (invoice.isApproved && invoice.approvedAt != null) {
                events += AuditEvent(
                    at = invoice.approvedAt,
                    actor = invoice.approvedByUserId ?: "غير معروف",
                    action = "اعتمد فاتورة",
                    subject = invoice.invoiceNumber,
                    detail = invoice.buyerName,
                    category = AuditCategory.INVOICE,
                )
            }
            if (invoice.status == InvoiceStatus.REJECTED) {
                events += AuditEvent(
                    at = invoice.updatedAt,
                    actor = invoice.approvedByUserId ?: "غير معروف",
                    action = "رفض فاتورة",
                    subject = invoice.invoiceNumber,
                    detail = invoice.rejectionReason,
                    category = AuditCategory.INVOICE,
                )
            }
        }

        orderRows.forEach { order ->
            /*
             * سجل الطلبية من الـ timeline نفسه لا من حقل واحد: الطلبية تمر
             * بأربع حالات، ولكلٍّ فاعلها ووقتها. أخذ الحالة الأخيرة وحدها كان
             * سيُسقط أثر من حرّكها فعلاً.
             */
            order.timeline.forEach { entry ->
                events += AuditEvent(
                    at = entry.at,
                    actor = entry.actorName,
                    action = "نقل الطلبية إلى ${entry.status.name}",
                    subject = order.orderNumber,
                    detail = entry.note,
                    category = AuditCategory.ORDER,
                )
            }
        }

        receiptRows.forEach { receipt ->
            events += AuditEvent(
                at = receipt.collectedAt,
                actor = receipt.collectorName,
                action = "سجّل سند قبض",
                subject = receipt.receiptNumber,
                detail = "${receipt.buyerName} · ${receipt.amount}",
                category = AuditCategory.RECEIPT,
            )
        }

        permissionRows.forEach { request ->
            events += AuditEvent(
                at = request.createdAt,
                actor = request.requesterName,
                action = "طلب صلاحية",
                subject = request.action,
                detail = request.reason,
                category = AuditCategory.PERMISSION,
            )
            if (!request.isPending && request.reviewedAt != null) {
                events += AuditEvent(
                    at = request.reviewedAt,
                    actor = request.reviewedByName ?: "غير معروف",
                    action = if (request.isApproved) "اعتمد طلب صلاحية" else "رفض طلب صلاحية",
                    subject = request.action,
                    detail = request.reviewNote,
                    category = AuditCategory.PERMISSION,
                )
            }
        }

        return events.sortedByDescending { it.at }
    }

    fun setCategory(value: AuditCategory?) {
        categoryFilter.value = value
    }
}

/**
 * إعدادات الوكالة.
 *
 * ما يُحفظ هنا نطاق تشغيلي لا بيانات مالية: اسم الوكالة، الهاتف، نسبة العمولة
 * الافتراضية، وسقف الاعتماد المالي. تُحفظ في [SecureStore] لأنه التخزين الوحيد
 * الموجود والمشفَّر — ووضعُها في قاعدة البيانات كان سيتطلّب `migration`
 * وترقية مخطط لأربعة حقول نصية، وهو ثمن أعلى من نفعه اليوم.
 *
 * والصراحة الواجبة: هذه القيم **تُحفظ وتُعرض ولا تؤثر بعد على منطق الفوترة**.
 * نسبة العمولة تُطبَّق حالياً من وسيط الشاشة لا من هنا. الشاشة تقول ذلك
 * بنصّها، فلا يعتقد المدير أنه غيّر سلوكاً بينما غيّر نصاً.
 */
data class AgencySettings(
    val agencyName: String = "وكالة الأمين",
    val phone: String = "",
    val address: String = "",
    val defaultCommissionPercent: Int = 2,
    val creditWarningPercent: Int = 80,
) {
    val isValid: Boolean
        get() = agencyName.isNotBlank() &&
            defaultCommissionPercent in 0..100 &&
            creditWarningPercent in 1..100
}

data class SettingsUiState(
    val settings: AgencySettings = AgencySettings(),
    val isSaving: Boolean = false,
    val notice: String? = null,
    val error: String? = null,
    val storageEncrypted: Boolean = true,
)

@HiltViewModel
class AgencySettingsViewModel @Inject constructor(
    private val store: SecureStore,
) : ViewModel() {

    private val _ui = MutableStateFlow(SettingsUiState())
    val ui: StateFlow<SettingsUiState> = _ui.asStateFlow()

    init {
        _ui.value = SettingsUiState(
            settings = load(),
            storageEncrypted = store.isEncrypted,
        )
    }

    private fun load(): AgencySettings = AgencySettings(
        agencyName = store.getString(KEY_NAME) ?: AgencySettings().agencyName,
        phone = store.getString(KEY_PHONE) ?: "",
        address = store.getString(KEY_ADDRESS) ?: "",
        defaultCommissionPercent = store.getLong(KEY_COMMISSION, 2L).toInt(),
        creditWarningPercent = store.getLong(KEY_CREDIT, 80L).toInt(),
    )

    fun update(transform: (AgencySettings) -> AgencySettings) {
        _ui.value = _ui.value.copy(
            settings = transform(_ui.value.settings),
            notice = null,
            error = null,
        )
    }

    fun save() {
        val current = _ui.value.settings
        if (!current.isValid) {
            _ui.value = _ui.value.copy(
                error = "نسبة العمولة يجب أن تكون بين 0 و100، وتحذير الحد الائتماني بين 1 و100"
            )
            return
        }
        _ui.value = _ui.value.copy(isSaving = true, error = null)
        runCatching {
            store.putString(KEY_NAME, current.agencyName)
            store.putString(KEY_PHONE, current.phone)
            store.putString(KEY_ADDRESS, current.address)
            store.putLong(KEY_COMMISSION, current.defaultCommissionPercent.toLong())
            store.putLong(KEY_CREDIT, current.creditWarningPercent.toLong())
        }.onSuccess {
            _ui.value = _ui.value.copy(
                isSaving = false,
                notice = "حُفظت الإعدادات على هذا الجهاز",
            )
        }.onFailure {
            _ui.value = _ui.value.copy(
                isSaving = false,
                error = "تعذّر الحفظ: ${it.message ?: "خطأ غير معروف"}",
            )
        }
    }

    private companion object {
        const val KEY_NAME = "agency.name"
        const val KEY_PHONE = "agency.phone"
        const val KEY_ADDRESS = "agency.address"
        const val KEY_COMMISSION = "agency.commission"
        const val KEY_CREDIT = "agency.creditWarn"
    }
}

/**
 * مصفوفة الصلاحيات — الحقيقة من `UserRole` لا من جدول مكتوب في الشاشة.
 *
 * الشاشة تُولّد المصفوفة من `UserRole.entries` وتقرأ خصائص الدور الفعلية
 * (`canWriteFinancials`, `isReadOnly`). لو كُتبت المصفوفة يدوياً لتألّقت
 * في العرض وتباعدت عن الكود عند أول تعديل — وهي أسوأ من غيابها: وثيقة
 * تقول صلاحية لا يملكها الكود.
 */
data class PermissionMatrixRow(
    val role: UserRole,
    val canWriteFinancials: Boolean,
    val isReadOnly: Boolean,
    val usersCount: Int,
)

data class PermissionMatrixUiState(
    val rows: List<PermissionMatrixRow> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class AdminPermissionMatrixViewModel @Inject constructor(
    userRepository: UserRepository,
) : ViewModel() {

    val ui: StateFlow<PermissionMatrixUiState> = userRepository.observeUsers()
        .map { users ->
            val active = users.filter { it.isActive }
            PermissionMatrixUiState(
                rows = UserRole.entries.map { role ->
                    PermissionMatrixRow(
                        role = role,
                        canWriteFinancials = role.canWriteFinancials,
                        isReadOnly = role.isReadOnly,
                        usersCount = active.count { it.role == role },
                    )
                },
                isLoading = false,
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            PermissionMatrixUiState()
        )
}

/**
 * النسخ الاحتياطي — فحص حالة البيانات وإرشاد، لا «زر نسخ» وهمي.
 *
 * لا أستطيع نسخ قاعدة Room إلى ملف ثم ادّعاء أنه نسخ احتياطي آمن: النسخة
 * تحتاج تشفيراً ومفتاحاً مستقلاً ومساحة خارجية، وكلها خارج نطاق هذا الإصدار.
 * الشاشة تقيس **جاهزية** البيانات للنسخ: حجم السجلات، تناسق الأرصدة، وعدد
 * السندات غير المزامَنة التي ستضيع لو تعطّف الجهاز الآن. تقرير حالة صادق
 * أنفع من زر يوهم بالأمان.
 */
data class BackupCheckItem(
    val label: String,
    val value: String,
    val ok: Boolean,
)

data class BackupUiState(
    val items: List<BackupCheckItem> = emptyList(),
    val unsyncedReceipts: Int = 0,
    val duplicates: Int = 0,
    val isLoading: Boolean = true,
) {
    val isHealthy: Boolean get() = items.all { it.ok }
    val hasLossRisk: Boolean get() = unsyncedReceipts > 0
}

@HiltViewModel
class BackupStatusViewModel @Inject constructor(
    userRepository: UserRepository,
    buyerRepository: BuyerRepository,
    invoiceRepository: InvoiceRepository,
    receiptRepository: ReceiptRepository,
    orderRepository: OrderRepository,
) : ViewModel() {

    private val users = userRepository.observeUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val balances = buyerRepository.observeBalances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val invoices = invoiceRepository.observeInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val receipts = receiptRepository.observeReceipts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val orders = orderRepository.observeOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ui: StateFlow<BackupUiState> =
        combine(users, balances, invoices, receipts, orders) { u, b, i, r, o ->
            val approved = i.filter { it.status == InvoiceStatus.APPROVED }
            val invoiced = approved.fold(Money.ZERO) { acc, inv -> acc + inv.grandTotal }
            val paid = r.fold(Money.ZERO) { acc, rec -> acc + rec.amount }
            val outstanding = b.fold(Money.ZERO) { acc, bal -> acc + bal.outstanding }
            val unsynced = r.count { !it.isSynced }

            /*
             * فحص التناسق: مجموع الذمم القائمة يجب أن يساوي (المفوتر − المدفوع).
             * إن اختلفا فالقاعدة تحمل رصيداً لا يطابق عملياته، وهي أخطر حالة
             * يمكن أن تُنسخ احتياطياً بلا إنذار.
             */
            val expected = invoiced - paid
            val consistent = expected == outstanding

            BackupUiState(
                items = listOf(
                    BackupCheckItem("المستخدمون", "${u.size}", u.isNotEmpty()),
                    BackupCheckItem("المشترون", "${b.size}", b.isNotEmpty()),
                    BackupCheckItem("الطلبيات", "${o.size}", true),
                    BackupCheckItem("الفواتير", "${i.size}", true),
                    BackupCheckItem("السندات", "${r.size}", true),
                    BackupCheckItem(
                        label = "تناسق الأرصدة",
                        value = if (consistent) "متطابقة" else "اختلاف: ${outstanding} ≠ $expected",
                        ok = consistent,
                    ),
                    BackupCheckItem(
                        label = "سندات غير مزامنة",
                        value = if (unsynced == 0) "لا يوجد" else "$unsynced سند",
                        ok = unsynced == 0,
                    ),
                ),
                unsyncedReceipts = unsynced,
                duplicates = 0,
                isLoading = false,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupUiState())
}

/** إعدادات السند: ما يُطبع على سند القبض وسلوك التحقق الحيوي. */
data class ReceiptSettings(
    val showAgencyStamp: Boolean = true,
    val requireBiometricOverThreshold: Boolean = true,
    val footerNote: String = "شكراً لتعاملكم معنا",
    val duplicatePrintWarning: Boolean = true,
)

data class ReceiptSettingsUiState(
    val settings: ReceiptSettings = ReceiptSettings(),
    val notice: String? = null,
)

@HiltViewModel
class ReceiptSettingsViewModel @Inject constructor(
    private val store: SecureStore,
) : ViewModel() {

    private val _ui = MutableStateFlow(
        ReceiptSettingsUiState(
            settings = ReceiptSettings(
                showAgencyStamp = store.getBoolean(KEY_STAMP, true),
                requireBiometricOverThreshold = store.getBoolean(KEY_BIOMETRIC, true),
                footerNote = store.getString(KEY_FOOTER) ?: "شكراً لتعاملكم معنا",
                duplicatePrintWarning = store.getBoolean(KEY_DUP, true),
            )
        )
    )
    val ui: StateFlow<ReceiptSettingsUiState> = _ui.asStateFlow()

    fun toggleStamp() = persist { copy(showAgencyStamp = !showAgencyStamp) }
    fun toggleBiometric() = persist { copy(requireBiometricOverThreshold = !requireBiometricOverThreshold) }
    fun toggleDuplicateWarning() = persist { copy(duplicatePrintWarning = !duplicatePrintWarning) }
    fun onFooterChange(value: String) = persist { copy(footerNote = value) }

    private fun persist(transform: ReceiptSettings.() -> ReceiptSettings) {
        val next = _ui.value.settings.transform()
        store.putBoolean(KEY_STAMP, next.showAgencyStamp)
        store.putBoolean(KEY_BIOMETRIC, next.requireBiometricOverThreshold)
        store.putString(KEY_FOOTER, next.footerNote)
        store.putBoolean(KEY_DUP, next.duplicatePrintWarning)
        _ui.value = ReceiptSettingsUiState(settings = next, notice = "حُفظ")
    }

    private companion object {
        const val KEY_STAMP = "receipt.stamp"
        const val KEY_BIOMETRIC = "receipt.biometric"
        const val KEY_FOOTER = "receipt.footer"
        const val KEY_DUP = "receipt.duplicate"
    }
}