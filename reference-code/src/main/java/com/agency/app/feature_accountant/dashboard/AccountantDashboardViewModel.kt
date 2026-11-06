// feature_accountant/dashboard/AccountantDashboardViewModel.kt
package com.agency.app.feature_accountant.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.network.MessageType
import com.agency.app.core.network.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountantDashboardUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val totalInvoices: Double = 0.0,
    val invoicesCount: Int = 0,
    val totalReceipts: Double = 0.0,
    val receiptsCount: Int = 0,
    val totalDebts: Double = 0.0,
    val debtorsCount: Int = 0,
    val todayCollected: Double = 0.0,
    val todayReceiptsCount: Int = 0,
    val alerts: List<String> = emptyList(),
    val recentTransactions: List<RecentTransaction> = emptyList(),
    val unreadNotifications: Int = 0,
    val error: String? = null
)

@HiltViewModel
class AccountantDashboardViewModel @Inject constructor(
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountantDashboardUiState())
    val uiState: StateFlow<AccountantDashboardUiState> = _uiState.asStateFlow()

    init {
        observeRealTimeUpdates()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val transactions = listOf(
                    RecentTransaction(
                        id = "inv_001",
                        type = TransactionType.INVOICE,
                        buyerName = "أحمد محمد العلي",
                        amount = 28500.0,
                        timestamp = "منذ 5 دقائق",
                        actorName = "محمد (المخزن)",
                        actorRole = "مخزن"
                    ),
                    RecentTransaction(
                        id = "rcp_001",
                        type = TransactionType.RECEIPT,
                        buyerName = "محل النور",
                        amount = 15000.0,
                        timestamp = "منذ 15 دقيقة",
                        actorName = "علي (المتحصل)",
                        actorRole = "متحصل"
                    ),
                    RecentTransaction(
                        id = "inv_002",
                        type = TransactionType.INVOICE,
                        buyerName = "سوق الخير",
                        amount = 45000.0,
                        timestamp = "منذ ساعة",
                        actorName = "محمد (المخزن)",
                        actorRole = "مخزن"
                    ),
                    RecentTransaction(
                        id = "rcp_002",
                        type = TransactionType.RECEIPT,
                        buyerName = "أحمد محمد العلي",
                        amount = 10000.0,
                        timestamp = "منذ ساعتين",
                        actorName = "علي (المتحصل)",
                        actorRole = "متحصل"
                    )
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        totalInvoices = 73500.0,
                        invoicesCount = 2,
                        totalReceipts = 25000.0,
                        receiptsCount = 2,
                        totalDebts = 450000.0,
                        debtorsCount = 15,
                        todayCollected = 25000.0,
                        todayReceiptsCount = 2,
                        alerts = listOf(
                            "3 فواتير بانتظار الاعتماد",
                            "مشتري واحد تجاوز حد الائتمان"
                        ),
                        recentTransactions = transactions,
                        unreadNotifications = 5
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun observeRealTimeUpdates() {
        viewModelScope.launch {
            syncManager.messages
                .filter { it.type in listOf(MessageType.INVOICE_CREATED, MessageType.RECEIPT_CREATED) }
                .collect { message ->
                    // تحديث فوري عند ورود فاتورة أو سند جديد
                    handleNewTransaction(message)
                }
        }
    }

    private fun handleNewTransaction(message: com.agency.app.core.network.SyncMessage) {
        // إضافة المعاملة الجديدة إلى القائمة
        viewModelScope.launch {
            _uiState.update { currentState ->
                val newTransaction = RecentTransaction(
                    id = message.payload.optString("id"),
                    type = if (message.type == MessageType.INVOICE_CREATED) 
                        TransactionType.INVOICE 
                    else TransactionType.RECEIPT,
                    buyerName = message.payload.optString("buyerName"),
                    amount = message.payload.optDouble("amount"),
                    timestamp = "الآن",
                    actorName = message.payload.optString("actorName"),
                    actorRole = message.payload.optString("actorRole")
                )

                currentState.copy(
                    recentTransactions = listOf(newTransaction) + currentState.recentTransactions,
                    unreadNotifications = currentState.unreadNotifications + 1
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            syncManager.syncFinancialData()
            loadDashboard()
            _uiState.update { it.copy(isSyncing = false) }
        }
    }
}
