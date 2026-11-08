package com.agency.app.feature_warehouse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agency.app.core.domain.model.Money
import com.agency.app.core.ui.arabic
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.BadgePill
import com.agency.app.core.ui.components.CurrencyText
import com.agency.app.core.ui.components.EmptyState
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.SectionHeader
import com.agency.app.core.ui.components.LoadingShimmer
import com.agency.app.core.ui.components.formatted

/**
 * إحصائيات المخزن — قياس لا عرض.
 *
 * كل بطاقة هنا تقود إلى قرار: قيمة المخزون تقود إلى رأس المال المحتجز،
 * والمنتجات الناقصة تقود إلى أمر شراء، والدخل المحتمل يقود إلى توقّع.
 * لهذا لا تُعرض أرقام بلا شرح — رقم بلا معنى يُتجاهل.
 */
@Composable
fun WarehouseStatsScreen(
    viewModel: WarehouseStatsViewModel,
    onBack: () -> Unit = {},
) {
    val state by viewModel.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إحصائيات المخزن", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding)) { LoadingShimmer() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    SectionHeader("صحة المخزون")
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${state.stockHealthPercent}%",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (state.hasIssues) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${state.activeProducts} منتج نشط",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (state.hasIssues) {
                                Text(
                                    "${state.outOfStock} نفد · ${state.lowStock} منخفض",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { state.stockHealthPercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (state.hasIssues) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    /*
                     * النسبة محسوبة على المنتجات التي لا تحتاج تدخلاً. وُضع
                     * الشرح تحتها لأن نسبة بلا تعريف تُقرأ خطأً: 60% قد تعني
                     * «سليم» أو «نصف المخزن ناقص» بحسب ما قيس.
                     */
                    Text(
                        "النسبة = المنتجات التي لا تحتاج تدخلاً ÷ المنتجات النشطة",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                AppCard {
                    SectionHeader("القيمة المالية")
                    InfoRow("قيمة المخزون بالتكلفة", state.stockValue.formatted())
                    InfoRow("الإيراد المحتمل بالبيع", state.potentialRevenue.formatted())
                    InfoRow(
                        label = "الهامش المتوقع",
                        value = state.projectedMargin.formatted(),
                        valueColor = MaterialTheme.colorScheme.primary,
                        emphasise = true,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "التكلفة هي الخسارة الفعلية لو تلف المخزون؛ الإيراد احتمال. " +
                            "رفع الرقم الأول ليطابق الثاني يجعل قرار الشراء متفائلاً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                AppCard {
                    SectionHeader("العمل المتراكم")
                    InfoRow("وحدات في المخزن", "${state.totalStockUnits}")
                    InfoRow("طلبيات تحتاج تحضيراً", "${state.ordersToPrepare}")
                    InfoRow("فواتير بانتظار الاعتماد", "${state.pendingInvoices}")
                    InfoRow("إجمالي المنتجات (مع الموقوفة)", "${state.totalProducts}")
                }
            }
        }
    }
}

/** حركة المخزون — الصادر المستنبط من الطلبيات الحقيقية. */
@Composable
fun StockMovementScreen(
    viewModel: StockMovementViewModel,
    onBack: () -> Unit = {},
) {
    val state by viewModel.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("حركة المخزون", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding)) { LoadingShimmer() }
            state.isEmpty -> Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    title = "لا حركة بعد",
                    description = "لم تُسجَّل طلبيات على منتجات نشطة حتى الآن.",
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    AppCard {
                        InfoRow("إجمالي الوحدات المطلوبة", "${state.totalOutgoing}")
                        InfoRow(
                            label = "منتجات ستنفد بعد التنفيذ",
                            value = "${state.criticalCount}",
                            valueColor = if (state.criticalCount > 0)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                            emphasise = state.criticalCount > 0,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "الصادر مستنبط من الطلبيات غير الملغاة. الطلبية الملغاة " +
                                "لا تُخصم — وهذا سبب عدم وجود جدول حركة منفصل: " +
                                "مصدر حقيقة ثانٍ للأرصدة يفتح باب التناقض.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.rows, key = { it.productId }) { row ->
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(row.productName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${row.currentStock} ${row.unit} في المخزن · " +
                                        "${row.outgoingUnits} مطلوبة في ${row.ordersCount} طلبية",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${row.remainingAfterPending}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (row.isCritical) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "المتبقي",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (row.isCritical) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "تحت الحد الأدنى بعد تنفيذ الطلبيات القائمة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** تنبيهات المخزون — قائمة عمل مع كمية مقترحة جاهزة للطلب. */
@Composable
fun StockAlertsScreen(
    viewModel: StockAlertsViewModel,
    onBack: () -> Unit = {},
) {
    val state by viewModel.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنبيهات المخزون", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding)) { LoadingShimmer() }
            state.isEmpty -> Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    title = "كل الأرصدة سليمة",
                    description = "لا منتج نفد ولا اقترب من الحد الأدنى.",
                    icon = Icons.Filled.CheckCircle,
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    AppCard {
                        InfoRow(
                            label = "نفد من المخزن",
                            value = "${state.outOfStockCount}",
                            valueColor = MaterialTheme.colorScheme.error,
                            emphasise = state.outOfStockCount > 0,
                        )
                        InfoRow("قارب على النفاد", "${state.lowCount}")
                    }
                }
                items(state.alerts, key = { it.productId }) { row ->
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(row.productName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "SKU ${row.sku} · الحد الأدنى ${row.minOrderQuantity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            BadgePill(
                                text = if (row.isOutOfStock) "نفد" else "منخفض",
                                color = if (row.isOutOfStock) MaterialTheme.colorScheme.error
                                else Color(0xFFE8A33D),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "المتوفر: ${row.currentStock}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "اطلب: ${row.suggestedOrder}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** فواتير المخزن — عرض وقراءة، بلا اعتماد ولا تعديل. */
@Composable
fun WarehouseInvoicesScreen(
    viewModel: WarehouseInvoicesViewModel,
    onOpenInvoice: (String) -> Unit,
    onBack: () -> Unit = {},
) {
    val state by viewModel.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("فواتير المخزن", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WarehouseInvoiceFilter.entries.forEach { option ->
                    BadgePill(
                        text = option.label,
                        color = if (state.filter == option) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setFilter(option) },
                    )
                }
            }
            AppCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                InfoRow(
                    label = "إجمالي المعروض (${state.visible.size} فاتورة)",
                    value = state.visibleTotal.formatted(),
                    emphasise = true,
                )
                Text(
                    "هذه الشاشة للعرض فقط: مخزن يجمع ويجهّز، والمال يعتمده المدير.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                state.isLoading -> Box(Modifier.fillMaxSize()) { LoadingShimmer() }
                state.isEmpty -> EmptyState(
                    title = "لا فواتير",
                    description = "لا فواتير مطابقة للتصنيف المختار.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.visible, key = { it.id }) { invoice ->
                        AppCard(onClick = { onOpenInvoice(invoice.id) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        invoice.invoiceNumber,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        "${invoice.buyerName} · ${invoice.items.size} بند",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                CurrencyText(
                                    amount = invoice.grandTotal,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** عارض الفاتورة — تفصيل كامل محسوب من الفاتورة نفسها. */
@Composable
fun InvoiceViewerScreen(
    invoiceId: String,
    viewModel: InvoiceViewerViewModel,
    onBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(invoiceId) { viewModel.load(invoiceId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("عرض الفاتورة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        when (val current = state) {
            InvoiceViewerState.Loading ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            is InvoiceViewerState.Failed ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    EmptyState(title = "تعذّر تحميل الفاتورة", description = current.message)
                }
            is InvoiceViewerState.Found -> {
                val invoice = current.invoice
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        AppCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        invoice.invoiceNumber,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        invoice.buyerName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                BadgePill(
                                    text = invoice.status.arabic(),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    item {
                        AppCard {
                            SectionHeader("البنود")
                            invoice.items.forEach { item ->
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            item.productName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            "${item.quantity} × ${item.unitPrice.formatted()} ${item.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        if (item.discount.isPositive) {
                                            Text(
                                                "خصم ${item.discount.formatted()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                    CurrencyText(item.net, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    item {
                        AppCard {
                            InfoRow("المجموع", invoice.subtotal.formatted())
                            if (invoice.discountTotal.isPositive) {
                                InfoRow("الخصومات", invoice.discountTotal.formatted())
                            }
                            InfoRow("قبل العمولة", invoice.total.formatted())
                            if (invoice.commission.isPositive) {
                                InfoRow("العمولة", invoice.commission.formatted())
                            }
                            Spacer(Modifier.height(8.dp))
                            InfoRow(
                                label = "الإجمالي المستحق",
                                value = invoice.grandTotal.formatted(),
                                valueColor = MaterialTheme.colorScheme.primary,
                                emphasise = true,
                            )
                            /*
                             * المعادلة تُعرض حيّة، فهي اختبار للبيانات لا زينة:
                             * لو انحرف أي بند عن مجموعه، يظهر التناقض هنا
                             * قبل أن يصل إلى تحصيل.
                             */
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "التحقق: ${invoice.subtotal.formatted()} − " +
                                    "${invoice.discountTotal.formatted()} + " +
                                    "${invoice.commission.formatted()} = " +
                                    invoice.grandTotal.formatted(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}