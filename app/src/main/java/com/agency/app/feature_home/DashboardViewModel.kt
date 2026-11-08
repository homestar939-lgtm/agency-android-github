package com.agency.app.feature_home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.data.auth.SessionState
import com.agency.app.core.domain.model.BuyerBalance
import com.agency.app.core.domain.model.Invoice
import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.domain.model.Order
import com.agency.app.core.domain.model.OrderStatus
import com.agency.app.core.domain.model.Product
import com.agency.app.core.domain.repository.BuyerRepository
import com.agency.app.core.domain.repository.InvoiceRepository
import com.agency.app.core.domain.repository.OrderRepository
import com.agency.app.core.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * مؤشرات لوحة الدور.
 *
 * كل رقم هنا مشتق من `Flow` قادم من Room؛ لا يوجد استدعاء شبكة واحد
 * ولا قيمة ثابتة مكتوبة يدوياً.
 */
data class DashboardMetrics(
    val ordersTotal: Int = 0,
    val ordersNew: Int = 0,
    val ordersPreparing: Int = 0,
    val ordersPrepared: Int = 0,
    val ordersSent: Int = 0,
    val ordersDelivered: Int = 0,
    val invoicesDraft: Int = 0,
    val invoicesPending: Int = 0,
    val invoicesApproved: Int = 0,
    val productsLowStock: Int = 0,
    val productsOutOfStock: Int = 0,
    val outstandingTotalCents: Long = 0L,
    val overLimitBuyers: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    authManager: AuthManager,
    orderRepository: OrderRepository,
    invoiceRepository: InvoiceRepository,
    productRepository: ProductRepository,
    buyerRepository: BuyerRepository
) : ViewModel() {

    private val authManager = authManager

    val session: StateFlow<SessionState> = authManager.session

    private val orders: StateFlow<List<Order>> = orderRepository.observeOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val invoices: StateFlow<List<Invoice>> = invoiceRepository.observeInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val products: StateFlow<List<Product>> = productRepository.observeAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val balances: StateFlow<List<BuyerBalance>> = buyerRepository.observeBalances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val metrics: StateFlow<DashboardMetrics> = combine(
        orders, invoices, products, balances
    ) { orderRows, invoiceRows, productRows, balanceRows ->
        DashboardMetrics(
            ordersTotal = orderRows.size,
            ordersNew = orderRows.count { it.status == OrderStatus.NEW },
            ordersPreparing = orderRows.count { it.status == OrderStatus.PREPARING },
            ordersPrepared = orderRows.count { it.status == OrderStatus.PREPARED },
            ordersSent = orderRows.count { it.status == OrderStatus.SENT },
            ordersDelivered = orderRows.count { it.status == OrderStatus.DELIVERED },
            invoicesDraft = invoiceRows.count { it.status == InvoiceStatus.DRAFT },
            invoicesPending = invoiceRows.count { it.status == InvoiceStatus.PENDING_APPROVAL },
            invoicesApproved = invoiceRows.count { it.status == InvoiceStatus.APPROVED },
            productsLowStock = productRows.count { it.isLowStock },
            productsOutOfStock = productRows.count { it.isOutOfStock },
            outstandingTotalCents = balanceRows.sumOf { it.outstanding.cents },
            overLimitBuyers = balanceRows.count { it.isOverLimit }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardMetrics())

    fun signOut() = authManager.signOut()
}