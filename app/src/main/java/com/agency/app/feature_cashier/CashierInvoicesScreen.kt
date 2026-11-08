package com.agency.app.feature_cashier

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
import com.agency.app.core.ui.components.EmptyState
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.formatted
import com.agency.app.core.ui.theme.LocalAgencyAccents
import com.agency.app.core.util.formattedDate
import com.agency.app.core.util.relative

/** قائمة الفواتير في الصندوق. */
@Composable
fun CashierInvoicesScreen(
    viewModel: CashierInvoicesViewModel,
    onOpenInvoice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InvoiceFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            val count = filter.status?.let { state.counts[it] ?: 0 }
                            Text(if (count == null) filter.label else "${filter.label} ($count)")
                        },
                    )
                }
            }

            if (state.invoices.isEmpty()) {
                EmptyState(
                    title = "لا فواتير في هذا التصنيف",
                    description = "تُنشأ الفواتير من الطلبيات الجاهزة أو المرسلة.",
                    icon = Icons.Filled.ReceiptLong,
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(state.invoices, key = { it.id }) { invoice ->
                        InvoiceRow(invoice = invoice, onClick = { onOpenInvoice(invoice.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceRow(invoice: Invoice, onClick: () -> Unit) {
    val accents = LocalAgencyAccents.current
    AppCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = invoice.invoiceNumber,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            BadgePill(text = invoice.status.label(), color = invoiceColor(invoice.status, accents))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = invoice.buyerName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        InfoRow(label = "الإجمالي", value = invoice.grandTotal.formatted(), emphasise = true)
        InfoRow(label = "التاريخ", value = invoice.createdAt.formattedDate())
        Text(
            text = invoice.updatedAt.relative(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun invoiceColor(
    status: InvoiceStatus,
    accents: com.agency.app.core.ui.theme.AgencyAccents,
) = when (status) {
    InvoiceStatus.DRAFT -> accents.info
    InvoiceStatus.PENDING_APPROVAL -> accents.warning
    InvoiceStatus.APPROVED -> accents.success
    InvoiceStatus.REJECTED -> MaterialTheme.colorScheme.error
    InvoiceStatus.ARCHIVED -> MaterialTheme.colorScheme.outline
}

internal fun InvoiceStatus.label(): String = when (this) {
    InvoiceStatus.DRAFT -> "مسودة"
    InvoiceStatus.PENDING_APPROVAL -> "بانتظار الاعتماد"
    InvoiceStatus.APPROVED -> "معتمدة"
    InvoiceStatus.REJECTED -> "مرفوضة"
    InvoiceStatus.ARCHIVED -> "مؤرشفة"
}