package com.agency.app.feature_warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.domain.model.AppError
import com.agency.app.core.domain.model.Invoice
import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.Order
import com.agency.app.core.domain.model.OrderStatus
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.Product
import com.agency.app.core.domain.repository.InvoiceRepository
import com.agency.app.core.domain.repository.OrderRepository
import com.agency.app.core.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * إحصائيات المخزن.
 *
 * كل رقم هنا محسوب من المستودعات لا مخزَّن في حالة: لو خُزِّن لانحرف عن
 * القاعدة بعد أول تعديل، وظهرت شاشة تقول «قيمة المخزون 5000» بينما هو 3200.
 *
 * والحد الأدنى للمخزون يُشتق من `Product.isLowStock` المشتركة مع بقية
 * الشاشات، فلا يوجد رقمان لمعنى واحد.
 */
data class WarehouseStatsUiState(
    val totalProducts: Int = 0,
    val activeProducts: Int = 0,
    val outOfStock: Int = 0,
    val lowStock: Int = 0,
    val totalStockUnits: Int = 0,
    val stockValue: Money = Money.ZERO,
    val potentialRevenue: Money = Money.ZERO,
    val projectedMargin: Money = Money.ZERO,
    val pendingInvoices: Int = 0,
    val ordersToPrepare: Int = 0,
    val isLoading: Boolean = true,
) {
    /** نسبة المنتجات التي تحتاج تدخلاً فورياً من إجمالي النشِط. */
    val stockHealthPercent: Int
        get() = if (activeProducts == 0) 100
        else (((activeProducts - outOfStock - lowStock) * 100) / activeProducts)
            .coerceIn(0, 100)

    val hasIssues: Boolean get() = outOfStock > 0 || lowStock > 0
}

@HiltViewModel
class WarehouseStatsViewModel @Inject constructor(
    productRepository: ProductRepository,
    invoiceRepository: InvoiceRepository,
    orderRepository: OrderRepository,
) : ViewModel() {

    private val products: StateFlow<List<Product>> = productRepository.observeAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val invoices: StateFlow<List<Invoice>> = invoiceRepository.observeInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val orders: StateFlow<List<Order>> = orderRepository.observeOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ui: StateFlow<WarehouseStatsUiState> =
        combine(products, invoices, orders) { productRows, invoiceRows, orderRows ->
            buildState(productRows, invoiceRows, orderRows)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            WarehouseStatsUiState()
        )

    private fun buildState(
        productRows: List<Product>,
        invoiceRows: List<Invoice>,
        orderRows: List<Order>,
    ): WarehouseStatsUiState {
        val active = productRows.filter { it.isActive }

        /*
         * قيمة المخزون بالتكلفة لا بالسعر: هي ما خسرناه فعلاً لو تلف المخزون.
         * الإيراد المحتمل منفصل عنها لأن الخلط بين الاثنين يجعل قرار الشراء
         * مبنياً على رقم متفائل.
         */
        val stockValue = active.fold(Money.ZERO) { acc, p -> acc + (p.cost * p.stockQuantity) }
        val potential = active.fold(Money.ZERO) { acc, p -> acc + (p.price * p.stockQuantity) }

        return WarehouseStatsUiState(
            totalProducts = productRows.size,
            activeProducts = active.size,
            outOfStock = active.count { it.isOutOfStock },
            lowStock = active.count { it.isLowStock },
            totalStockUnits = active.sumOf { it.stockQuantity },
            stockValue = stockValue,
            potentialRevenue = potential,
            projectedMargin = potential - stockValue,
            pendingInvoices = invoiceRows.count { it.status == InvoiceStatus.PENDING_APPROVAL },
            ordersToPrepare = orderRows.count {
                it.status == OrderStatus.NEW || it.status == OrderStatus.PREPARING
            },
            isLoading = false,
        )
    }
}

/**
 * حركة المخزون: كميات صادرة وواردة مستنبطة من الطلبيات الحقيقية.
 *
 * لا يوجد جدول حركة مخزون في القاعدة، واختراع جدول كان سيُنشئ مصدر حقيقة
 * ثانياً للأرصدة — وهو بالضبط ما نتجنّبه عمداً (لهذا لا يوجد زر «خصم مخزون»
 * في شاشة المسح). فالمعروض هنا مستنبط من الطلبيات: كمية كل منتج في الطلبيات
 * غير الملغاة تُعدّ حركة صادرة، والملغاة لا تُخصم.
 */
data class StockMovementRow(
    val productId: String,
    val productName: String,
    val unit: String,
    val currentStock: Int,
    val outgoingUnits: Int,
    val ordersCount: Int,
    val remainingAfterPending: Int,
    val isCritical: Boolean,
)

