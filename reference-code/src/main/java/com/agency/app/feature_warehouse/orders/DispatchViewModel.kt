// feature_warehouse/orders/DispatchViewModel.kt
package com.agency.app.feature_warehouse.orders

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.network.SyncManager
import com.agency.app.core.network.MessageType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.json.JSONObject
import javax.inject.Inject

data class DispatchUiState(
    val orderId: String = "",
    val buyerName: String = "",
    val itemsCount: Int = 0,
    val totalAmount: Double = 0.0,
    val currency: String = "د.ع",
    val contactNumber: String = "",
    val deliveryMethod: String = "DIRECT",
    val canDispatch: Boolean = false,
    val isDispatching: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DispatchViewModel @Inject constructor(
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DispatchUiState())
    val uiState: StateFlow<DispatchUiState> = _uiState.asStateFlow()

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            // تحميل بيانات الطلبية من قاعدة البيانات
            _uiState.update { 
                it.copy(
                    orderId = orderId,
                    buyerName = "أحمد محمد",
                    itemsCount = 5,
                    totalAmount = 1250.0
                ) 
            }
        }
    }

    fun updateContactNumber(number: String) {
        _uiState.update { 
            it.copy(
                contactNumber = number,
                canDispatch = number.length >= 10 && it.deliveryMethod.isNotEmpty()
            ) 
        }
    }

    fun selectDeliveryMethod(method: String) {
        _uiState.update { 
            it.copy(
                deliveryMethod = method,
                canDispatch = it.contactNumber.length >= 10
            ) 
        }
    }

    fun loadContactFromUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            try {
                val cursor = contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    null, null, null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val number = it.getString(0)
                        updateContactNumber(number)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun confirmDispatch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDispatching = true) }
            try {
                val payload = JSONObject().apply {
                    put("orderId", _uiState.value.orderId)
                    put("contactNumber", _uiState.value.contactNumber)
                    put("deliveryMethod", _uiState.value.deliveryMethod)
                    put("sentAt", Clock.System.now().toEpochMilliseconds())
                }
                
                syncManager.send(
                    com.agency.app.core.network.SyncMessage(
                        type = MessageType.ORDER_SENT,
                        payload = payload,
                        timestamp = Clock.System.now().toEpochMilliseconds(),
                        userId = "warehouse_user"
                    )
                )
                
                _uiState.update { it.copy(isDispatching = false) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isDispatching = false, error = e.message) 
                }
            }
        }
    }
}
