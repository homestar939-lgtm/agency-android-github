package com.agency.app.feature_warehouse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.domain.model.AppError
import com.agency.app.core.domain.model.Order
import com.agency.app.core.domain.model.OrderStatus
import com.agency.app.core.domain.model.Outcome
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
 * طابور طلبيات المخزن.
 *
 * الفلتر الافتراضي «العمل الجاري» أي كل ما لم يصل حالة نهائية، لأن موظف
 * المخزن يهتم بما يجب تحريكه الآن لا بأرشيف ما سُلّم.
 */
enum class WarehouseFilter(val label: String) {
    ACTIVE("العمل الجاري"),
    NEW("جديدة"),
    PREPARING("قيد التحضير"),
    PREPARED("جاهزة"),
    SENT("مرسلة"),
    DELIVERED("مسلَّمة"),
    CANCELLED("ملغاة"),
    ALL("الكل");

    val status: OrderStatus? get() = when (this) {
        ACTIVE, ALL -> null
        NEW -> OrderStatus.NEW
        PREPARING -> OrderStatus.PREPARING
        PREPARED -> OrderStatus.PREPARED
        SENT -> OrderStatus.SENT
        DELIVERED -> OrderStatus.DELIVERED
        CANCELLED -> OrderStatus.CANCELLED
    }
}

data class WarehouseOrdersUiState(
    val orders: List<Order> = emptyList(),
    val filter: WarehouseFilter = WarehouseFilter.ACTIVE,
    val counts: Map<OrderStatus, Int> = emptyMap(),
)

@HiltViewModel
class WarehouseOrdersViewModel @Inject constructor(
    orderRepository: OrderRepository,
) : ViewModel() {

    private val allOrders = orderRepository.observeOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _filter = MutableStateFlow(WarehouseFilter.ACTIVE)

    val ui: StateFlow<WarehouseOrdersUiState> = combine(allOrders, _filter) { rows, filter ->
        val target = filter.status
        val visible = when (filter) {
            WarehouseFilter.ACTIVE -> rows.filterNot { it.status.isFinal }
            WarehouseFilter.ALL -> rows
            else -> rows.filter { it.status == target }
        }
        WarehouseOrdersUiState(
            orders = visible,
            filter = filter,
            counts = OrderStatus.entries.associateWith { status -> rows.count { it.status == status } },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WarehouseOrdersUiState())

    fun setFilter(value: WarehouseFilter) {
        _filter.value = value
    }
}

/**
 * تفاصيل طلبية في المخزن وانتقال حالتها.
 *
 * الانتقال يُحال إلى [OrderRepository.transition] التي تتحقق من
 * [OrderStatus.allowedNext] وتعيد المخزون عند الإلغاء — الشاشة لا تكرّر
 * هذا المنطق ولا تتجاوزه.
 */
data class WarehouseOrderDetailUiState(
    val order: Order? = null,
    val isWorking: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class WarehouseOrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>("orderId").orEmpty()

    private val _state = MutableStateFlow(WarehouseOrderDetailUiState())
    val ui: StateFlow<WarehouseOrderDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            orderRepository.observeOrder(orderId).collect { order ->
                _state.value = _state.value.copy(order = order)
            }
        }
    }

    /** ينفّذ الانتقال المطلوب ويسجّل الفاعل من الجلسة الحالية. */
    fun transition(target: OrderStatus, note: String? = null) {
        val user = authManager.currentUser ?: return
        if (_state.value.isWorking) return
        _state.value = _state.value.copy(isWorking = true, error = null)
        viewModelScope.launch {
            when (
                val result = orderRepository.transition(
                    orderId = orderId,
                    target = target,
                    actorId = user.id,
                    actorName = user.displayName,
                    note = note,
                )
            ) {
                is Outcome.Success -> _state.value = _state.value.copy(isWorking = false)
                is Outcome.Failure -> _state.value = _state.value.copy(
                    isWorking = false,
                    error = describe(result.error),
                )
            }
        }
    }

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }

    private fun describe(error: AppError): String = when (error) {
        is AppError.Conflict -> error.reason
        is AppError.Permission -> error.required
        is AppError.Validation -> "${error.field}: ${error.reason}"
        is AppError.NotFound -> "الطلبية غير موجودة"
        is AppError.Network -> "تعذّر الاتصال: ${error.reason}"
        is AppError.Unknown -> "خطأ غير متوقع، أعد المحاولة"
    }
}