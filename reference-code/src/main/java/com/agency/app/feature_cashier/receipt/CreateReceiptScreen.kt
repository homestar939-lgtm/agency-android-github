// feature_cashier/receipt/CreateReceiptScreen.kt
package com.agency.app.feature_cashier.receipt

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.AppTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReceiptScreen(
    buyerId: String?,
    onReceiptCreated: (String) -> Unit,
    viewModel: CreateReceiptViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHaptic.current
    val scrollState = rememberScrollState()

    LaunchedEffect(buyerId) {
        if (buyerId != null) {
            viewModel.loadBuyer(buyerId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("سند قبض جديد", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { /* العودة */ }) {
                        Icon(Icons.Filled.ArrowBack, "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.canCreate) {
                ExtendedFloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.createReceipt { receiptId ->
                            onReceiptCreated(receiptId)
                        }
                    },
                    icon = { Icon(Icons.Filled.Check, "إنشاء") },
                    text = { Text("إنشاء وإرسال") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    enabled = !uiState.isCreating
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // بطاقة معلومات المشتري
            AppCard(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    uiState.buyerName.take(1),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                uiState.buyerName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                uiState.buyerPhone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )

                    // معلومات الذمم
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "إجمالي الذمة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                "${uiState.totalDebt.toInt()} د.ع",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "المستحق الآن",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                "${uiState.currentDue.toInt()} د.ع",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // حقل المبلغ
            AppCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "المبلغ المستلم",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // المبلغ بالأرقام
                    AppTextField(
                        value = uiState.amountInput,
                        onValueChange = { viewModel.updateAmount(it) },
                        label = "المبلغ",
                        placeholder = "0",
                        leadingIcon = { Icon(Icons.Outlined.AttachMoney, "المبلغ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = uiState.amountError != null,
                        supportingText = uiState.amountError?.let { { Text(it) } }
                    )

                    // المبلغ بالحروف (تلقائي)
                    if (uiState.amountInWords.isNotBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "المبلغ بالحروف",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    uiState.amountInWords,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // اختيار العملة
                    ExposedDropdownMenuBox(
                        expanded = uiState.isCurrencyDropdownExpanded,
                        onExpandedChange = { viewModel.toggleCurrencyDropdown() }
                    ) {
                        OutlinedTextField(
                            value = uiState.selectedCurrencyName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("العملة") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = uiState.isCurrencyDropdownExpanded
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = uiState.isCurrencyDropdownExpanded,
                            onDismissRequest = { viewModel.toggleCurrencyDropdown() }
                        ) {
                            uiState.currencies.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency.name) },
                                    onClick = {
                                        viewModel.selectCurrency(currency)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // طريقة الدفع
            AppCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "طريقة الدفع",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentMethodChip(
                            text = "نقدي",
                            icon = Icons.Filled.Money,
                            selected = uiState.paymentMethod == "CASH",
                            onClick = { viewModel.selectPaymentMethod("CASH") }
                        )
                        PaymentMethodChip(
                            text = "تحويل",
                            icon = Icons.Filled.AccountBalance,
                            selected = uiState.paymentMethod == "TRANSFER",
                            onClick = { viewModel.selectPaymentMethod("TRANSFER") }
                        )
                        PaymentMethodChip(
                            text = "شيك",
                            icon = Icons.Filled.Receipt,
                            selected = uiState.paymentMethod == "CHECK",
                            onClick = { viewModel.selectPaymentMethod("CHECK") }
                        )
                    }
                }
            }

            // الملاحظات
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.updateNotes(it) },
                label = { Text("ملاحظات (اختياري)") },
                placeholder = { Text("أي ملاحظات إضافية") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 4
            )

            // معاينة الأثر المالي
            AppCard(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "الأثر المالي",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "الذمة بعد السند",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            "${uiState.remainingDebt.toInt()} د.ع",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "عمولتي من هذا السند",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            "${uiState.myCommissionFromThis.toInt()} د.ع",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun PaymentMethodChip(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = {
            Icon(icon, text, modifier = Modifier.size(18.dp))
        },
        modifier = Modifier.weight(1f)
    )
}
