// feature_warehouse/orders/PreparationScreen.kt
package com.agency.app.feature_warehouse.orders

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agency.app.core.database.entity.OrderItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreparationScreen(
    orderId: String,
    onPreparationComplete: () -> Unit,
    viewModel: PreparationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHaptic.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تحضير الطلبية", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { /* العودة */ }) {
                        Icon(Icons.Filled.ArrowBack, "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.progress == 100) {
                ExtendedFloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.confirmPreparation()
                        onPreparationComplete()
                    },
                    icon = { Icon(Icons.Filled.CheckCircle, "تأكيد") },
                    text = { Text("تأكيد التحضير") },
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // شريط التقدم الدائري
            PreparationProgressHeader(
                progress = uiState.progress,
                totalItems = uiState.totalItems,
                preparedItems = uiState.preparedItems
            )

            // قائمة الأصناف
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.items, key = { it.productId }) { item ->
                    PreparationItemCard(
                        item = item,
                        isPrepared = uiState.preparedItems.contains(item.productId),
                        onToggle = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.toggleItemPreparation(item.productId)
                        },
                        onScanBarcode = {
                            viewModel.scanBarcode(item.productId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PreparationProgressHeader(
    progress: Int,
    totalItems: Int,
    preparedItems: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // العدّاد الدائري
            CircularProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.size(120.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Text(
                text = "$progress%",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "$preparedItems من $totalItems صنف",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // شريط التقدم الخطي
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun PreparationItemCard(
    item: OrderItem,
    isPrepared: Boolean,
    onToggle: () -> Unit,
    onScanBarcode: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrepared) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // معلومات الصنف
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.quantity} ${item.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // أزرار الإجراءات
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onScanBarcode) {
                    Icon(
                        Icons.Filled.QrCodeScanner,
                        "مسح الباركود",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Checkbox(
                    checked = isPrepared,
                    onCheckedChange = { onToggle() }
                )
            }
        }
    }
}