data class StockMovementUiState(
    val rows: List<StockMovementRow> = emptyList(),
    val sortNewestStockFirst: Boolean = true,
    val isLoading: Boolean = true,
) {
    val totalOutgoing: Int get() = rows.sumOf { it.outgoingUnits }
    val criticalCount: Int get() = rows.count { it.isCritical }
    val isEmpty: Boolean get() = !isLoading && rows.isEmpty()
}

@HiltViewModel
class StockMovementViewModel @Inject constructor(
    productRepository: ProductRepository,
    orderRepository: OrderRepository,
) : ViewModel() {

    private val sortFlag = MutableStateFlow(true)
    private val _ui = MutableStateFlow(StockMovementUiState())
    val ui: StateFlow<StockMovementUiState> = _ui.asStateFlow()

    private val products = productRepository.observeAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val orders = orderRepository.observeOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            combine(products, orders, sortFlag) { productRows, orderRows, newestFirst ->
                Triple(productRows, orderRows, newestFirst)
            }.collect { (productRows, orderRows, newestFirst) ->
                val live = orderRows.filter { it.status != OrderStatus.CANCELLED }
                val rows = productRows.filter { it.isActive }.map { product ->
                    val touching = live.filter { order ->
                        order.items.any { it.productId == product.id }
                    }
                    val outgoing = touching.sumOf { order ->
                        order.items.filter { it.productId == product.id }.sumOf { it.quantity }
                    }
                    StockMovementRow(
                        productId = product.id,
                        productName = product.name,
                        unit = product.unit,
                        currentStock = product.stockQuantity,
                        outgoingUnits = outgoing,
                        ordersCount = touching.size,
                        remainingAfterPending = product.stockQuantity - outgoing,
                        /*
                         * الخطر معرَّف على «ما تبقّى بعد تنفيذ الطلبيات القائمة»،
                         * لا على الرصيد الحالي. رصيد 8 يبدو آمناً حتى تعلم أن
                         * 15 وحدة مطلوبة فعلاً.
                         */
                        isCritical = (product.stockQuantity - outgoing) < product.minOrderQuantity,
                    )
                }
                val sorted = if (newestFirst) {
                    rows.sortedWith(compareByDescending<StockMovementRow> { it.outgoingUnits }
                        .thenBy { it.remainingAfterPending })
                } else {
                    rows.sortedBy { it.remainingAfterPending }
                }
                _ui.value = StockMovementUiState(
                    rows = sorted,
                    sortNewestStockFirst = newestFirst,
                    isLoading = false,
                )
            }
        }
    }

    fun toggleSort() {
        sortFlag.value = !sortFlag.value
    }
}

/** تنبيهات المخزون: قائمة عمل لا قائمة معلومات. */
data class StockAlertRow(
    val productId: String,
    val productName: String,
    val sku: String,
    val currentStock: Int,
    val minOrderQuantity: Int,
    val suggestedOrder: Int,
    val isOutOfStock: Boolean,
)

data class StockAlertsUiState(
    val alerts: List<StockAlertRow> = emptyList(),
    val isLoading: Boolean = true,
) {
    val outOfStockCount: Int get() = alerts.count { it.isOutOfStock }
    val lowCount: Int get() = alerts.size - outOfStockCount
    val isEmpty: Boolean get() = !isLoading && alerts.isEmpty()
}

