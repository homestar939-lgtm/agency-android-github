// feature_cashier/dashboard/CashierDashboardViewModel.kt
package com.agency.app.feature_cashier.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.network.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CashierDashboardUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val periodLabel: String = "اليوم — 07 نوفمبر 2026",
    val totalCollected: Double = 0.0,
    val activeBuyersCount: Int = 0,
    val myCommission: Double = 0.0,
    val commissionPercent: Double = 2.5,
    val buyers: List<BuyerDebt> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class CashierDashboardViewModel @Inject constructor(
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CashierDashboardUiState())
    val uiState: StateFlow<CashierDashboardUiState> = _uiState.asStateFlow()

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val buyers = listOf(
                    BuyerDebt(
                        buyerId = "b1",
                        buyerName = "أحمد محمد العلي",
                        phone = "07701234567",
                        totalDebt = 125000.0,
                        currentDue = 28500.0,
                        lastReceiptDate = "05 نوفمبر 2026",
                        hasNewOrder = true
                    ),
                    BuyerDebt(
                        buyerId = "b2",
                        buyerName = "محل النور",
                        phone = "07712345678",
                        totalDebt = 85000.0,
                        currentDue = 15000.0,
                        lastReceiptDate = "03 نوفمبر 2026",
                        hasNewOrder = false
                    ),
                    BuyerDebt(
                        buyerId = "b3",
                        buyerName = "سوق الخير",
                        phone = "07723456789",
                        totalDebt = 210000.0,
                        currentDue = 45000.0,
                        lastReceiptDate = "01 نوفمبر 2026",
                        hasNewOrder = true
                    )
                )

                val totalCollected = 150000.0
                val commissionPercent = 2.5
                val myCommission = totalCollected * (commissionPercent / 100)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        buyers = buyers,
                        totalCollected = totalCollected,
                        activeBuyersCount = buyers.size,
                        myCommission = myCommission,
                        commissionPercent = commissionPercent
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            syncManager.syncCollections()
            loadDashboard()
            _uiState.update { it.copy(isSyncing = false) }
        }
    }
}
