package com.agency.app.feature_admin

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agency.app.core.domain.model.Buyer
import com.agency.app.core.domain.model.PermissionRequest
import com.agency.app.core.domain.model.PermissionRequestStatus
import com.agency.app.core.domain.model.User
import com.agency.app.core.domain.model.UserRole
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
 * إدارة المستخدمين.
 *
 * المدير وحده ينشئ الحسابات؛ لا يوجد تسجيل ذاتي في أي شاشة. وحساب المشتري
 * يجب أن يُربط بمشترٍ موجود، لأن الربط هو ما يجعل «ذمّته» قابلة للحساب أصلاً.
 */
@Composable
fun AdminUsersScreen(
    viewModel: AdminUsersViewModel,
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

            AppCard(containerColor = accents.primary.copy(alpha = 0.10f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "المستخدمون",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${state.activeCount} نشط من ${state.totalCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AppButton(
                        text = "حساب جديد",
                        onClick = viewModel::openCreate,
                        icon = Icons.Filled.Add,
                        containerColor = accents.primary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            AppTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                label = "ابحث بالاسم أو اسم المستخدم",
                leadingIcon = Icons.Filled.Search,
            )

            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.roleFilter == null,
                        onClick = { viewModel.onRoleFilterChange(null) },
                        label = { Text("الكل") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accents.primary.copy(alpha = 0.18f),
                            selectedLabelColor = accents.primary,
                        ),
                    )
                }
                items(UserRole.entries.toList()) { role ->
                    FilterChip(
                        selected = state.roleFilter == role,
                        onClick = { viewModel.onRoleFilterChange(role) },
                        label = { Text(role.label()) },
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
                    title = if (state.isLoading) "جارٍ التحميل…" else "لا يوجد مستخدمون مطابقون",
                    description = "أنشئ أول حساب لعضو من الفريق.",
                    icon = Icons.Filled.People,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                ) {
                    items(state.visible, key = { it.id }) { user ->
                        UserCard(
                            user = user,
                            buyerName = state.linkableBuyers.firstOrNull { it.id == user.buyerId }?.name,
                            onToggle = { viewModel.toggleActive(user) },
                        )
                    }
                }
            }
        }
    }

    if (state.showCreate) {
        CreateUserDialog(state = state, viewModel = viewModel)
    }
}

