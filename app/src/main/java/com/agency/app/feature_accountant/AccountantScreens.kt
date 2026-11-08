package com.agency.app.feature_accountant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agency.app.core.domain.model.BuyerBalance
import com.agency.app.core.domain.model.BuyerStatementEntry
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.PermissionRequest
import com.agency.app.core.domain.model.PermissionRequestStatus
import com.agency.app.core.ui.components.AppButton
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.AppTextField
import com.agency.app.core.ui.components.BadgePill
import com.agency.app.core.ui.components.EmptyState
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.SectionHeader
import com.agency.app.core.ui.components.formatted
import com.agency.app.core.ui.theme.LocalAgencyAccents
import com.agency.app.core.util.formattedDateTime

/**
 * أرصدة المشترين — شاشة المحاسب الرئيسية.
 *
 * كل الأرقام هنا مشتقة في طبقة البيانات من مستنداتها (فواتير معتمدة وسندات قبض)،
 * وليست مجاميع يُحسبها العرض: مجموع «عليهم» في الأعلى يساوي مجموع الصفوف تحته
 * بالضبط لأن الاثنين يقرآن نفس المصدر.
 *
 * زر الإجراء لا يعدّل شيئاً. يفتح حوار طلب صلاحية، لأن المحاسب للقراءة فقط:
 * `UserRole.isReadOnly` يمنع الكتابة المالية، و`AuthManager.requireFinancialWriter`
 * كان سيرفض أي محاولة مباشرة. لذلك الطريق الوحيد المشروع هو طلب موقَّع من المدير.
 */
@Composable
fun AccountantBalancesScreen(
    viewModel: AccountantBalancesViewModel,
    onOpenStatement: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val accents = LocalAgencyAccents.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(12.dp))

            /* ------------------------- ملخّص المحفظة ------------------------- */
            AppCard(containerColor = accents.primary.copy(alpha = 0.10f)) {
                SectionHeader(title = "إجمالي الذمم")
                Text(
                    text = state.totalOutstanding.formatted(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = accents.primary,
                )
                Spacer(Modifier.height(12.dp))
                InfoRow("إجمالي الفواتير المعتمدة", state.totalInvoiced.formatted())
                InfoRow("إجمالي المحصَّل", state.totalPaid.formatted(), valueColor = accents.success)
                InfoRow(
                    label = "متجاوزو سقف الائتمان",
                    value = "${state.overLimitCount}",
                    valueColor = if (state.overLimitCount > 0) MaterialTheme.colorScheme.error else accents.success,
                )
            }

            Spacer(Modifier.height(12.dp))
            AppTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                label = "ابحث باسم المشتري",
                leadingIcon = Icons.Filled.Search,
            )

            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BalanceFilter.entries.toList()) { option ->
                    FilterChip(
                        selected = state.filter == option,
                        onClick = { viewModel.onFilterChange(option) },
                        label = { Text(option.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accents.primary.copy(alpha = 0.18f),
                            selectedLabelColor = accents.primary,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (state.visible.isEmpty()) {
                EmptyState(
                    title = if (state.isLoading) "جارٍ التحميل…" else "لا توجد أرصدة مطابقة",
                    description = if (state.filter == BalanceFilter.ALL) {
                        "ستظهر الأرصدة فور اعتماد أول فاتورة."
                    } else {
                        "جرّب فلتراً آخر أو امسح البحث."
                    },
                    icon = Icons.Filled.AccountBalanceWallet,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                ) {
                    items(state.visible, key = { it.buyerId }) { balance ->
                        BalanceCard(
                            balance = balance,
                            myRequest = state.myRequests.firstOrNull {
                                it.targetId == balance.buyerId &&
                                    it.status == PermissionRequestStatus.PENDING
                            },
                            onOpenStatement = { onOpenStatement(balance.buyerId) },
                            onRequest = { viewModel.openRequest(balance) },
                        )
                    }
                }
            }
        }
    }

    val target = state.requestTarget
    if (target != null) {
        PermissionRequestDialog(
            buyerName = target.buyerName,
            outstanding = target.outstanding,
            reason = state.requestReason,
            busy = state.requestBusy,
            onReasonChange = viewModel::onReasonChange,
            onConfirm = viewModel::submitRequest,
            onDismiss = viewModel::dismissRequest,
        )
    }
}

@Composable
private fun BalanceCard(
    balance: BuyerBalance,
    myRequest: PermissionRequest?,
    onOpenStatement: () -> Unit,
    onRequest: () -> Unit,
) {
    val accents = LocalAgencyAccents.current
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = balance.buyerName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            when {
                balance.isSettled -> BadgePill("مسدَّد", accents.success)
                balance.isOverLimit -> BadgePill("تجاوز السقف", MaterialTheme.colorScheme.error)
                else -> BadgePill("مدين", accents.warning)
            }
        }

        Spacer(Modifier.height(10.dp))
        InfoRow(
            label = "المستحق",
            value = balance.outstanding.formatted(),
            emphasise = true,
            valueColor = if (balance.isOverLimit) MaterialTheme.colorScheme.error else accents.primary,
        )
        InfoRow("فاتورة", balance.totalInvoiced.formatted())
        InfoRow("تحصيل", balance.totalPaid.formatted(), valueColor = accents.success)
        if (balance.creditLimit.isPositive) {
            InfoRow("سقف الائتمان", balance.creditLimit.formatted())
            Spacer(Modifier.height(8.dp))
            CollectionBar(rate = balance.collectionRate.coerceIn(0, 100), color = accents.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "نسبة التحصيل ${balance.collectionRate.coerceIn(0, 100)}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(
                text = "كشف الحساب",
                onClick = onOpenStatement,
                modifier = Modifier.weight(1f),
                containerColor = accents.primary,
            )
            AppButton(
                text = if (myRequest != null) "طلب قيد المراجعة" else "طلب تعديل",
                onClick = onRequest,
                modifier = Modifier.weight(1f),
                enabled = myRequest == null,
                icon = Icons.Filled.Lock,
                containerColor = accents.secondary,
            )
        }

        if (myRequest != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "طلبت تعديلاً على هذا الرصيد ولا يمكن تكرار الطلب قبل بتّ المدير فيه.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CollectionBar(rate: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small),
    ) {
        if (rate > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(rate / 100f)
                    .height(6.dp)
                    .background(color, MaterialTheme.shapes.small),
            ) {}
        }
    }
}

