package com.agency.app.feature_buyer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agency.app.core.domain.model.Invoice
import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.BadgePill
import com.agency.app.core.ui.components.CurrencyText
import com.agency.app.core.ui.components.EmptyState
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.formatted
import com.agency.app.core.ui.theme.LocalAgencyAccents
import com.agency.app.core.util.formattedDate

/**
 * فواتير المشتري.
 *
 * الفاتورة المسودّة تظهر بوسم واضح «غير معتمدة»، لأن إظهارها كدين مستحق
 * قبل اعتماد المدير يُفزع المشتري بمبلغ غير ملزم. الرقم المعروض في الترويسة
 * هو ذمّة المشتري الفعلية من القاعدة، لا مجموع ما على الشاشة.
 */
@Composable
fun BuyerInvoicesScreen(
    viewModel: BuyerInvoicesViewModel,
    onOpenStatement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val accents = LocalAgencyAccents.current

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            state.balance?.let { balance ->
                AppCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    onClick = onOpenStatement,
                ) {
                    Text(
                        text = "ذمّتك القائمة",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CurrencyText(
                        amount = balance.outstanding,
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (balance.isOverLimit) accents.warning
                        else MaterialTheme.colorScheme.onSurface,
                    )
                    if (balance.isOverLimit) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "تجاوزت الحد الائتماني — يُرجى السداد قبل طلبيات جديدة",
                            style = MaterialTheme.typography.bodySmall,
                            color = accents.warning,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "اضغط لعرض كشف الحساب التفصيلي",
                        style = MaterialTheme.typography.bodySmall,
                        color = accents.info,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BuyerInvoiceFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = filter == state.filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            val count = filter.status?.let { state.counts[it] } ?: state.counts.values.sum()
                            Text(if (count > 0) "${filter.label} ($count)" else filter.label)
                        },
                    )
                }
            }

            if (state.invoices.isEmpty()) {
                EmptyState(
                    title = "لا توجد فواتير في هذا التبويب",
                    description = "تصلك الفاتورة بعد اعتمادها من إدارة الوكالة.",
                    icon = Icons.Filled.ReceiptLong,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.invoices, key = { it.id }) { invoice ->
                        InvoiceRow(invoice = invoice)
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceRow(invoice: Invoice) {
    val accents = LocalAgencyAccents.current
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = invoice.invoiceNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = invoice.createdAt.formattedDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BadgePill(
                text = invoice.status.arabic(),
                color = when (invoice.status) {
                    InvoiceStatus.DRAFT -> MaterialTheme.colorScheme.outline
                    InvoiceStatus.PENDING_APPROVAL -> accents.warning
                    InvoiceStatus.APPROVED -> accents.success
                    InvoiceStatus.REJECTED -> MaterialTheme.colorScheme.error
                    InvoiceStatus.ARCHIVED -> accents.info
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        InfoRow(label = "عدد الأسطر", value = invoice.items.size.toString())
        InfoRow(label = "الإجمالي قبل العمولة", value = invoice.total.formatted())
        InfoRow(
            label = "الإجمالي المستحق",
            value = invoice.grandTotal.formatted(),
            emphasise = true,
        )
        if (!invoice.isApproved) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "غير معتمدة — لا تُحتسب في ذمّتك حتى يوقّعها المدير",
                style = MaterialTheme.typography.bodySmall,
                color = accents.warning,
            )
        }
        invoice.rejectionReason?.let { reason ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = "سبب الرفض: $reason",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** تسمية عربية لحالة الفاتورة — تُستخدم هنا وفي شاشة الصندوق لاحقاً. */
internal fun InvoiceStatus.arabic(): String = when (this) {
    InvoiceStatus.DRAFT -> "مسودّة"
    InvoiceStatus.PENDING_APPROVAL -> "بانتظار الاعتماد"
    InvoiceStatus.APPROVED -> "معتمدة"
    InvoiceStatus.REJECTED -> "مرفوضة"
    InvoiceStatus.ARCHIVED -> "مؤرشفة"
}