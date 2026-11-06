// feature_accountant/invoices/InvoicesListScreen.kt
package com.agency.app.feature_accountant.invoices

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
import com.agency.app.core.ui.components.AppCard

data class InvoiceSummary(
    val id: String,
    val invoiceNumber: String,
    val buyerName: String,
    val amount: Double,
    val date: String,
    val status: InvoiceStatus,
    val createdBy: String
)

enum class InvoiceStatus {
    PENDING,    // بانتظار الاعتماد
    APPROVED,   // معتمدة
    REJECTED    // مرفوضة
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesListScreen(
    onInvoiceClick: (String) -> Unit,
    viewModel: InvoicesListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHaptic.current
    var selectedStatus by remember { mutableStateOf("ALL") }

    LaunchedEffect(selectedStatus) {
        viewModel.loadInvoices(selectedStatus)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الفواتير", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* بحث */ }) {
                        Icon(Icons.Filled.Search, "بحث")
                    }
                    IconButton(onClick = { /* فلترة */ }) {
                        Icon(Icons.Filled.FilterList, "فلترة")
                    }
                }
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
                selectedTabIndex = when (selectedStatus) {
                    "ALL" -> 0
                    "PENDING" -> 1
                    "APPROVED" -> 2
                    "REJECTED" -> 3
                    else -> 0
                },
                edgePadding = 16.dp
            ) {
                Tab(
                    selected = selectedStatus == "ALL",
                    onClick = { selectedStatus = "ALL" },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("الكل")
                            Badge { Text(uiState.allCount.toString()) }
                        }
                    }
                )
                Tab(
                    selected = selectedStatus == "PENDING",
                    onClick = { selectedStatus = "PENDING" },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("بانتظار الاعتماد")
                            Badge { Text(uiState.pendingCount.toString()) }
                        }
                    }
                )
                Tab(
                    selected = selectedStatus == "APPROVED",
                    onClick = { selectedStatus = "APPROVED" },
                    text = { Text("معتمدة") }
                )
                Tab(
                    selected = selectedStatus == "REJECTED",
                    onClick = { selectedStatus = "REJECTED" },
                    text = { Text("مرفوضة") }
                )
            }

            // قائمة الفواتير
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.invoices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Receipt,
                            "لا توجد فواتير",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            "لا توجد فواتير في هذه الحالة",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.invoices, key = { it.id }) { invoice ->
                        InvoiceCard(
                            invoice = invoice,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onInvoiceClick(invoice.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceCard(
    invoice: InvoiceSummary,
    onClick: () -> Unit
) {
    val statusColor = when (invoice.status) {
        InvoiceStatus.PENDING -> MaterialTheme.colorScheme.tertiary
        InvoiceStatus.APPROVED -> MaterialTheme.colorScheme.secondary
        InvoiceStatus.REJECTED -> MaterialTheme.colorScheme.error
    }

    val statusText = when (invoice.status) {
        InvoiceStatus.PENDING -> "بانتظار الاعتماد"
        InvoiceStatus.APPROVED -> "معتمدة"
        InvoiceStatus.REJECTED -> "مرفوضة"
    }

    AppCard(onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            invoice.invoiceNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = statusColor.copy(alpha = 0.12f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                statusText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        invoice.buyerName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${invoice.amount.toInt()} د.ع",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        invoice.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "أنشأها: ${invoice.createdBy}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.Filled.Visibility,
                    "عرض",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