@Composable
private fun PermissionRequestDialog(
    buyerName: String,
    outstanding: Money,
    reason: String,
    busy: Boolean,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        title = { Text("طلب تعديل رصيد") },
        text = {
            Column {
                Text(
                    text = "دورك للقراءة فقط، فالتعديل لا يُنفَّذ من هنا. يُرسل طلب موقَّع إلى المدير، " +
                        "ولا يتغيّر الرصيد قبل توقيعه.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                InfoRow("المشتري", buyerName, emphasise = true)
                InfoRow("الرصيد الحالي", outstanding.formatted())
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = "سبب التعديل (إلزامي)",
                    singleLine = false,
                    enabled = !busy,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy) {
                Text(if (busy) "جارٍ الإرسال…" else "إرسال الطلب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("إلغاء") }
        },
    )
}

/**
 * كشف حساب مشترٍ كما يراه المحاسب.
 *
 * الرصيد المعروض في الأعلى هو `balanceAfter` لآخر حركة كما حفظتها طبقة البيانات،
 * لا مجموعاً يعيد العرض حسابه — فلا يمكن أن يختلف كشف المحاسب عن كشف المشتري.
 */
@Composable
fun AccountantStatementScreen(
    viewModel: AccountantStatementViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val accents = LocalAgencyAccents.current

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text("رجوع") }
                Text(
                    text = state.balance?.buyerName ?: "كشف حساب",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            val balance = state.balance
            if (balance != null) {
                AppCard(containerColor = accents.primary.copy(alpha = 0.10f)) {
                    InfoRow(
                        label = "المستحق حالياً",
                        value = balance.outstanding.formatted(),
                        emphasise = true,
                        valueColor = if (balance.isOverLimit) MaterialTheme.colorScheme.error else accents.primary,
                    )
                    InfoRow("إجمالي الفواتير", balance.totalInvoiced.formatted())
                    InfoRow("إجمالي المسدَّد", balance.totalPaid.formatted(), valueColor = accents.success)
                }
                Spacer(Modifier.height(12.dp))
            }

            SectionHeader(title = "الحركات")

            if (state.entries.isEmpty() && !state.isLoading) {
                EmptyState(
                    title = "لا توجد حركات",
                    description = "لم تُعتمد أي فاتورة ولم يُسجَّل أي سند لهذا المشتري بعد.",
                    icon = Icons.Filled.Info,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                ) {
                    items(state.entries, key = { it.id }) { entry ->
                        StatementRow(entry = entry, accent = accents.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatementRow(entry: BuyerStatementEntry, accent: Color) {
    val isDebit = entry.debit.isPositive
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = entry.at.formattedDateTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isDebit) "+${entry.debit.formatted()}" else "−${entry.credit.formatted()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDebit) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "الرصيد ${entry.balanceAfter.formatted()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}