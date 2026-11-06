// feature_buyer/order/OrderConfirmationViewModel.kt
package com.agency.app.feature_buyer.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.network.MessageType
import com.agency.app.core.network.SyncManager
import com.agency.app.core.network.SyncMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.json.JSONObject
import javax.inject.Inject

data class OrderConfirmationUiState(
    val isLoading: Boolean = true,
    val buyerName: String = "أحمد محمد",
    val buyerPhone: String = "07701234567",
    val buyerAddress: String = "بغداد - الكرادة",
    val agencyName: String = "وكالة النور",
    val agencyAddress: String = "بغداد - الشورجة",
    val items: List<OrderSummaryItem> = emptyList(),
    val subtotal: Double = 0.0,
    val tax: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val notes: String = "",
    val isConfirming: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OrderConfirmationViewModel @Inject constructor(
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderConfirmationUiState())
    val uiState: StateFlow<OrderConfirmationUiState> = _uiState.asStateFlow()

    fun loadOrderSummary() {
        viewModelScope.launch {
            val items = listOf(
                OrderSummaryItem("رز بسمتي", 5, "كغ", 1500.0, 7500.0),
                OrderSummaryItem("زيت عباد الشمس", 3, "لتر", 3000.0, 9000.0),
                OrderSummaryItem("سكر أبيض", 10, "كغ", 1200.0, 12000.0)
            )
            val subtotal = items.sumOf { it.totalPrice }
            val tax = subtotal * 0.05
            val total = subtotal + tax

            _uiState.update {
                it.copy(
                    isLoading = false,
                    items = items,
                    subtotal = subtotal,
                    tax = tax,
                    total = total,
                    notes = "يرجى التسليم صباحاً"
                )
            }
        }
    }

    fun confirmOrder() {
        viewModelScope.launch {
            _uiState.update { it.copy(isConfirming = true) }
            try {
                val payload = JSONObject().apply {
                    put("buyerName", _uiState.value.buyerName)
                    put("itemsCount", _uiState.value.items.size)
                    put("total", _uiState.value.total)
                    put("timestamp", Clock.System.now().toEpochMilliseconds())
                }

                syncManager.send(
                    SyncMessage(
                        type = MessageType.ORDER_CREATED,
                        payload = payload,
                        timestamp = Clock.System.now().toEpochMilliseconds(),
                        userId = "buyer_user"
                    )
                )

                _uiState.update { it.copy(isConfirming = false) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isConfirming = false, error = e.message) 
                }
            }
        }
    }
}
