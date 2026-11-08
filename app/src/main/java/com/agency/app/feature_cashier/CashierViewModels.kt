package com.agency.app.feature_cashier

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.domain.model.AppError
import com.agency.app.core.domain.model.Buyer
import com.agency.app.core.domain.model.BuyerBalance
import com.agency.app.core.domain.model.Invoice
import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.PaymentMethod
import com.agency.app.core.domain.repository.BuyerRepository
import com.agency.app.core.domain.repository.InvoiceRepository
import com.agency.app.core.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** قائمة فواتير الصندوق مع فلترة بالحالة. */
data class CashierInvoicesUiState(
    val invoices: List<Invoice> = emptyList(),
    val filter: InvoiceFilter = InvoiceFilter.PENDING,
    val counts: Map<InvoiceStatus, Int> = emptyMap(),
)

enum class InvoiceFilter(val label: String) {
    PENDING("بانتظار الاعتماد"),
    DRAFT("مسودات"),
    APPROVED("معتمدة"),
    REJECTED("مرفوضة"),
    ALL("الكل");

    val status: InvoiceStatus? get() = when (this) {
        PENDING -> InvoiceStatus.PENDING_APPROVAL
        DRAFT -> InvoiceStatus.DRAFT
        APPROVED -> InvoiceStatus.APPROVED
        REJECTED -> InvoiceStatus.REJECTED
        ALL -> null
    }
}

@HiltViewModel
class CashierInvoicesViewModel @Inject constructor(
    invoiceRepository: InvoiceRepository,
) : ViewModel() {

    private val allInvoices = invoiceRepository.observeInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _filter = MutableStateFlow(InvoiceFilter.PENDING)

    val ui: StateFlow<CashierInvoicesUiState> = combine(allInvoices, _filter) { rows, filter ->
        val target = filter.status
        CashierInvoicesUiState(
            invoices = if (target == null) rows else rows.filter { it.status == target },
            filter = filter,
            counts = InvoiceStatus.entries.associateWith { status -> rows.count { it.status == status } },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CashierInvoicesUiState())

    fun setFilter(value: InvoiceFilter) {
        _filter.value = value
    }
}

/**
 * تفاصيل فاتورة وإجراء الاعتماد أو الرفض.
 *
 * الاعتماد مقيَّد بدور يملك `canWriteFinancials`، والفحص يجري في طبقة
 * البيانات أيضاً عبر [InvoiceRepository.approve] — الشاشة تشرح فقط
 * سبب الرفض حين يعود.
 */
data class InvoiceDetailUiState(
    val invoice: Invoice? = null,
    val isWorking: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

@HiltViewModel
class CashierInvoiceDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val invoiceRepository: InvoiceRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val invoiceId: String = savedStateHandle.get<String>("invoiceId").orEmpty()

    private val _state = MutableStateFlow(InvoiceDetailUiState())
    val ui: StateFlow<InvoiceDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            invoiceRepository.observeInvoice(invoiceId).collect { invoice ->
                _state.value = _state.value.copy(invoice = invoice)
            }
        }
    }

    fun approve() {
        val user = authManager.currentUser ?: return
        if (_state.value.isWorking) return
        _state.value = _state.value.copy(isWorking = true, error = null, notice = null)
        viewModelScope.launch {
            when (val result = invoiceRepository.approve(invoiceId, user.id)) {
                is Outcome.Success -> _state.value = _state.value.copy(
                    isWorking = false,
                    notice = "اعتُمدت الفاتورة ${result.value.invoiceNumber} وأُضيفت إلى ذمة المشتري",
                )
                is Outcome.Failure -> _state.value = _state.value.copy(
                    isWorking = false,
                    error = describe(result.error),
                )
            }
        }
    }

    fun reject(reason: String) {
        val user = authManager.currentUser ?: return
        if (reason.isBlank()) {
            _state.value = _state.value.copy(error = "اذكر سبب الرفض")
            return
        }
        if (_state.value.isWorking) return
        _state.value = _state.value.copy(isWorking = true, error = null, notice = null)
        viewModelScope.launch {
            when (val result = invoiceRepository.reject(invoiceId, user.id, reason)) {
                is Outcome.Success -> _state.value = _state.value.copy(
                    isWorking = false,
                    notice = "رُفضت الفاتورة وأُعيدت للمسودة",
                )
                is Outcome.Failure -> _state.value = _state.value.copy(
                    isWorking = false,
                    error = describe(result.error),
                )
            }
        }
    }

    fun dismissMessages() {
        _state.value = _state.value.copy(error = null, notice = null)
    }

    private fun describe(error: AppError): String = when (error) {
        is AppError.Permission -> error.required
        is AppError.Conflict -> error.reason
        is AppError.Validation -> "${error.field}: ${error.reason}"
        is AppError.NotFound -> "الفاتورة غير موجودة"
        is AppError.Network -> "تعذّر الاتصال: ${error.reason}"
        is AppError.Unknown -> "خطأ غير متوقع، أعد المحاولة"
    }
}

