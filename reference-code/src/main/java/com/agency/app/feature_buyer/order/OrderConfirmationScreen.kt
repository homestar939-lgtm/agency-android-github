// feature_buyer/order/OrderConfirmationScreen.kt
package com.agency.app.feature_buyer.order

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
import com.agency.app.core.ui.components.AppButton
import com.agency.app.core.ui.components.AppCard

data class OrderSummaryItem(
    val productName: String,
    val quantity: Int,
    val unit: String,
    val unitPrice: Double,
    val totalPrice: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderConfirmationScreen(
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    viewModel: OrderConfirmationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHaptic.current

    LaunchedEffect(Unit) {
        viewModel.loadOrderSummary()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تأكيد الطلبية", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { /* العودة */ }) {
                        Icon(Icons.Filled.ArrowBack, "رجوع")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // الملخص المالي الكامل
                    SummaryRow("المجموع الفرعي", "${uiState.subtotal} د.ع")
                    SummaryRow("الضريبة", "${uiState.tax} د.ع")
                    if (uiState.discount > 0) {
                        SummaryRow("الخصم", "- ${uiState.discount} د.ع", isNegative = true)
                    }
                    HorizontalDivider()
                    SummaryRow(
                        "الإجمالي النهائي",
                        "${uiState.total} د.ع",
                        isTotal = true
                    )

                    // أزرار الإجراءات
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Filled.Edit, "تعديل")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تعديل")
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.confirmOrder()
                                onConfirm()
                            },
                            modifier = Modifier
                                .weight(2f)
                                .height(56.dp),
                            shape = MaterialTheme.shapes.large,
                            enabled = !uiState.isConfirming
                        ) {
                            if (uiState.isConfirming) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.Send, "إرسال")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "تأكيد وإرسال",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // معلومات المشتري
            item {
                AppCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                "المشتري",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "معلومات المشتري",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        InfoRow("الاسم", uiState.buyerName)
                        InfoRow("رقم الهاتف", uiState.buyerPhone)
                        InfoRow("العنوان", uiState.buyerAddress)
                    }
                }
            }

            // معلومات الوكالة
            item {
                AppCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Store,
                                "الوكالة",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "الوكالة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        InfoRow("اسم الوكالة", uiState.agencyName)
                        InfoRow("العنوان", uiState.agencyAddress)
                    }
                }
            }

            // قائمة الأصناف
            item {
                Text(
                    "الأصناف (${uiState.items.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(uiState.items, key = { it.productName }) { item ->
                AppCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.productName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${item.quantity} ${item.unit} × ${item.unitPrice} د.ع",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "${item.totalPrice} د.ع",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ملاحظات
            if (uiState.notes.isNotBlank()) {
                item {
                    AppCard(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Filled.Notes,
                                "ملاحظات",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Column {
                                Text(
                                    "ملاحظات",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    uiState.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    isTotal: Boolean = false,
    isNegative: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = if (isTotal) MaterialTheme.typography.titleMedium
                    else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            value,
            style = if (isTotal) MaterialTheme.typography.titleLarge
                    else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.SemiBold,
            color = when {
                isTotal -> MaterialTheme.colorScheme.primary
                isNegative -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
