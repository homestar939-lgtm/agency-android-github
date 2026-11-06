// feature_warehouse/orders/OrdersViewModel.kt
package com.agency.app.feature_warehouse.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.database.entity.Order
import com.agency.app.core.database.entity.OrderStatus
import com.agency.app.core.network.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrdersUiState(
    val isLoading: Boolean = true,
    val orders: List<Order> = emptyList(),
    val selectedStatusIndex: Int = 0,
    val ordersCountByStatus: Map<OrderStatus, Int> = emptyMap(),
    val isSpeedDialExpanded: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    init {
        refreshOrders()
        observeOrders()
    }

    fun refreshOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                syncManager.syncOrders()
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun observeOrders() {
        viewModelScope.launch {
            // مراقبة الطلبات من قاعدة البيانات المحلية
            // ordersRepository.getOrders().collect { orders ->
            //     updateUiState(orders)
            // }
        }
    }

    fun selectStatus(index: Int) {
        _uiState.update { it.copy(selectedStatusIndex = index) }
        // تصفية الطلبات حسب الحالة المختارة
    }

    fun toggleSpeedDial() {
        _uiState.update { it.copy(isSpeedDialExpanded = !it.isSpeedDialExpanded) }
    }

    private fun updateUiState(orders: List<Order>) {
        val countByStatus = orders.groupingBy { it.status }.eachCount()
        val filteredOrders = filterBySelectedStatus(orders)
        
        _uiState.update {
            it.copy(
                orders = filteredOrders,
                ordersCountByStatus = countByStatus
            )
        }
    }

    private fun filterBySelectedStatus(orders: List<Order>): List<Order> {
        val selectedStatus = OrderStatus.values()[_uiState.value.selectedStatusIndex]
        return orders.filter { it.status == selectedStatus }
    }
}
