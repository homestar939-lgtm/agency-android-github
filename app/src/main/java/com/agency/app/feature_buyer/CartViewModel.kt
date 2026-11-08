package com.agency.app.feature_buyer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.domain.model.AppError
import com.agency.app.core.domain.model.BuyerBalance
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.repository.BuyerRepository
import com.agency.app.core.domain.repository.NewOrderLine
import com.agency.app.core.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * سلة المشتري وتأكيد الطلبية.
 *
 * التأكيد يمرّ عبر [OrderRepository.createOrder] فقط — لا كتابة مباشرة على
 * Room من الواجهة. نجاح العملية يُفرِّغ السلة ويُبلغ عن رقم الطلبية،
 * وفشلها يُظهر سبب الخطأ كما أعادته الطبقة الوسطى دون إعادة صياغة.
 */
data class CartUiState(
    val lines: List<CartLine> = emptyList(),
    val total: Money = Money.ZERO,
    val itemCount: Int = 0,
    val canCheckout: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val lastCreatedOrderId: String? = null,
    val lastCreatedOrderNumber: String? = null,
    val balance: BuyerBalance? = null,
)

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartStore: CartStore,
    private val orderRepository: OrderRepository,
    buyerRepository: BuyerRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val _state = MutableStateFlow(CartUiState())
    val ui: StateFlow<CartUiState> = _state.asStateFlow()

    private val buyerId: String? = (authManager.currentUser)?.buyerId

    private val balanceFlow = buyerId
        ?.let { buyerRepository.observeBalance(it) }
        ?: MutableStateFlow(null)

    init {
        viewModelScope.launch {
            combine(cartStore.lines, balanceFlow) { lines, balance ->
                lines to balance
            }.collect { (lines, balance) ->
                _state.value = _state.value.copy(
                    lines = lines,
                    total = CartStore.total(lines),
                    itemCount = CartStore.itemCount(lines),
                    canCheckout = CartStore.canCheckout(lines),
                    balance = balance,
                )
            }
        }
    }

    fun setQuantity(productId: String, quantity: Int) = cartStore.setQuantity(productId, quantity)

    fun remove(productId: String) = cartStore.remove(productId)

    fun clear() = cartStore.clear()

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }

    fun dismissCreated() {
        _state.value = _state.value.copy(lastCreatedOrderId = null, lastCreatedOrderNumber = null)
    }

    /** ينشئ الطلبية فعلياً. لا يفعل شيئاً إن كانت السلة غير صالحة. */
    fun submit() {
        if (_state.value.isSubmitting) return
        val lines = _state.value.lines
        if (!CartStore.canCheckout(lines)) {
            _state.value = _state.value.copy(error = "راجع الكميات: هناك سطر أقل من الحد الأدنى أو يتجاوز المخزون")
            return
        }
        val user = authManager.currentUser
        val targetBuyerId = user?.buyerId ?: buyerId
        if (user == null || targetBuyerId.isNullOrBlank()) {
            _state.value = _state.value.copy(
                error = "حسابك غير مرتبط بمشتري؛ راجع المدير لربط الحساب"
            )
            return
        }

        _state.value = _state.value.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            val payload = lines.map { NewOrderLine(productId = it.productId, quantity = it.quantity) }
            when (
                val result = orderRepository.createOrder(
                    buyerId = targetBuyerId,
                    lines = payload,
                    actorId = user.id,
                    actorName = user.displayName,
                )
            ) {
                is Outcome.Success -> {
                    cartStore.clear()
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        lastCreatedOrderId = result.value.id,
                        lastCreatedOrderNumber = result.value.orderNumber,
                    )
                }
                is Outcome.Failure -> {
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        error = messageOf(result.error),
                    )
                }
            }
        }
    }

    private fun messageOf(error: AppError): String = when (error) {
        is AppError.Validation -> "${error.field}: ${error.reason}"
        is AppError.NotFound -> "العنصر غير موجود: ${error.entity}"
        is AppError.Permission -> error.required
        is AppError.Conflict -> error.reason
        is AppError.Network -> "تعذّر الاتصال: ${error.reason}"
        is AppError.Unknown -> "خطأ غير متوقع، أعد المحاولة"
    }
}