// feature_warehouse/orders/DispatchScreen.kt
package com.agency.app.feature_warehouse.orders

import android.Manifest
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agency.app.core.ui.components.AppButton
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.AppTextField
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchScreen(
    orderId: String,
    onDispatchComplete: () -> Unit,
    viewModel: DispatchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHaptic.current
    val context = LocalContext.current

    // Launcher للوصول لجهات الاتصال
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let {
            viewModel.loadContactFromUri(it, context.contentResolver)
        }
    }

    // Launcher لطلب إذن قراءة جهات الاتصال
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إرسال الطلبية", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { /* العودة */ }) {
                        Icon(Icons.Filled.ArrowBack, "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.canDispatch) {
                ExtendedFloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.confirmDispatch()
                        onDispatchComplete()
                    },
                    icon = { Icon(Icons.Filled.Send, "إرسال") },
                    text = { Text("تأكيد الإرسال") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // بطاقة معلومات الطلبية
            AppCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "معلومات الطلبية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المشتري:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            uiState.buyerName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("عدد الأصناف:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${uiState.itemsCount} صنف",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المبلغ الإجمالي:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${uiState.totalAmount} ${uiState.currency}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // حقل رقم الوصيل
            AppTextField(
                value = uiState.contactNumber,
                onValueChange = { viewModel.updateContactNumber(it) },
                label = "رقم التواصل",
                placeholder = "05xxxxxxxx",
                leadingIcon = { Icon(Icons.Outlined.Phone, "الهاتف") },
                trailingIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }) {
                        Icon(Icons.Filled.Contacts, "جهات الاتصال")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            // اختيار وسيلة الإرسال
            Text(
                text = "وسيلة الإرسال",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeliveryMethodChip(
                    text = "توصيل مباشر",
                    icon = Icons.Filled.LocalShipping,
                    selected = uiState.deliveryMethod == "DIRECT",
                    onClick = { viewModel.selectDeliveryMethod("DIRECT") }
                )
                DeliveryMethodChip(
                    text = "شركة شحن",
                    icon = Icons.Filled.Truck,
                    selected = uiState.deliveryMethod == "COMPANY",
                    onClick = { viewModel.selectDeliveryMethod("COMPANY") }
                )
                DeliveryMethodChip(
                    text = "استلام",
                    icon = Icons.Filled.Store,
                    selected = uiState.deliveryMethod == "PICKUP",
                    onClick = { viewModel.selectDeliveryMethod("PICKUP") }
                )
            }

            // معاينة الرسالة
            if (uiState.contactNumber.isNotEmpty()) {
                AppCard(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "معاينة الإرسال",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "سيتم إرسال الطلبية إلى:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Phone,
                                "الهاتف",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = uiState.contactNumber,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(
                            text = "الوسيلة: ${getDeliveryMethodText(uiState.deliveryMethod)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DeliveryMethodChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        leadingIcon = {
            Icon(
                icon,
                text,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

private fun getDeliveryMethodText(method: String): String {
    return when (method) {
        "DIRECT" -> "توصيل مباشر"
        "COMPANY" -> "شركة شحن"
        "PICKUP" -> "استلام من المحل"
        else -> ""
    }
}