/**
 * شاشة التحصيل.
 *
 * كل سند يمرّ عبر [ReceiptRepository.collect]، وهي المسار الوحيد لتخفيض
 * ذمّة المشتري. المبالغ فوق [com.agency.app.core.domain.model.Receipt.BIOMETRIC_THRESHOLD]
 * تستلزم بصمة قبل الإرسال، والقرار يُتخذ في الواجهة ثم يُسجَّل على السند.
 */
data class CollectUiState(
    val buyers: List<Buyer> = emptyList(),
    val balances: Map<String, BuyerBalance> = emptyMap(),
    val selectedBuyerId: String? = null,
    val amountText: String = "",
    val method: PaymentMethod = PaymentMethod.CASH,
    val reference: String = "",
    val note: String = "",
    val isCollecting: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val requiresBiometric: Boolean = false,
)

@HiltViewModel
class CollectViewModel @Inject constructor(
    buyerRepository: BuyerRepository,
    private val receiptRepository: ReceiptRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val _state = MutableStateFlow(CollectUiState())
    val ui: StateFlow<CollectUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                buyerRepository.observeBuyers(),
                buyerRepository.observeBalances(),
            ) { buyers, balances ->
                buyers.filter { it.isActive } to balances.associateBy { it.buyerId }
            }.collect { (buyers, balanceMap) ->
                _state.value = _state.value.copy(buyers = buyers, balances = balanceMap)
            }
        }
    }

    fun selectBuyer(buyerId: String) {
        _state.value = _state.value.copy(selectedBuyerId = buyerId, error = null)
    }

    /** يحدّث المبلغ ويعيد حساب شرط البصمة مباشرةً. */
    fun onAmountChange(value: String) {
        val parsed = com.agency.app.core.domain.model.Money.fromString(value)
        _state.value = _state.value.copy(
            amountText = value,
            requiresBiometric = parsed != null &&
                parsed.cents >= com.agency.app.core.domain.model.Receipt.BIOMETRIC_THRESHOLD.cents,
        )
    }

    fun onMethodChange(value: PaymentMethod) {
        _state.value = _state.value.copy(method = value)
    }

    fun onReferenceChange(value: String) {
        _state.value = _state.value.copy(reference = value)
    }

    fun onNoteChange(value: String) {
        _state.value = _state.value.copy(note = value)
    }

    /** ينفّذ التحصيل — لا يُستدعى إلا بعد نجاح البصمة إن كانت مطلوبة. */
    fun collect(biometricSatisfied: Boolean) {
        if (_state.value.isCollecting) return
        val user = authManager.currentUser ?: return
        val buyerId = _state.value.selectedBuyerId
        if (buyerId.isNullOrBlank()) {
            _state.value = _state.value.copy(error = "اختر المشتري أولاً")
            return
        }
        val amount = com.agency.app.core.domain.model.Money.fromString(_state.value.amountText)
        if (amount == null || !amount.isPositive) {
            _state.value = _state.value.copy(error = "المبلغ يجب أن يكون رقماً أكبر من صفر")
            return
        }
        if (_state.value.requiresBiometric && !biometricSatisfied) {
            _state.value = _state.value.copy(error = "المبلغ يتجاوز حد البصمة؛ يجب التحقق بالبصمة أولاً")
            return
        }

        _state.value = _state.value.copy(isCollecting = true, error = null, notice = null)
        viewModelScope.launch {
            when (
                val result = receiptRepository.collect(
                    buyerId = buyerId,
                    amount = amount,
                    method = _state.value.method,
                    collectorUserId = user.id,
                    collectorName = user.displayName,
                    reference = _state.value.reference.ifBlank { null },
                    note = _state.value.note.ifBlank { null },
                    isSigned = biometricSatisfied,
                    signaturePath = if (biometricSatisfied) "biometric" else null,
                )
            ) {
                is Outcome.Success -> _state.value = CollectUiState(
                    buyers = _state.value.buyers,
                    balances = _state.value.balances,
                    notice = "سُجِّل سند القبض ${result.value.receiptNumber} بمبلغ ${amount} وخصم من الذمة",
                )
                is Outcome.Failure -> _state.value = _state.value.copy(
                    isCollecting = false,
                    error = describe(result.error),
                )
            }
        }
    }

    fun dismissMessages() {
        _state.value = _state.value.copy(error = null, notice = null)
    }

    private fun describe(error: AppError): String = when (error) {
        is AppError.Permission -> error.required
        is AppError.Conflict -> error.reason
        is AppError.Validation -> "${error.field}: ${error.reason}"
        is AppError.NotFound -> "المشتري غير موجود"
        is AppError.Network -> "تعذّر الاتصال: ${error.reason}"
        is AppError.Unknown -> "خطأ غير متوقع، أعد المحاولة"
    }
}