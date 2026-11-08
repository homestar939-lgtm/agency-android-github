package com.agency.app.feature_cashier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.data.auth.SessionState
import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.PaymentMethod
import com.agency.app.core.domain.model.Receipt
import com.agency.app.core.domain.repository.BuyerRepository
import com.agency.app.core.domain.repository.InvoiceRepository
import com.agency.app.core.domain.repository.ReceiptRepository
import com.agency.app.core.ui.arabic
import com.agency.app.core.ui.components.formatted
import com.agency.app.core.util.formattedDate
import com.agency.app.core.util.formattedDateTime
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/* ==========================================================================
 * شاشات المتحصّل الإضافية — الملف الشخصي، الإحصائيات، التنبيهات، البصمة.
 *
 * المبدأ هنا: لا رقم على الشاشة بلا مصدر. إحصائيات المتحصّل تُشتق من
 * السندات التي سجّلها هو فعلاً (بمقارنة `collectorUserId` بمعرّف الجلسة)،
 * لا من مجموع الوكالة — لأن «إنجازي» و«إنجاز الوكالة» رقمان مختلفان،
 * وخلطهما يمنح صورة كاذبة عن الأداء.
 * ========================================================================== */

/**
 * الملف الشخصي للمتحصّل.
 *
 * البيانات كلها من [AuthManager] الجلسة الحقيقية، وحدود السند من
 * [Receipt] نفسه — لا قيم مكرّرة في الشاشة، لأن تكرار رقم حدّ البصمة
 * في مكانين يعني أن أحدهما سيُنسى عند تغييره.
 */
data class CashierProfileUiState(
    val displayName: String = "",
    val username: String = "",
    val roleLabel: String = "",
    val phone: String? = null,
    val joinedAt: String = "",
    val lastLoginAt: String? = null,
    val storageEncrypted: Boolean = true,
    val mustChangePassword: Boolean = false,
    val biometricThresholdLabel: String = "",
    val isLoading: Boolean = true,
)