@Composable
private fun UserCard(user: User, buyerName: String?, onToggle: () -> Unit) {
    val accents = LocalAgencyAccents.current
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                BadgePill(user.role.label(), accents.secondary)
                Spacer(Modifier.height(6.dp))
                BadgePill(
                    text = if (user.isActive) "نشط" else "موقوف",
                    color = if (user.isActive) accents.success else MaterialTheme.colorScheme.error,
                )
            }
        }

        if (user.phone != null) {
            Spacer(Modifier.height(8.dp))
            InfoRow("الجوال", user.phone)
        }
        if (user.role == UserRole.BUYER) {
            val linked = buyerName ?: "غير مرتبط"
            InfoRow(
                label = "المشتري المرتبط",
                value = linked,
                valueColor = if (buyerName == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (user.lastLoginAt != null) {
            InfoRow("آخر دخول", user.lastLoginAt.formattedDateTime())
        }

        Spacer(Modifier.height(10.dp))
        AppButton(
            text = if (user.isActive) "إيقاف الحساب" else "تفعيل الحساب",
            onClick = onToggle,
            containerColor = if (user.isActive) accents.secondary else accents.success,
        )
    }
}

@Composable
private fun CreateUserDialog(state: AdminUsersUiState, viewModel: AdminUsersViewModel) {
    AlertDialog(
        onDismissRequest = { if (!state.busy) viewModel.dismissCreate() },
        title = { Text("حساب مستخدم جديد") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                AppTextField(
                    value = state.draftDisplayName,
                    onValueChange = viewModel::onDisplayNameChange,
                    label = "الاسم المعروض",
                    enabled = !state.busy,
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = state.draftUsername,
                    onValueChange = viewModel::onUsernameChange,
                    label = "اسم المستخدم",
                    enabled = !state.busy,
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = state.draftPassword,
                    onValueChange = viewModel::onPasswordChange,
                    label = "كلمة المرور (6 أحرف على الأقل)",
                    isPassword = true,
                    enabled = !state.busy,
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = state.draftPhone,
                    onValueChange = viewModel::onPhoneChange,
                    label = "الجوال (اختياري)",
                    enabled = !state.busy,
                )

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "الدور",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(UserRole.entries.toList()) { role ->
                        FilterChip(
                            selected = state.draftRole == role,
                            onClick = { viewModel.onRoleChange(role) },
                            enabled = !state.busy,
                            label = { Text(role.label()) },
                        )
                    }
                }

                /* ربط المشتري يظهر لدور المشتري فقط — حقل لا معنى له في غيره. */
                if (state.draftRole == UserRole.BUYER) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "المشتري المرتبط (إلزامي)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (state.linkableBuyers.isEmpty()) {
                        Text(
                            text = "لا يوجد مشترون بعد. أضف مشترياً من شاشة «المشترون» أولاً.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        Column {
                            state.linkableBuyers.forEach { buyer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    FilterChip(
                                        selected = state.draftBuyerId == buyer.id,
                                        onClick = { viewModel.onBuyerLinkChange(buyer.id) },
                                        enabled = !state.busy,
                                        label = { Text(buyer.name) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::submitCreate, enabled = !state.busy) {
                Text(if (state.busy) "جارٍ الإنشاء…" else "إنشاء")
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissCreate, enabled = !state.busy) { Text("إلغاء") }
        },
    )
}

/**
 * إدارة المشترين.
 *
 * تعطيل المشتري لا يحذفه: فواتيره وسنداته وذمّته التاريخية تبقى، لكنه يختفي
 * من قوائم الفوترة الجديدة. الحذف الحقيقي لمشترٍ له مستندات كان سيجعل
 * الأرصدة تشير إلى طرف غير موجود.
 */
@Composable
fun AdminBuyersScreen(
    viewModel: AdminBuyersViewModel,
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

            AppCard(containerColor = accents.primary.copy(alpha = 0.10f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "المشترون",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${state.activeCount} نشط من ${state.totalCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AppButton(
                        text = "مشترٍ جديد",
                        onClick = viewModel::openCreate,
                        icon = Icons.Filled.Add,
                        containerColor = accents.primary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            AppTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                label = "ابحث بالاسم أو الجوال أو المحل",
                leadingIcon = Icons.Filled.Search,
            )
            Spacer(Modifier.height(12.dp))

            if (state.visible.isEmpty()) {
                EmptyState(
                    title = if (state.isLoading) "جارٍ التحميل…" else "لا يوجد مشترون",
                    description = "أضف أول مشترٍ لتتمكن من إصدار فواتير له.",
                    icon = Icons.Filled.Storefront,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                ) {
                    items(state.visible, key = { it.id }) { buyer ->
                        BuyerCard(
                            buyer = buyer,
                            onEdit = { viewModel.openEdit(buyer) },
                            onToggle = { viewModel.toggleActive(buyer) },
                        )
                    }
                }
            }
        }
    }

    if (state.showEditor) {
        BuyerEditorDialog(state = state, viewModel = viewModel)
    }
}

@Composable
private fun BuyerCard(buyer: Buyer, onEdit: () -> Unit, onToggle: () -> Unit) {
    val accents = LocalAgencyAccents.current
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buyer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (buyer.shopName != null) {
                    Text(
                        text = buyer.shopName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            BadgePill(
                text = if (buyer.isActive) "نشط" else "موقوف",
                color = if (buyer.isActive) accents.success else MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(8.dp))
        InfoRow("الجوال", buyer.phone)
        if (!buyer.address.isNullOrBlank()) InfoRow("العنوان", buyer.address)
        InfoRow(
            label = "سقف الائتمان",
            value = if (buyer.creditLimit.isZero) "غير محدّد" else buyer.creditLimit.formatted(),
        )

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(
                text = "تعديل",
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                containerColor = accents.primary,
            )
            AppButton(
                text = if (buyer.isActive) "إيقاف" else "تفعيل",
                onClick = onToggle,
                modifier = Modifier.weight(1f),
                containerColor = if (buyer.isActive) accents.secondary else accents.success,
            )
        }
    }
}

@Composable
private fun BuyerEditorDialog(state: AdminBuyersUiState, viewModel: AdminBuyersViewModel) {
    AlertDialog(
        onDismissRequest = { if (!state.busy) viewModel.dismissEditor() },
        title = { Text(if (state.editingId == null) "مشترٍ جديد" else "تعديل مشترٍ") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                AppTextField(
                    value = state.draftName,
                    onValueChange = viewModel::onNameChange,
                    label = "اسم المشتري",
                    enabled = !state.busy,
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = state.draftPhone,
                    onValueChange = viewModel::onPhoneChange,
                    label = "رقم الجوال",
                    enabled = !state.busy,
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = state.draftShopName,
                    onValueChange = viewModel::onShopNameChange,
                    label = "اسم المحل (اختياري)",
                    enabled = !state.busy,
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = state.draftAddress,
                    onValueChange = viewModel::onAddressChange,
                    label = "العنوان (اختياري)",
                    enabled = !state.busy,
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = state.draftCreditLimit,
                    onValueChange = viewModel::onCreditLimitChange,
                    label = "سقف الائتمان (اتركه فارغاً بلا سقف)",
                    enabled = !state.busy,
                    supportingText = "يُحسب بالسنت داخلياً، فلا يوجد تقريب عائم.",
                )
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::save, enabled = !state.busy) {
                Text(if (state.busy) "جارٍ الحفظ…" else "حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissEditor, enabled = !state.busy) { Text("إلغاء") }
        },
    )
}

/**
 * مركز الموافقات.
 *
 * هذا هو الطرف الآخر من قاعدة «لا كتابة بلا تصريح»: كل طلب صلاحية يقدّمه
 * المخزن أو المحاسب يصل هنا، ولا يصبح ساري المفعول إلا بتوقيع المدير.
 * الرفض يتطلب سبباً مكتوباً لأن مقدّم الطلب يحتاج أن يعرف كيف يصحّح طلبه.
 */
@Composable
fun AdminPermissionsScreen(
    viewModel: AdminPermissionsViewModel,
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

            AppCard(
                containerColor = if (state.pendingCount > 0) {
                    accents.warning.copy(alpha = 0.12f)
                } else {
                    accents.success.copy(alpha = 0.10f)
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = if (state.pendingCount > 0) accents.warning else accents.success,
                    )
                    Spacer(Modifier.height(0.dp))
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = if (state.pendingCount > 0) {
                                "${state.pendingCount} طلب بانتظار توقيعك"
                            } else {
                                "لا توجد طلبات معلّقة"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "لا يُنفَّذ أي إجراء مالي أو سعري قبل توقيعك هنا.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.filter == null,
                        onClick = { viewModel.onFilterChange(null) },
                        label = { Text("الكل") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accents.primary.copy(alpha = 0.18f),
                            selectedLabelColor = accents.primary,
                        ),
                    )
                }
                items(PermissionRequestStatus.entries.toList()) { status ->
                    FilterChip(
                        selected = state.filter == status,
                        onClick = { viewModel.onFilterChange(status) },
                        label = { Text(status.label()) },
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
                    title = if (state.isLoading) "جارٍ التحميل…" else "لا توجد طلبات",
                    description = "طلبات الصلاحية التي يقدّمها الفريق ستظهر هنا.",
                    icon = Icons.Filled.Lock,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                ) {
                    items(state.visible, key = { it.id }) { request ->
                        PermissionCard(
                            request = request,
                            onApprove = { viewModel.openReview(request, approve = true) },
                            onReject = { viewModel.openReview(request, approve = false) },
                        )
                    }
                }
            }
        }
    }

    state.reviewing?.let { request ->
        ReviewDialog(
            request = request,
            note = state.reviewNote,
            approve = state.approve,
            busy = state.busy,
            onNoteChange = viewModel::onReviewNoteChange,
            onConfirm = viewModel::submitReview,
            onDismiss = viewModel::dismissReview,
        )
    }
}

@Composable
private fun PermissionCard(
    request: PermissionRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val accents = LocalAgencyAccents.current
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = request.action.actionLabel(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            val color = when (request.status) {
                PermissionRequestStatus.PENDING -> accents.warning
                PermissionRequestStatus.APPROVED -> accents.success
                PermissionRequestStatus.REJECTED -> MaterialTheme.colorScheme.error
            }
            BadgePill(request.status.label(), color)
        }

        Spacer(Modifier.height(8.dp))
        InfoRow("مقدّم الطلب", "${request.requesterName} (${request.requesterRole.label()})")
        InfoRow("الكيان", request.targetEntity)
        if (request.targetId != null) InfoRow("المعرّف", request.targetId)
        InfoRow("التاريخ", request.createdAt.formattedDateTime())
        Spacer(Modifier.height(6.dp))
        Text(
            text = "السبب: ${request.reason}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (request.isApproved) {
            Spacer(Modifier.height(8.dp))
            InfoRow(
                label = "التوقيع",
                value = if (request.isSigned) "موقَّع" else "بلا توقيع",
                valueColor = if (request.isSigned) accents.success else MaterialTheme.colorScheme.error,
            )
            if (request.reviewedByName != null) {
                InfoRow("وقّعه", request.reviewedByName)
            }
            if (!request.reviewNote.isNullOrBlank()) {
                InfoRow("ملاحظة", request.reviewNote)
            }
        }

        if (request.isPending) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton(
                    text = "توقيع واعتماد",
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    containerColor = accents.success,
                )
                AppButton(
                    text = "رفض",
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    containerColor = accents.secondary,
                )
            }
        }
    }
}

@Composable
private fun ReviewDialog(
    request: PermissionRequest,
    note: String,
    approve: Boolean,
    busy: Boolean,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val accents = LocalAgencyAccents.current
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        title = { Text(if (approve) "توقيع واعتماد" else "رفض الطلب") },
        text = {
            Column {
                Text(
                    text = if (approve) {
                        "بتوقيعك يصبح الإجراء «${request.action.actionLabel()}» مسموحاً لـ" +
                            " «${request.requesterName}». التوقيع مرتبط بهذا الطلب وحده ولا يُعاد استعماله."
                    } else {
                        "الرفض يوقف الطلب نهائياً. اكتب سبباً واضحاً ليعرف مقدّمه ما ينقص."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                InfoRow("الطلب", request.action.actionLabel())
                InfoRow("المقدّم", request.requesterName)
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    label = if (approve) "ملاحظة (اختيارية)" else "سبب الرفض (إلزامي)",
                    singleLine = false,
                    enabled = !busy,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy) {
                Text(
                    text = when {
                        busy -> "جارٍ التنفيذ…"
                        approve -> "توقيع"
                        else -> "رفض"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("إلغاء") }
        },
    )
}