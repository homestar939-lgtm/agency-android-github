// feature_accountant/dashboard/AccountantDashboardScreen.kt
package com.agency.app.feature_accountant.dashboard

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agency.app.core.ui.components.AppCard

data class RecentTransaction(
    val id: String,
    val type: TransactionType,
    val buyerName: String,
    val amount: Double,
    val timestamp: String,
    val actorName: String,
    val actorRole: String
)

enum class TransactionType {
    INVOICE,    // فاتورة
    RECEIPT     // سند قبض
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountantDashboardScreen(
    onInvoiceClick: (String) -> Unit,
    onReceiptClick: (String) -> Unit,
    viewModel: AccountantDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHaptic.current

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "المراقبة المالية",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "عرض فقط — لا يمكن التعديل",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // مؤشر المزامنة
                    IconButton(onClick = { viewModel.refresh() }) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Outlined.Sync, "تحديث")
                        }
                    }
                    IconButton(onClick = { /* إشعارات */ }) {
                        BadgedBox(
                            badge = {
                                if (uiState.unreadNotifications > 0) {
                                    Badge { Text(uiState.unreadNotifications.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Notifications, "إشعارات")
                        }
                    }
                }
            )
        },
        bottomBar = {
            AccountantBottomNavigation(currentRoute = "accountant/dashboard")
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // بطاقات المؤشرات المالية
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FinancialStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Receipt,
                        label = "إجمالي الفواتير",
                        value = "${uiState.totalInvoices.toInt()} د.ع",
                        count = uiState.invoicesCount,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FinancialStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.ReceiptLong,
                        label = "إجمالي السندات",
                        value = "${uiState.totalReceipts.toInt()} د.ع",
                        count = uiState.receiptsCount,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FinancialStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.AccountBalance,
                        label = "إجمالي الذمم",
                        value = "${uiState.totalDebts.toInt()} د.ع",
                        count = uiState.debtorsCount,
                        color = MaterialTheme.colorScheme.error
                    )
                    FinancialStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.CheckCircle,
                        label = "المحصّل اليوم",
                        value = "${uiState.todayCollected.toInt()} د.ع",
                        count = uiState.todayReceiptsCount,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // التنبيهات المهمة
            if (uiState.alerts.isNotEmpty()) {
                item {
                    AppCard(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Warning,
                                    "تنبيه",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "تنبيهات مهمة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            uiState.alerts.forEach { alert ->
                                Text(
                                    "• $alert",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // عنوان النشاط الأخير
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "النشاط المالي الأخير",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { /* عرض الكل */ }) {
                        Text("عرض الكل")
                    }
                }
            }

            // قائمة المعاملات الأخيرة
            if (uiState.isLoading) {
                item { LoadingShimmer() }
            } else if (uiState.recentTransactions.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Receipt,
                        title = "لا توجد معاملات حالياً",
                        subtitle = "ستظهر الفواتير والسندات هنا فور إنشائها"
                    )
                }
            } else {
                items(uiState.recentTransactions, key = { it.id }) { transaction ->
                    TransactionCard(
                        transaction = transaction,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            when (transaction.type) {
                                TransactionType.INVOICE -> onInvoiceClick(transaction.id)
                                TransactionType.RECEIPT -> onReceiptClick(transaction.id)
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun FinancialStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    count: Int,
    color: Color
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = color.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, label, tint = color, modifier = Modifier.size(22.dp))
                    }
                }
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        "$count",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun TransactionCard(
    transaction: RecentTransaction,
    onClick: () -> Unit
) {
    val isInvoice = transaction.type == TransactionType.INVOICE
    val icon = if (isInvoice) Icons.Filled.Receipt else Icons.Filled.ReceiptLong
    val color = if (isInvoice) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.tertiary
    val label = if (isInvoice) "فاتورة" else "سند قبض"

    AppCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = color.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, label, tint = color)
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = color,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            transaction.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        transaction.buyerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "بواسطة: ${transaction.actorName} (${transaction.actorRole})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "${transaction.amount.toInt()} د.ع",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Icon(
                    Icons.Filled.ChevronLeft,
                    "عرض",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingShimmer() {
    repeat(4) {
        AppCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            title,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
