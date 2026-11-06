// feature_buyer/order/OrderTrackingViewModel.kt
package com.agency.app.feature_buyer.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.database.entity.OrderStatus
import com.agency.app.core.network.MessageType
import com.agency.app.core.network.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderTrackingUiState(
    val orderId: String = "",
    val status: OrderStatus = OrderStatus.NEW,
    val orderDate: String = "",
    val createdAt: String = "",
    val preparingAt: String? = null,
    val preparedAt: String? = null,
    val sentAt: String? = null,
    val deliveredAt: String? = null,
    val itemsCount: Int = 0,
    val totalAmount: Double = 0.0,
    val deliveryMethod: String = "",
    val deliveryContact: String? = null
)

@HiltViewModel
class OrderTrackingViewModel @Inject constructor(
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderTrackingUiState())
    val uiState: StateFlow<OrderTrackingUiState> = _uiState.asStateFlow()

    fun observeOrder(orderId: String) {
        viewModelScope.launch {
            // تحميل البيانات الأولية
            _uiState.update {
                it.copy(
                    orderId = orderId,
                    status = OrderStatus.PREPARED,
                    orderDate = "07 نوفمبر 2026 - 10:30 ص",
                    createdAt = "10:30 ص",
                    preparedAt = "11:15 ص",
                    itemsCount = 3,
                    totalAmount = 28500.0,
                    deliveryMethod = "توصيل مباشر"
                )
            }

            // الاستماع لتحديثات WebSocket
            syncManager.messages
                .filter { msg ->
                    msg.payload.optString("orderId") == orderId &&
                    msg.type in listOf(
                        MessageType.ORDER_PREPARED,
                        MessageType.ORDER_SENT,
                        MessageType.ORDER_DELIVERED
                    )
                }
                .collect { message ->
                    updateStatusFromMessage(message)
                }
        }
    }

    private fun updateStatusFromMessage(message: com.agency.app.core.network.SyncMessage) {
        val timestamp = formatTimestamp(message.timestamp)
        
        _uiState.update { current ->
            when (message.type) {
                MessageType.ORDER_PREPARED -> current.copy(
                    status = OrderStatus.PREPARED,
                    preparedAt = timestamp
                )
                MessageType.ORDER_SENT -> current.copy(
                    status = OrderStatus.SENT,
                    sentAt = timestamp,
                    deliveryContact = message.payload.optString("contactNumber")
                )
                MessageType.ORDER_DELIVERED -> current.copy(
                    status = OrderStatus.DELIVERED,
                    deliveredAt = timestamp
                )
                else -> current
            }
        }
    }

    private fun formatTimestamp(epoch: Long): String {
        // تحويل الطابع الزمني إلى نص قابل للقراءة
        return "11:45 ص"
    }
}
