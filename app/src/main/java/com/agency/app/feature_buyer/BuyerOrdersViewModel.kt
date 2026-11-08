package com.agency.app.feature_buyer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.domain.model.Order
import com.agency.app.core.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/** قائمة طلبيات المشتري الحالي. */
@HiltViewModel
class BuyerOrdersViewModel @Inject constructor(
    orderRepository: OrderRepository,
    authManager: AuthManager,
) : ViewModel() {

    val orders: StateFlow<List<Order>> = orderRepository
        .observeByBuyer(authManager.currentUser?.buyerId.orEmpty())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/**
 * تتبّع طلبية واحدة.
 *
 * الشاشة تقرأ من Room مباشرةً، وحين يكتب [com.agency.app.core.data.sync.SyncManager]
 * حدث تغيّر الحالة في الجدول يصل التحديث للشاشة تلقائياً بلا أي اشتراك شبكي
 * إضافي من الواجهة — هذا هو مسار WebSocket → Room → Flow → UI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OrderTrackingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    orderRepository: OrderRepository,
) : ViewModel() {

    private val orderId: String = savedStateHandle.get<String>("orderId").orEmpty()

    val order: StateFlow<Order?> = orderRepository.observeOrder(orderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** الحالة الوسيطة تُستخدم لتمييز "لم يُحمَّل بعد" عن "غير موجود". */
    val placeholder: StateFlow<Boolean> = flowOf(orderId.isBlank())
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
}