// feature_warehouse/orders/OrdersScreen.kt
package com.agency.app.feature_warehouse.orders

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agency.app.core.database.entity.Order
import com.agency.app.core.database.entity.OrderStatus
import com.agency.app.core.ui.components.AppCard
import kotlinx.datetime.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onOrderClick: (String) -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHaptic.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الطلبات الواردة", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refreshOrders() }) {
                        Icon(Icons.Outlined.Sync, "تحديث")
                    }
                    IconButton(onClick = { /* فتح قائمة الفائض */ }) {
                        Icon(Icons.Outlined.MoreVert, "المزيد")
                    }
                }
            )
        },
        bottomBar = {
            // شريط التنقل السفلي للمخزن
            WarehouseBottomNavigation()
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.toggleSpeedDial() },
                icon = { Icon(Icons.Filled.Bolt, "إجراء سريع") },
                text = { Text("إجراء سريع") },
                expanded = uiState.isSpeedDialExpanded
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // تبويبات الحالات
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedStatusIndex,
                edgePadding = 16.dp
            ) {
                OrderStatus.values().forEachIndexed { index, status ->
                    Tab(
                        selected = uiState.selectedStatusIndex == index,
                        onClick = {
                            viewModel.selectStatus(index)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(getStatusText(status))
                                val count = uiState.ordersCountByStatus[status] ?: 0
                                if (count > 0) {
                                    Badge { Text(count.toString()) }
                                }
                            }
                        }
                    )
                }
            }

            // قائمة الطلبات
            if (uiState.isLoading) {
                LoadingShimmer()
            } else if (uiState.orders.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Inbox,
                    text = "لا توجد طلبات في هذه الحالة",
                    onRefresh = { viewModel.refreshOrders() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.orders, key = { it.id }) { order ->
                        OrderCard(
                            order = order,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onOrderClick(order.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: Order,
    onClick: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // الجانب الأيمن - معلومات الطلبية
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = order.buyerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${order.items.size} أصناف",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDateTime(order.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // الجانب الأيسر - المبلغ والحالة
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${order.totalAmount} ${order.currency}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                StatusBadge(status = order.status)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: OrderStatus) {
    val (color, text) = when (status) {
        OrderStatus.NEW -> MaterialTheme.colorScheme.secondary to "جديد"
        OrderStatus.PREPARING -> MaterialTheme.colorScheme.tertiary to "قيد التحضير"
        OrderStatus.PREPARED -> MaterialTheme.colorScheme.primary to "تم التحضير"
        OrderStatus.SENT -> MaterialTheme.colorScheme.secondary to "تم الإرسال"
        OrderStatus.DELIVERED -> MaterialTheme.colorScheme.tertiary to "تم التسليم"
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error to "ملغي"
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}
