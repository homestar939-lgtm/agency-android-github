package com.agency.app.feature_admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.domain.model.BuyerBalance
import com.agency.app.core.domain.model.Invoice
import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.Order
import com.agency.app.core.domain.model.OrderStatus
import com.agency.app.core.domain.model.Product
import com.agency.app.core.domain.model.Receipt
import com.agency.app.core.domain.repository.BuyerRepository
import com.agency.app.core.domain.repository.InvoiceRepository
import com.agency.app.core.domain.repository.OrderRepository
import com.agency.app.core.domain.repository.ProductRepository
import com.agency.app.core.domain.repository.ReceiptRepository
import com.agency.app.core.util.TimeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** فترة التقرير — كل الفترات تُقاس على `createdAt` بتوقيت الجهاز. */
enum class ReportRange(val label: String, val days: Int?) {
    ALL("الكل", null),
    LAST_7("آخر 7 أيام", 7),
    LAST_30("آخر 30 يوماً", 30),
    LAST_90("آخر 90 يوماً", 90);

    /** بداية الفترة بالمللي ثانية، أو null لـ[ALL]. */
    fun startMillis(nowMillis: Long): Long? =
        days?.let { nowMillis - it * 86_400_000L }
}

/**
 * أرقام التقرير.
 *
 * كل مبلغ هنا مجموع من مستندات حقيقية (فواتير معتمدة وسندات قبض)، لا رقم
 * مشتق من ذاكرة الواجهة. مجموع `receiptsTotal` و`outstandingTotal` مع
 * `invoicedTotal` يحقق المعادلة `invoiced = collected + outstanding`، وهي
 * التي تكشف أي خلل محاسبي فوراً لو اختلّت في العرض.
 */
data class AdminReportMetrics(
    val invoicedTotal: Money = Money.ZERO,
    val collectedTotal: Money = Money.ZERO,
    val commissionTotal: Money = Money.ZERO,
    val outstandingTotal: Money = Money.ZERO,
    val ordersTotal: Int = 0,
    val ordersDelivered: Int = 0,
    val ordersCancelled: Int = 0,
    val invoicesApproved: Int = 0,
    val invoicesRejected: Int = 0,
    val buyersTotal: Int = 0,
    val buyersOverLimit: Int = 0,
    val stockValueTotal: Money = Money.ZERO,
    val lowStockCount: Int = 0,
) {
    /** معدّل التحصيل الكلي — نسبة ما حُصّل من المفوتر المعتمد. */
    val collectionRate: Int
        get() = if (invoicedTotal.isZero) 100
        else ((collectedTotal.cents * 100L) / invoicedTotal.cents).toInt().coerceIn(0, 100)

    /**
     * هل المعادلة المحاسبية محققة؟
     *
     * تُقاس على كل السجل فقط. عند اختيار فترة جزئية يكون المفوتر والمحصّل
     * مقيَّدين بالفترة بينما الذمة راهنة، فلا تتوازن المعادلة بحكم التعريف
     * لا بحكم خلل — والشاشة تقول ذلك صراحةً بدل عرض تحذير كاذب.
     */
    val isBalanced: Boolean
        get() = invoicedTotal.cents == collectedTotal.cents + outstandingTotal.cents
}

data class AdminReportsUiState(
    val range: ReportRange = ReportRange.LAST_30,
    val metrics: AdminReportMetrics = AdminReportMetrics(),
    val topBuyers: List<BuyerBalance> = emptyList(),
    val topProducts: List<ProductLine> = emptyList(),
)

/** سطر منتج في تقرير الأكثر مبيعاً. */
data class ProductLine(
    val productId: String,
    val name: String,
    val quantitySold: Int,
    val revenue: Money,
)

/**
 * تقارير الإدارة.
 *
 * التجميع يقع هنا في طبقة العرض لا في `Room` لأن حجم بيانات وكالة واحدة
 * يحتمله، ولأن كتابة استعلام تجميعي معقّد في DAO تضيف سطحاً جديداً للأخطاء
 * المحاسبية. حين يصير الحجم مشكلة يُنقل الاستعلام، لكن المعادلة تبقى واحدة.
 */
