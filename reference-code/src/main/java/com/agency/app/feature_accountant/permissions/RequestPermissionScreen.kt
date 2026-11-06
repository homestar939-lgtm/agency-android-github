// feature_accountant/permissions/RequestPermissionScreen.kt
package com.agency.app.feature_accountant.permissions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.AppTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestPermissionScreen(
    documentId: String,
    documentType: String,
    onRequestSent: () -> Unit,
    viewModel: RequestPermissionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHaptic.current
    val scrollState = rememberScrollState()

    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId, documentType)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("طلب تصريح تعديل", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { /* العودة */ }) {
                        Icon(Icons.Filled.ArrowBack, "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.canRequest) {
                ExtendedFloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.sendRequest {
                            onRequestSent()
                        }
                    },
                    icon = { Icon(Icons.Filled.Send, "إرسال") },
                    text = { Text("إرسال الطلب") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    enabled = !uiState.isSending
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
            // تنبيه
            AppCard(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.Info,
                        "معلومات",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Column {
                        Text(
                            "صلاحيات مقيدة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "بصفتك محاسباً، يمكنك العرض فقط. لتعديل هذا المستند، يجب الحصول على تصريح من المدير.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // معلومات المستند
            AppCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "معلومات المستند",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    InfoRow("نوع المستند", documentType)
                    InfoRow("رقم المستند", uiState.documentNumber)
                    InfoRow("المشتري", uiState.buyerName)
                    InfoRow("المبلغ", "${uiState.amount.toInt()} د.ع")
                    InfoRow("التاريخ", uiState.date)
                    InfoRow("أنشأه", uiState.createdBy)
                }
            }

            // نوع التعديل المطلوب
            AppCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "نوع التعديل المطلوب",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    EditTypeChip(
                        text = "تعديل المبلغ",
                        icon = Icons.Filled.Edit,
                        selected = uiState.editType == "AMOUNT",
                        onClick = { viewModel.selectEditType("AMOUNT") }
                    )
                    EditTypeChip(
                        text = "تعديل الأصناف",
                        icon = Icons.Filled.Inventory,
                        selected = uiState.editType == "ITEMS",
                        onClick = { viewModel.selectEditType("ITEMS") }
                    )
                    EditTypeChip(
                        text = "إلغاء المستند",
                        icon = Icons.Filled.Cancel,
                        selected = uiState.editType == "CANCEL",
                        onClick = { viewModel.selectEditType("CANCEL") }
                    )
                }
            }

            // سبب الطلب
            AppTextField(
                value = uiState.reason,
                onValueChange = { viewModel.updateReason(it) },
                label = "سبب الطلب (إلزامي)",
                placeholder = "اشرح سبب طلب التعديل...",
                leadingIcon = { Icon(Icons.Outlined.Notes, "السبب") },
                isError = uiState.reasonError != null,
                supportingText = uiState.reasonError?.let { { Text(it) } }
            )

            // التوقيع الرقمي
            AppCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "التوقيع الرقمي",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "لتأكيد هويتك، يرجى إدخال كلمة المرور",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppTextField(
                        value = uiState.password,
                        onValueChange = { viewModel.updatePassword(it) },
                        label = "كلمة المرور",
                        placeholder = "••••••••",
                        leadingIcon = { Icon(Icons.Outlined.Lock, "كلمة المرور") },
                        isPassword = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun EditTypeChip(
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
        modifier = Modifier.fillMaxWidth()
    )
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
