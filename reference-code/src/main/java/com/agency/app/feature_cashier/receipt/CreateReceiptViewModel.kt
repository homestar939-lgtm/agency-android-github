// feature_cashier/receipt/CreateReceiptViewModel.kt
package com.agency.app.feature_cashier.receipt

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

data class Currency(val code: String, val name: String, val symbol: String)

data class CreateReceiptUiState(
    val buyerId: String = "",
    val buyerName: String = "",
    val buyerPhone: String = "",
    val totalDebt: Double = 0.0,
    val currentDue: Double = 0.0,
    val amountInput: String = "",
    val amountValue: Double = 0.0,
    val amountInWords: String = "",
    val amountError: String? = null,
    val selectedCurrency: Currency = Currency("IQD", "دينار عراقي", "د.ع"),
    val selectedCurrencyName: String = "دينار عراقي",
    val isCurrencyDropdownExpanded: Boolean = false,
    val currencies: List<Currency> = listOf(
        Currency("IQD", "دينار عراقي", "د.ع"),
        Currency("USD", "دولار أمريكي", "$"),
        Currency("EUR", "يورو", "€")
    ),
    val paymentMethod: String = "CASH",
    val notes: String = "",
    val remainingDebt: Double = 0.0,
    val myCommissionFromThis: Double = 0.0,
    val commissionPercent: Double = 2.5,
    val canCreate: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CreateReceiptViewModel @Inject constructor(
    private val syncManager: SyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateReceiptUiState())
    val uiState: StateFlow<CreateReceiptUiState> = _uiState.asStateFlow()

    fun loadBuyer(buyerId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    buyerId = buyerId,
                    buyerName = "أحمد محمد العلي",
                    buyerPhone = "07701234567",
                    totalDebt = 125000.0,
                    currentDue = 28500.0,
                    remainingDebt = 125000.0
                )
            }
        }
    }

    fun updateAmount(input: String) {
        val cleaned = input.replace(",", "").replace(" ", "")
        val value = cleaned.toDoubleOrNull() ?: 0.0
        
        val error = when {
            value <= 0 -> "المبلغ يجب أن يكون أكبر من صفر"
            value > _uiState.value.totalDebt -> "المبلغ أكبر من الذمة"
            else -> null
        }

        val remaining = _uiState.value.totalDebt - value
        val commission = value * (_uiState.value.commissionPercent / 100)

        _uiState.update {
            it.copy(
                amountInput = input,
                amountValue = value,
                amountInWords = convertToWords(value),
                amountError = error,
                remainingDebt = remaining,
                myCommissionFromThis = commission,
                canCreate = value > 0 && error == null && it.paymentMethod.isNotBlank()
            )
        }
    }

    fun selectCurrency(currency: Currency) {
        _uiState.update {
            it.copy(
                selectedCurrency = currency,
                selectedCurrencyName = currency.name,
                isCurrencyDropdownExpanded = false
            )
        }
    }

    fun toggleCurrencyDropdown() {
        _uiState.update { 
            it.copy(isCurrencyDropdownExpanded = !it.isCurrencyDropdownExpanded) 
        }
    }

    fun selectPaymentMethod(method: String) {
        _uiState.update {
            it.copy(
                paymentMethod = method,
                canCreate = it.amountValue > 0 && it.amountError == null
            )
        }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    fun createReceipt(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }
            try {
                val receiptId = "RCP-${System.currentTimeMillis()}"
                
                val payload = JSONObject().apply {
                    put("receiptId", receiptId)
                    put("buyerId", _uiState.value.buyerId)
                    put("amount", _uiState.value.amountValue)
                    put("currency", _uiState.value.selectedCurrency.code)
                    put("paymentMethod", _uiState.value.paymentMethod)
                    put("notes", _uiState.value.notes)
                    put("cashierId", "cashier_user")
                    put("timestamp", Clock.System.now().toEpochMilliseconds())
                }

                // إرسال سند القبض لحظياً لكل الأدوار
                syncManager.send(
                    SyncMessage(
                        type = MessageType.RECEIPT_CREATED,
                        payload = payload,
                        timestamp = Clock.System.now().toEpochMilliseconds(),
                        userId = "cashier_user"
                    )
                )

                _uiState.update { it.copy(isCreating = false) }
                onSuccess(receiptId)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(isCreating = false, error = e.message) 
                }
            }
        }
    }

    private fun convertToWords(amount: Double): String {
        // تحويل المبلغ إلى كلمات (مبسّط)
        return when {
            amount == 0.0 -> ""
            amount < 1000 -> "${amount.toInt()} دينار"
            amount < 1_000_000 -> "${"%,.0f".format(amount)} دينار"
            else -> "${"%,.0f".format(amount)} دينار"
        }
    }
}