@HiltViewModel
class AdminReportsViewModel @Inject constructor(
    invoiceRepository: InvoiceRepository,
    receiptRepository: ReceiptRepository,
    orderRepository: OrderRepository,
    buyerRepository: BuyerRepository,
    productRepository: ProductRepository,
) : ViewModel() {

    private val invoices = invoiceRepository.observeInvoices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val receipts = receiptRepository.observeReceipts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val orders = orderRepository.observeOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val balances = buyerRepository.observeBalances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val products = productRepository.observeAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _range = MutableStateFlow(ReportRange.LAST_30)

    /**
     * تُدمج القوائم الخمس أولاً في حزمة واحدة، ثم تُدمج الحزمة مع الفترة.
     *
     * الدمج على مرحلتين لأنه لا يوجد `combine` بأنواع آمنة لستّة تدفقات —
     * البديل المتاح مع ستّة هو صيغة `vararg` التي تُعيد `Array<Any?>`
     * وتفرض تحويلات غير آمنة. تحويل واحد مكتوب بخطأ لن يظهر إلا وقت
     * التشغيل، فالتجميع على مرحلتين أضمن من `@Suppress`.
     */
    private val snapshot = combine(
        invoices, receipts, orders, balances, products
    ) { invoiceRows, receiptRows, orderRows, balanceRows, productRows ->
        ReportSnapshot(invoiceRows, receiptRows, orderRows, balanceRows, productRows)
    }

    val ui: StateFlow<AdminReportsUiState> = combine(snapshot, _range) { data, range ->
        buildState(
            range = range,
            invoiceRows = data.invoices,
            receiptRows = data.receipts,
            orderRows = data.orders,
            balanceRows = data.balances,
            productRows = data.products
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminReportsUiState())

    fun setRange(value: ReportRange) {
        _range.value = value
    }

    /** حزمة القوائم الخمس — كائن مُسمّى بدل مصفوفة مبهمة. */
    private data class ReportSnapshot(
        val invoices: List<Invoice>,
        val receipts: List<Receipt>,
        val orders: List<Order>,
        val balances: List<BuyerBalance>,
        val products: List<Product>,
    )

    /**
     * يُبنى التقرير كاملاً من القوائم الخمس.
     *
     * لاحظ أن `outstandingTotal` يُجمع من الأرصدة لا بالطرح: لو حُسب بالطرح
     * لأخفى أي سند قبض يتيماً بلا فاتورة. جمعه من مصدره الثاني يجعل
     * [AdminReportMetrics.isBalanced] اختباراً حقيقياً لا توتولوجي.
     */
    private fun buildState(
        range: ReportRange,
        invoiceRows: List<Invoice>,
        receiptRows: List<Receipt>,
        orderRows: List<Order>,
        balanceRows: List<BuyerBalance>,
        productRows: List<Product>
    ): AdminReportsUiState {
        val start = range.startMillis(TimeSource.now().toEpochMilliseconds())
        val inRangeInvoices = if (start == null) invoiceRows
        else invoiceRows.filter { it.createdAt.toEpochMilliseconds() >= start }
        val inRangeReceipts = if (start == null) receiptRows
        else receiptRows.filter { it.collectedAt.toEpochMilliseconds() >= start }
        val inRangeOrders = if (start == null) orderRows
        else orderRows.filter { it.createdAt.toEpochMilliseconds() >= start }

        val approved = inRangeInvoices.filter { it.status == InvoiceStatus.APPROVED }

        val invoicedTotal = approved.fold(Money.ZERO) { acc, inv -> acc + inv.grandTotal }

        val commissionTotal = approved.fold(Money.ZERO) { acc, inv -> acc + inv.commission }

        val collectedTotal = inRangeReceipts.fold(Money.ZERO) { acc, r -> acc + r.amount }

        /*
         * الأرصدة تُجمع كاملةً لا مقيَّدة بالفترة: الذمة القائمة اليوم حالة
         * راهنة لا حدث زمني، وتقييدها بالفترة يُنتج رقماً بلا معنى (ذمّة
         * آخر 7 أيام؟). لهذا لا تتوازن المعادلة عند فترة جزئية، وهي حالة
         * متوقعة ومعلنة في الشاشة بدل إخفائها.
         */
        val outstandingTotal = balanceRows.fold(Money.ZERO) { acc, b -> acc + b.outstanding }

        val stockValue = productRows.fold(Money.ZERO) { acc, p -> acc + p.stockValue }

        val soldByProduct = approved
            .flatMap { it.items }
            .groupBy { it.productId }
            .map { (productId, items) ->
                ProductLine(
                    productId = productId,
                    name = items.first().productName,
                    quantitySold = items.sumOf { it.quantity },
                    revenue = items.fold(Money.ZERO) { acc, item -> acc + item.net }
                )
            }
            .sortedByDescending { it.revenue.cents }
            .take(TOP_LIMIT)

        return AdminReportsUiState(
            range = range,
            metrics = AdminReportMetrics(
                invoicedTotal = invoicedTotal,
                collectedTotal = collectedTotal,
                commissionTotal = commissionTotal,
                outstandingTotal = outstandingTotal,
                ordersTotal = inRangeOrders.size,
                ordersDelivered = inRangeOrders.count { it.status == OrderStatus.DELIVERED },
                ordersCancelled = inRangeOrders.count { it.status == OrderStatus.CANCELLED },
                invoicesApproved = approved.size,
                invoicesRejected = invoiceRows.count { it.status == InvoiceStatus.REJECTED },
                buyersTotal = balanceRows.size,
                buyersOverLimit = balanceRows.count { it.isOverLimit },
                stockValueTotal = stockValue,
                lowStockCount = productRows.count { it.isLowStock || it.isOutOfStock },
            ),
            topBuyers = balanceRows.sortedByDescending { it.outstanding.cents }.take(TOP_LIMIT),
            topProducts = soldByProduct,
        )
    }

    private companion object {
        const val TOP_LIMIT = 5
    }
}