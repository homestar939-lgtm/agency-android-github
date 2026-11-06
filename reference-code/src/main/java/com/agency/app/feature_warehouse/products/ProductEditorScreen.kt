// feature_warehouse/products/ProductEditorScreen.kt
package com.agency.app.feature_warehouse.products

import androidx.compose.animation.*
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
import com.agency.app.core.ui.components.AppButton
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.AppTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditorScreen(
    productId: String?,
    onSaveComplete: () -> Unit,
    viewModel: ProductEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHaptic.current
    val scrollState = rememberScrollState()
    val isNewProduct = productId == null || productId == "new"

    LaunchedEffect(productId) {
        if (!isNewProduct) {
            viewModel.loadProduct(productId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (isNewProduct) "إضافة منتج" else "تعديل المنتج",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { /* العودة */ }) {
                        Icon(Icons.Filled.ArrowBack, "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.saveProduct()
                        onSaveComplete()
                    }) {
                        Icon(Icons.Filled.Check, "حفظ")
                    }
                }
            )
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
            // قسم المعلومات الأساسية
            SectionHeader("المعلومات الأساسية")
            
            AppTextField(
                value = uiState.name,
                onValueChange = { viewModel.updateName(it) },
                label = "اسم المنتج",
                placeholder = "أدخل اسم المنتج",
                leadingIcon = { Icon(Icons.Outlined.Label, "الاسم") }
            )

            // اختيار التصنيف
            ExposedDropdownMenuBox(
                expanded = uiState.isCategoryDropdownExpanded,
                onExpandedChange = { viewModel.toggleCategoryDropdown() }
            ) {
                AppTextField(
                    value = uiState.selectedCategoryName,
                    onValueChange = {},
                    label = "التصنيف",
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isCategoryDropdownExpanded)
                    },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = uiState.isCategoryDropdownExpanded,
                    onDismissRequest = { viewModel.toggleCategoryDropdown() }
                ) {
                    uiState.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                viewModel.selectCategory(category)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }
                }
            }

            // قسم المعلومات المالية
            SectionHeader("المعلومات المالية")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppTextField(
                    value = uiState.price,
                    onValueChange = { viewModel.updatePrice(it) },
                    label = "السعر",
                    placeholder = "0.00",
                    leadingIcon = { Icon(Icons.Outlined.AttachMoney, "السعر") },
                    modifier = Modifier.weight(1f)
                )
                
                // اختيار وحدة القياس
                ExposedDropdownMenuBox(
                    expanded = uiState.isUnitDropdownExpanded,
                    onExpandedChange = { viewModel.toggleUnitDropdown() },
                    modifier = Modifier.weight(1f)
                ) {
                    AppTextField(
                        value = uiState.selectedUnitName,
                        onValueChange = {},
                        label = "الوحدة",
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isUnitDropdownExpanded)
                        },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = uiState.isUnitDropdownExpanded,
                        onDismissRequest = { viewModel.toggleUnitDropdown() }
                    ) {
                        uiState.units.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.name) },
                                onClick = {
                                    viewModel.selectUnit(unit)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            )
                        }
                    }
                }
            }

            AppTextField(
                value = uiState.stock,
                onValueChange = { viewModel.updateStock(it) },
                label = "الكمية في المخزون",
                placeholder = "0",
                leadingIcon = { Icon(Icons.Outlined.Inventory, "المخزون") }
            )

            AppTextField(
                value = uiState.minOrderQuantity,
                onValueChange = { viewModel.updateMinOrder(it) },
                label = "أقل كمية للطلب",
                placeholder = "1",
                leadingIcon = { Icon(Icons.Outlined.Minimize, "الحد الأدنى") }
            )

            // قسم التفاصيل الإضافية
            SectionHeader("التفاصيل الإضافية")

            AppTextField(
                value = uiState.originCountry,
                onValueChange = { viewModel.updateOriginCountry(it) },
                label = "بلد الصنع",
                placeholder = "مثال: السعودية",
                leadingIcon = { Icon(Icons.Outlined.Public, "بلد الصنع") }
            )

            AppTextField(
                value = uiState.barcode,
                onValueChange = { viewModel.updateBarcode(it) },
                label = "الباركود (اختياري)",
                placeholder = "امسح أو أدخل يدوياً",
                leadingIcon = { Icon(Icons.Outlined.QrCode, "الباركود") },
                trailingIcon = {
                    IconButton(onClick = { /* فتح الكاميرا */ }) {
                        Icon(Icons.Filled.QrCodeScanner, "مسح")
                    }
                }
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("وصف المنتج") },
                placeholder = { Text("أدخل وصفاً تفصيلياً") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            // قسم ما يظهر للمشتري
            SectionHeader("ما يظهر للمشتري")
            Text(
                text = "حدد البيانات التي تريد إظهارها للمشتري في قائمة المنتجات",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AppCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VisibilityToggle(
                        text = "إظهار السعر",
                        icon = Icons.Outlined.AttachMoney,
                        checked = uiState.showPriceToBuyer,
                        onCheckedChange = { viewModel.toggleShowPrice() }
                    )
                    VisibilityToggle(
                        text = "إظهار وحدة القياس",
                        icon = Icons.Outlined.Scale,
                        checked = uiState.showUnitToBuyer,
                        onCheckedChange = { viewModel.toggleShowUnit() }
                    )
                    VisibilityToggle(
                        text = "إظهار تاريخ الانتهاء",
                        icon = Icons.Outlined.Event,
                        checked = uiState.showExpiryToBuyer,
                        onCheckedChange = { viewModel.toggleShowExpiry() }
                    )
                    VisibilityToggle(
                        text = "إظهار أقل كمية للطلب",
                        icon = Icons.Outlined.Minimize,
                        checked = uiState.showMinOrderToBuyer,
                        onCheckedChange = { viewModel.toggleShowMinOrder() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun VisibilityToggle(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, text, tint = MaterialTheme.colorScheme.primary)
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
