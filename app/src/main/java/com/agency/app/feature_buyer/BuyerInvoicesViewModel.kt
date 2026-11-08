package com.agency.app.feature_buyer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.domain.model.BuyerBalance
import com.agency.app.core.domain.model.Invoice
import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.domain.repository.BuyerRepository
import com.agency.app.core.domain.repository.InvoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** تبويب قائمة فواتير المشتري. */
enum class BuyerInvoiceFilter(val label: String) {
    ALL("الكل"),
    PENDING("بانتظار الاعتماد"),
    APPROVED("معتمدة"),
    ARCHIVED("مؤرشفة");

    val status: InvoiceStatus?
        get() = when (this) {
            ALL -> null
            PENDING -> InvoiceStatus.PENDING_APPROVAL
            APPROVED -> InvoiceStatus.APPROVED
            ARCHIVED -> InvoiceStatus.ARCHIVED
        }
}

data class BuyerInvoicesUiState(
    val invoices: List<Invoice> = emptyList(),
    val filter: BuyerInvoiceFilter = BuyerInvoiceFilter.ALL,
    val balance: BuyerBalance? = null,
    val counts: Map<InvoiceStatus, Int> = emptyMap(),
) {
    /** إجمالي ما لم يُسَدَّد بعد من الفواتير المعتمدة. */
    val approvedTotalCents: Long
        get() = invoices.filter { it.isApproved }.sumOf { it.grandTotal.cents }
}

/**
 * فواتير المشتري.
 *
 * المسودات غير المعتمدة تُعرض هنا أيضاً لأن المشتري يحتاج أن يعرف أن هناك
 * فاتورة قيد الإعداد، لكن [Invoice.isApproved] هو ما يحدد ما يدخل الذمّة.
 * الشاشة لا تحسب الذمة بنفسها بل تقرأ [BuyerBalance] المشتق في طبقة البيانات.
 */
@HiltViewModel
class BuyerInvoicesViewModel @Inject constructor(
    invoiceRepository: InvoiceRepository,
    buyerRepository: BuyerRepository,
    authManager: AuthManager,
) : ViewModel() {

    private val buyerId: String = authManager.currentUser?.buyerId.orEmpty()

    private val allInvoices = if (buyerId.isBlank()) {
        MutableStateFlow(emptyList<Invoice>())
    } else {
        invoiceRepository.observeByBuyer(buyerId)
    }

    private val balance = if (buyerId.isBlank()) {
        MutableStateFlow(null)
    } else {
        buyerRepository.observeBalance(buyerId)
    }

    private val _filter = MutableStateFlow(BuyerInvoiceFilter.ALL)

    val ui: StateFlow<BuyerInvoicesUiState> = combine(
        allInvoices, _filter, balance
    ) { rows, filter, bal ->
        val target = filter.status
        BuyerInvoicesUiState(
            invoices = if (target == null) rows else rows.filter { it.status == target },
            filter = filter,
            balance = bal,
            counts = InvoiceStatus.entries.associateWith { status ->
                rows.count { it.status == status }
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BuyerInvoicesUiState())

    fun setFilter(value: BuyerInvoiceFilter) {
        _filter.value = value
    }
}