@HiltViewModel
class StockAlertsViewModel @Inject constructor(
    productRepository: ProductRepository,
) : ViewModel() {

    val ui: StateFlow<StockAlertsUiState> = productRepository.observeAllProducts()
        .map { products ->
            val alerts = products
                .filter { it.isActive && (it.isOutOfStock || it.isLowStock) }
                .map { p ->
                    StockAlertRow(
                        productId = p.id,
                        productName = p.name,
                        sku = p.sku,
                        currentStock = p.stockQuantity,
                        minOrderQuantity = p.minOrderQuantity,
                        /*
                         * الكمية المقترحة تُعاد إلى ضعف الحد الأدنى لا إلى الحد
                         * الأدنى: الطلب الذي يصل بالرصيد إلى حده تماماً يعني
                         * إعادة الطلب بعد أول طلبية، وهو ما يستهلك المشرف.
                         */
                        suggestedOrder = (p.minOrderQuantity * 2) - p.stockQuantity,
                        isOutOfStock = p.isOutOfStock,
                    )
                }
                .sortedWith(
                    compareByDescending<StockAlertRow> { it.isOutOfStock }
                        .thenBy { it.currentStock }
                )
            StockAlertsUiState(alerts = alerts, isLoading = false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StockAlertsUiState())
}

/**
 * فواتير المخزن: قراءة فقط، والتفويض واضح.
 *
 * شاشة المخزن تعرض الفواتير ليعرف المشرف ما سيُجهَّز، لكنها **لا تعتمد**
 * ولا تعدّل. الاعتماد صلاحية مالية محفوظة للمدير، ووجود زر اعتماد هنا كان
 * سيُسقط القيد من الباب الخلفي.
 */
data class WarehouseInvoicesUiState(
    val invoices: List<Invoice> = emptyList(),
    val filter: WarehouseInvoiceFilter = WarehouseInvoiceFilter.APPROVED_ONLY,
    val query: String = "",
    val isLoading: Boolean = true,
) {
    val visible: List<Invoice>
        get() = invoices.filter { invoice ->
            filter.matches(invoice) &&
                (query.isBlank() ||
                    invoice.invoiceNumber.contains(query, ignoreCase = true) ||
                    invoice.buyerName.contains(query, ignoreCase = true))
        }

    val visibleTotal: Money get() = visible.fold(Money.ZERO) { acc, i -> acc + i.grandTotal }
    val isEmpty: Boolean get() = !isLoading && visible.isEmpty()
}

enum class WarehouseInvoiceFilter(val label: String) {
    APPROVED_ONLY("معتمدة"),
    PENDING("بانتظار الاعتماد"),
    ALL("الكل");

    fun matches(invoice: Invoice): Boolean = when (this) {
        APPROVED_ONLY -> invoice.status == InvoiceStatus.APPROVED
        PENDING -> invoice.status == InvoiceStatus.PENDING_APPROVAL
        ALL -> true
    }
}

@HiltViewModel
class WarehouseInvoicesViewModel @Inject constructor(
    invoiceRepository: InvoiceRepository,
) : ViewModel() {

    private val filter = MutableStateFlow(WarehouseInvoiceFilter.APPROVED_ONLY)
    private val query = MutableStateFlow("")
    private val _ui = MutableStateFlow(WarehouseInvoicesUiState())
    val ui: StateFlow<WarehouseInvoicesUiState> = _ui.asStateFlow()

    private val invoices = invoiceRepository.observeInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            /*
             * الدمج الثنائي هنا أُبقي على مرحلتين باسم صريح بدل تمرير ثلاثة
             * تدفقات إلى `combine` واحد: النوع الضمني في الحالة الأخيرة كان
             * سيصير `Triple` مبهم المعنى، والخطأ في الترتيب لا يظهر إلا وقت
             * التشغيل.
             */
            combine(invoices, filter) { rows, selected -> rows to selected }
                .combine(query) { (rows, selected), text ->
                    WarehouseInvoicesUiState(
                        invoices = rows,
                        filter = selected,
                        query = text,
                        isLoading = false,
                    )
                }
                .collect { _ui.value = it }
        }
    }

    fun setFilter(value: WarehouseInvoiceFilter) {
        filter.value = value
    }

    fun onQueryChange(value: String) {
        query.value = value
    }
}

/** عارض الفاتورة: يجلب فاتورة واحدة بالمعرّف، ويعرّف حالته بوضوح. */
sealed interface InvoiceViewerState {
    data object Loading : InvoiceViewerState
    data class Found(val invoice: Invoice) : InvoiceViewerState
    data class Failed(val message: String) : InvoiceViewerState
}

@HiltViewModel
class InvoiceViewerViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<InvoiceViewerState>(InvoiceViewerState.Loading)
    val state: StateFlow<InvoiceViewerState> = _state.asStateFlow()

    fun load(invoiceId: String) {
        if (_state.value is InvoiceViewerState.Found) return
        viewModelScope.launch {
            _state.value = when (val result = invoiceRepository.getInvoice(invoiceId)) {
                is Outcome.Success -> InvoiceViewerState.Found(result.value)
                is Outcome.Failure -> InvoiceViewerState.Failed(result.error.readable())
            }
        }
    }
}

/**
 * نص خطأ عربي واحد لكل حالات الفشل في وحدة المخزن.
 *
 * مكان واحد بدل تكرار `when` في كل شاشة: تكراره كان يجعل رسالة واحدة تختلف
 * بين شاشتين لنفس الخطأ، فيظن المستخدم أنهما عطلان مختلفان.
 */
internal fun AppError.readable(): String = when (this) {
    is AppError.Validation -> reason
    is AppError.Permission -> required
    is AppError.NotFound -> "العنصر ($entity) غير موجود: $id"
    is AppError.Conflict -> reason
    is AppError.Network -> "تعذّر الاتصال: $reason"
    is AppError.Unknown -> "خطأ غير متوقع"
}