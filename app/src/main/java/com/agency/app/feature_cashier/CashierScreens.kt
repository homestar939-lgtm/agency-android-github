package com.agency.app.feature_cashier

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agency.app.core.domain.model.Invoice
import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.ui.components.AppButton
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.AppTextField
import com.agency.app.core.ui.components.BadgePill
import com.agency.app.core.ui.components.EmptyState
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.SectionHeader
import com.agency.app.core.ui.components.formatted
import com.agency.app.core.ui.components.rememberBiometricGate
import com.agency.app.core.util.formattedDateTime
import kotlinx.coroutines.launch

/**
 * تفاصيل فاتورة — الاعتماد أو الرفض.
 *
 * الاعتماد يمرّ عبر [com.agency.app.core.domain.repository.InvoiceRepository.approve]
 * التي تتحقق من الدور ومن الحالة. الاعتماد ليس مجرد تغيير حالة: هو اللحظة
 * التي تدخل فيها الفاتورة في ذمّة المشتري.
 */
@Composable
fun CashierInvoiceDetailScreen(
    viewModel: CashierInvoiceDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    var rejectReason by remember { mutableStateOf("") }
    var showReject by remember { mutableStateOf(false) }
    val invoice = state.invoice

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        if (invoice == null) {
            Column(modifier = Modifier.padding(innerPadding)) {
                EmptyState(
                    title = "الفاتورة غير متاحة",
                    actionLabel = "رجوع",
                    onAction = onBack,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                InvoiceHead(invoice)

                AppCard {
                    SectionHeader(title = "الأصناف")
                    invoice.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${item.quantity} ${item.unit} × ${item.unitPrice.formatted()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(item.net.formatted(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    InfoRow(label = "المجموع", value = invoice.subtotal.formatted())
                    InfoRow(label = "الخصم", value = invoice.discountTotal.formatted())
                    InfoRow(label = "العمولة", value = invoice.commission.formatted())
                    InfoRow(
                        label = "الإجمالي المستحق",
                        value = invoice.grandTotal.formatted(),
                        emphasise = true,
                        valueColor = MaterialTheme.colorScheme.primary,
                    )
                }

                if (state.error != null || state.notice != null) {
                    AppCard(
                        containerColor = if (state.error != null) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Text(
                            text = state.error ?: state.notice.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                if (invoice.status == InvoiceStatus.PENDING_APPROVAL) {
                    AppCard {
                        SectionHeader(title = "القرار")
                        Spacer(Modifier.height(6.dp))
                        AppButton(
                            text = "اعتماد الفاتورة",
                            onClick = viewModel::approve,
                            loading = state.isWorking,
                            icon = Icons.Filled.CheckCircle,
                        )
                        Spacer(Modifier.height(8.dp))
                        if (!showReject) {
                            AppButton(
                                text = "رفض الفاتورة",
                                onClick = { showReject = true },
                                containerColor = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            AppTextField(
                                value = rejectReason,
                                onValueChange = { rejectReason = it },
                                label = "سبب الرفض",
                                singleLine = false,
                            )
                            Spacer(Modifier.height(8.dp))
                            AppButton(
                                text = "تأكيد الرفض",
                                onClick = { viewModel.reject(rejectReason) },
                                loading = state.isWorking,
                                containerColor = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                } else if (invoice.status == InvoiceStatus.APPROVED) {
                    AppCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            text = "الفاتورة معتمدة ومضافة إلى ذمة المشتري.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (invoice.approvedAt != null) {
                            InfoRow(label = "تاريخ الاعتماد", value = invoice.approvedAt.formattedDateTime())
                        }
                    }
                } else if (invoice.status == InvoiceStatus.REJECTED) {
                    AppCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Text(
                            text = "سبب الرفض: ${invoice.rejectionReason ?: "غير مذكور"}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                AppButton(text = "رجوع", onClick = onBack)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun InvoiceHead(invoice: Invoice) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = invoice.invoiceNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = invoice.buyerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BadgePill(
                text = invoice.status.label(),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(8.dp))
        InfoRow(label = "أُنشئت", value = invoice.createdAt.formattedDateTime())
    }
}

/**
 * شاشة التحصيل — إنشاء سند قبض.
 *
 * لو تجاوز المبلغ [com.agency.app.core.domain.model.Receipt.BIOMETRIC_THRESHOLD]
 * يُطلب التحقق بالبصمة قبل التسجيل. البصمة هنا ليست تجميلاً: السند يخفّض
 * ذمّة المشتري، وهو أثر مالي دائم.
 */
@Composable
fun CashierCollectScreen(
    viewModel: CollectViewModel,
    initialBuyerId: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val gate = rememberBiometricGate()
    val scope = rememberCoroutineScope()
    var biometricOk by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(initialBuyerId) {
        if (!initialBuyerId.isNullOrBlank()) viewModel.selectBuyer(initialBuyerId)
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppCard {
                SectionHeader(title = "المشتري")
                state.buyers.forEach { buyer ->
                    val balance = state.balances[buyer.id]
                    val selected = state.selectedBuyerId == buyer.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = buyer.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            )
                            Text(
                                text = "الذمة: ${balance?.outstanding?.formatted() ?: "—"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (balance?.isOverLimit == true) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        AppButton(
                            text = if (selected) "مختار" else "اختر",
                            onClick = { viewModel.selectBuyer(buyer.id) },
                            modifier = Modifier.height(40.dp),
                            enabled = !selected,
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }
            }

            AppCard {
                SectionHeader(title = "بيانات السند")
                AppTextField(
                    value = state.amountText,
                    onValueChange = viewModel::onAmountChange,
                    label = "المبلغ",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                )
                Spacer(Modifier.height(8.dp))
                Text("طريقة الدفع", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    com.agency.app.core.domain.model.PaymentMethod.entries.forEach { method ->
                        androidx.compose.material3.FilterChip(
                            selected = state.method == method,
                            onClick = { viewModel.onMethodChange(method) },
                            label = { Text(method.label()) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = state.reference,
                    onValueChange = viewModel::onReferenceChange,
                    label = "المرجع (رقم الحوالة/الشيك)",
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = state.note,
                    onValueChange = viewModel::onNoteChange,
                    label = "ملاحظة",
                    singleLine = false,
                )
            }

            if (state.requiresBiometric) {
                AppCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = null,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (biometricOk) {
                                "تم التحقق بالبصمة ✓"
                            } else {
                                "هذا المبلغ يتجاوز حد البصمة؛ يجب التحقق قبل التسجيل."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (!biometricOk) {
                        Spacer(Modifier.height(8.dp))
                        AppButton(
                            text = "تحقق بالبصمة",
                            onClick = {
                                val authenticator = gate
                                if (authenticator == null || !authenticator.canAuthenticate()) {
                                    biometricOk = false
                                    return@AppButton
                                }
                                scope.launch {
                                    biometricOk = authenticator.authenticate(
                                        title = "تأكيد التحصيل",
                                        subtitle = "سند بمبلغ ${state.amountText}",
                                    )
                                }
                            },
                            icon = Icons.Filled.Fingerprint,
                        )
                    }
                }
            }

            if (state.error != null || state.notice != null) {
                AppCard(
                    containerColor = if (state.error != null) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = state.error ?: state.notice.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            AppButton(
                text = "تسجيل سند القبض",
                onClick = { viewModel.collect(biometricSatisfied = biometricOk) },
                loading = state.isCollecting,
                enabled = state.selectedBuyerId != null && state.amountText.isNotBlank(),
            )
            AppButton(text = "رجوع", onClick = onBack)
            Spacer(Modifier.height(16.dp))
        }
    }
}

internal fun com.agency.app.core.domain.model.PaymentMethod.label(): String = when (this) {
    com.agency.app.core.domain.model.PaymentMethod.CASH -> "نقداً"
    com.agency.app.core.domain.model.PaymentMethod.TRANSFER -> "حوالة"
    com.agency.app.core.domain.model.PaymentMethod.CHECK -> "شيك"
}