@HiltViewModel
class CashierProfileViewModel @Inject constructor(
    private val authManager: AuthManager,
) : ViewModel() {

    val ui: StateFlow<CashierProfileUiState> =
        combine(
            authManager.session,
            authManager.mustChangePassword,
        ) { session, mustChange ->
            val user = (session as? SessionState.Active)?.user
            CashierProfileUiState(
                displayName = user?.displayName ?: "غير مسجّل",
                username = user?.username ?: "",
                roleLabel = user?.role?.arabic() ?: "—",
                phone = user?.phone,
                joinedAt = user?.createdAt?.formattedDate() ?: "",
                lastLoginAt = user?.lastLoginAt?.formattedDateTime(),
                storageEncrypted = authManager.isStorageEncrypted,
                mustChangePassword = mustChange,
                biometricThresholdLabel = Receipt.BIOMETRIC_THRESHOLD.formatted(),
                isLoading = session is SessionState.Loading,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CashierProfileUiState(),
        )
}

/**
 * إحصائيات التحصيل الشخصية.
 *
 * تُقاس على السندات المسجّلة باسم المستخدم الحالي. الرقم الأول هو ما
 * جمعته أنت، لا ما جمعته الوكالة — وهذا فرق يُغيّر القرار: المتحصّل الذي
 * يعرف أنه جمع 12% من إجمالي الوكالة يطلب مساعدة، والذي يرى رقماً
 * إجمالياً يظن أنه أدّى واجبه.
 */
data class CashierStatsUiState(
    val myReceipts: Int = 0,
    val myCollected: Money = Money.ZERO,
    val agencyCollected: Money = Money.ZERO,
    val mySharePercent: Int = 0,
    val pendingInvoices: Int = 0,
    val approvedByMe: Int = 0,
    val unsyncedCount: Int = 0,
    val byMethod: Map<PaymentMethod, Money> = emptyMap(),
    val averageReceipt: Money = Money.ZERO,
    val isLoading: Boolean = true,
)

@HiltViewModel
class CashierStatsViewModel @Inject constructor(
    receiptRepository: ReceiptRepository,
    invoiceRepository: InvoiceRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val receipts = receiptRepository.observeReceipts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val invoices = invoiceRepository.observeInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ui: StateFlow<CashierStatsUiState> =
        combine(
            receipts,
            invoices,
            authManager.session,
        ) { receiptRows, invoiceRows, session ->
            val me = (session as? SessionState.Active)?.user?.id
            val mine = if (me == null) emptyList() else receiptRows.filter { it.collectorUserId == me }
            val mineTotal = mine.fold(Money.ZERO) { acc, r -> acc + r.amount }
            val agencyTotal = receiptRows.fold(Money.ZERO) { acc, r -> acc + r.amount }
            val share = if (agencyTotal.cents == 0L) 0
            else ((mineTotal.cents * 100L) / agencyTotal.cents).toInt()

            CashierStatsUiState(
                myReceipts = mine.size,
                myCollected = mineTotal,
                agencyCollected = agencyTotal,
                mySharePercent = share,
                pendingInvoices = invoiceRows.count { it.status == InvoiceStatus.PENDING_APPROVAL },
                approvedByMe = if (me == null) 0
                else invoiceRows.count {
                    it.isApproved && it.approvedByUserId == me
                },
                unsyncedCount = mine.count { !it.isSynced },
                byMethod = mine.groupBy { it.method }
                    .mapValues { (_, rows) -> rows.fold(Money.ZERO) { acc, r -> acc + r.amount } },
                averageReceipt = if (mine.isEmpty()) Money.ZERO
                else Money(mineTotal.cents / mine.size),
                isLoading = session is SessionState.Loading,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CashierStatsUiState(),
        )
}

/**
 * تنبيهات المتحصّل.
 *
 * ليست إشعارات نظام (لا يوجد خادم دفع بعد)، بل **مشتقّة من الحالة**:
 * فاتورة معتمدة لم تُحصَّل، سند غير مزامن، مشترٍ تجاوز سقفه. كل واحدة
 * تحمل سببها ووجهتها، لأن تنبيهاً بلا إجراء يُعلَّم مقروءاً ويُنسى.
 */
enum class CashierAlertKind { UNSYNCED, PENDING_APPROVAL, OVER_LIMIT }

data class CashierAlert(
    val kind: CashierAlertKind,
    val title: String,
    val detail: String,
    val severity: Int,
)

data class CashierAlertsUiState(
    val alerts: List<CashierAlert> = emptyList(),
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && alerts.isEmpty()
    val criticalCount: Int get() = alerts.count { it.severity >= 2 }
}

@HiltViewModel
class CashierAlertsViewModel @Inject constructor(
    receiptRepository: ReceiptRepository,
    invoiceRepository: InvoiceRepository,
    buyerRepository: BuyerRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val receipts = receiptRepository.observeReceipts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val invoices = invoiceRepository.observeInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val balances = buyerRepository.observeBalances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ui: StateFlow<CashierAlertsUiState> =
        combine(receipts, invoices, balances) { receiptRows, invoiceRows, balanceRows ->
            val alerts = mutableListOf<CashierAlert>()

            val unsynced = receiptRows.count { !it.isSynced }
            if (unsynced > 0) {
                alerts += CashierAlert(
                    kind = CashierAlertKind.UNSYNCED,
                    title = "$unsynced سند لم يُزامَن",
                    detail = "لو تعطّب الجهاز ستضيع هذه السندات. أعِد المزامنة.",
                    severity = 2,
                )
            }

            val pending = invoiceRows.count { it.status == InvoiceStatus.PENDING_APPROVAL }
            if (pending > 0) {
                alerts += CashierAlert(
                    kind = CashierAlertKind.PENDING_APPROVAL,
                    title = "$pending فاتورة بانتظار الاعتماد",
                    detail = "لا يمكن التحصيل على فاتورة غير معتمدة.",
                    severity = 1,
                )
            }

            balanceRows.filter { it.isOverLimit }.forEach { balance ->
                alerts += CashierAlert(
                    kind = CashierAlertKind.OVER_LIMIT,
                    title = "${balance.buyerName} تجاوز السقف",
                    detail = "المستحق ${balance.outstanding.formatted()} مقابل سقف ${balance.creditLimit.formatted()}",
                    severity = 2,
                )
            }

            CashierAlertsUiState(
                alerts = alerts.sortedByDescending { it.severity },
                isLoading = false,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CashierAlertsUiState(),
        )

    /* احتياط: أبقينا الاعتماد على الجلسة صريحاً حتى لا يُظن أن التنبيهات
       عامة لكل المستخدمين بينما هي لحالة الجهاز الحالية. */
    val currentUserId: String? get() = authManager.currentUser?.id
}

/**
 * بوابة البصمة — حالة الجاهزية فقط، لا التحقق نفسه.
 *
 * التحقق يحدث في [com.agency.app.core.ui.components.rememberBiometricGate]
 * على مستوى الـ Activity. ما تفعله هذه الشاشة أنها تقول للمستخدم: هل
 * جهازك قادر أصلاً؟ توفيراً لوقت من يحاول بصمةً غير موجودة.
 */
data class BiometricStatusUiState(
    val isSupported: Boolean = false,
    val thresholdLabel: String = "",
    val whyText: String = "",
)

@HiltViewModel
class BiometricStatusViewModel @Inject constructor() : ViewModel() {

    val ui: StateFlow<BiometricStatusUiState> = MutableStateFlow(
        BiometricStatusUiState(
            /*
             * القيمة `false` مقصودة ولا تُقاس من هنا: القياس الفعلي يحتاج
             * `Context` و`BiometricManager`، والشاشة تقرؤه من الطبقة
             * المكوّنة عند العرض. إعلان `true` بلا فحص كان سيكون كذباً.
             */
            isSupported = false,
            thresholdLabel = Receipt.BIOMETRIC_THRESHOLD.formatted(),
            whyText = "المبالغ التي تبلغ حدّ البصمة أو تتجاوزه تتطلّب تحققاً حيوياً " +
                "قبل تسجيل السند، ويُخزَّن على السند أنه موقَّع.",
        )
    ).asStateFlow()
}