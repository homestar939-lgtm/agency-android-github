package com.agency.app.feature_buyer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
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
import com.agency.app.core.domain.model.Order
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.EmptyState
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.StatusBadge
import com.agency.app.core.ui.components.formatted
import com.agency.app.core.util.formattedDateTime

/** سجل طلبيات المشتري — تُقرأ من Room مباشرة. */
@Composable
fun BuyerOrdersScreen(
    viewModel: BuyerOrdersViewModel,
    onOpenOrder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val orders by viewModel.orders.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        if (orders.isEmpty()) {
            Column(modifier = Modifier.padding(innerPadding)) {
                EmptyState(
                    title = "لا توجد طلبيات",
                    description = "أنشئ طلبيتك الأولى من الكتالوج.",
                    icon = Icons.Filled.Receipt,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
            ) {
                items(orders, key = { it.id }) { order ->
                    OrderRow(order = order, onClick = { onOpenOrder(order.id) })
                }
            }
        }
    }
}

@Composable
private fun OrderRow(order: Order, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = order.orderNumber,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            StatusBadge(status = order.status)
        }
        Spacer(Modifier.height(8.dp))
        InfoRow(label = "عدد القطع", value = order.itemsCount.toString())
        InfoRow(label = "الإجمالي", value = order.total.formatted(), emphasise = false)
        InfoRow(label = "التاريخ", value = order.createdAt.formattedDateTime())
    }
}