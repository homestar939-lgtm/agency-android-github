package com.agency.app.feature_buyer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.domain.model.BuyerBalance
import com.agency.app.core.domain.model.BuyerStatementEntry
import com.agency.app.core.domain.model.Invoice
import com.agency.app.core.domain.repository.BuyerRepository
import com.agency.app.core.domain.repository.InvoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * كشف حساب المشتري.
 *
 * الرصيد مشتق في طبقة البيانات من «المعتمد من الفواتير ناقص السندات»،
 * ولا يُحتفظ به كرقم مخزَّن هنا، فلا يمكن أن يفترق الرصيد عن مستنداته.
 */
data class StatementUiState(
    val entries: List<BuyerStatementEntry> = emptyList(),
    val balance: BuyerBalance? = null,
    val invoices: List<Invoice> = emptyList(),
)

@HiltViewModel
class BuyerStatementViewModel @Inject constructor(
    buyerRepository: BuyerRepository,
    invoiceRepository: InvoiceRepository,
    authManager: AuthManager,
) : ViewModel() {

    private val buyerId: String = authManager.currentUser?.buyerId.orEmpty()

    private val entries = if (buyerId.isBlank()) {
        MutableStateFlow(emptyList<BuyerStatementEntry>())
    } else {
        buyerRepository.observeStatement(buyerId)
    }

    private val balance = if (buyerId.isBlank()) {
        MutableStateFlow(null)
    } else {
        buyerRepository.observeBalance(buyerId)
    }

    private val invoices = if (buyerId.isBlank()) {
        MutableStateFlow(emptyList<Invoice>())
    } else {
        invoiceRepository.observeByBuyer(buyerId)
    }

    val ui: StateFlow<StatementUiState> = combine(
        entries, balance, invoices
    ) { rows, bal, invoiceRows ->
        StatementUiState(entries = rows, balance = bal, invoices = invoiceRows)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatementUiState())
}