package com.agency.app.feature_warehouse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agency.app.core.domain.model.Product
import com.agency.app.core.ui.components.AppButton
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.AppTextField
import com.agency.app.core.ui.components.BadgePill
import com.agency.app.core.ui.components.EmptyState
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.formatted
import com.agency.app.core.ui.theme.LocalAgencyAccents

/**
 * إدارة منتجات المخزن.
 *
 * عند فتح نموذج التعديل يظهر السعر مقفلاً مع زر «طلب تصريح تعديل السعر»،
 * تطبيقاً للقاعدة: المخزن لا يعدّل السعر بلا موافقة المدير. الحذف مسمّى
 * «إيقاف» لأن المنتج لا يُمحى من قاعدة البيانات أبداً.
 */
@Composable
fun WarehouseProductsScreen(
    viewModel: WarehouseProductsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val accents = LocalAgencyAccents.current
    var permissionTarget by remember { mutableStateOf<Product?>(null) }
    var permissionReason by remember { mutableStateOf("") }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                label = "ابحث بالاسم أو الرمز",
                leadingIcon = Icons.Filled.Search,
            )
            Spacer(Modifier.height(10.dp))
            AppButton(
                text = "منتج جديد",
                onClick = { viewModel.startEdit(null) },
                icon = Icons.Filled.Add,
            )

            if (state.error != null || state.notice != null) {
                Spacer(Modifier.height(8.dp))
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
                    TextButton(onClick = viewModel::dismissMessages) { Text("إخفاء") }
                }
            }

            Spacer(Modifier.height(10.dp))
            if (state.products.isEmpty()) {
                EmptyState(
                    title = "لا منتجات",
                    description = "أضف أول منتج ليظهر في كتالوج المشترين.",
                    icon = Icons.Filled.Inventory2,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(state.products, key = { it.id }) { product ->
                        AppCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "رمز: ${product.sku}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                BadgePill(
                                    text = if (product.isActive) "نشط" else "موقوف",
                                    color = if (product.isActive) accents.success else MaterialTheme.colorScheme.error,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            InfoRow(label = "السعر", value = product.price.formatted())
                            InfoRow(label = "المخزون", value = product.stockQuantity.toString())
                            InfoRow(label = "الوحدة", value = product.unit)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { viewModel.startEdit(product) }) { Text("تعديل") }
                                TextButton(onClick = { permissionTarget = product }) {
                                    Text("طلب صلاحية السعر")
                                }
                                TextButton(
                                    onClick = {
                                        if (product.isActive) {
                                            viewModel.deactivate(product)
                                        } else {
                                            viewModel.reactivate(product)
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (product.isActive) "إيقاف" else "تنشيط",
                                        color = if (product.isActive) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val editing = state.editing
    if (state.editing != null || editing != null) {
        ProductEditorDialog(
            existing = editing,
            isSaving = state.isSaving,
            onDismiss = viewModel::cancelEdit,
            onSave = { name, sku, unit, price, stock, minOrder, priceChanged ->
                viewModel.save(editing, name, sku, unit, price, stock, minOrder, priceChanged)
            },
        )
    }

    val target = permissionTarget
    if (target != null) {
        AlertDialog(
            onDismissRequest = { permissionTarget = null },
            title = { Text("طلب تصريح تعديل سعر") },
            text = {
                Column {
                    Text(
                        text = "المنتج: ${target.name}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    AppTextField(
                        value = permissionReason,
                        onValueChange = { permissionReason = it },
                        label = "سبب التعديل",
                        singleLine = false,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.requestPricePermission(target, permissionReason)
                        permissionReason = ""
                        permissionTarget = null
                    }
                ) { Text("إرسال للمدير") }
            },
            dismissButton = {
                TextButton(onClick = { permissionTarget = null }) { Text("إلغاء") }
            },
        )
    }
}

@Composable
private fun ProductEditorDialog(
    existing: Product?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        sku: String,
        unit: String,
        price: String,
        stock: String,
        minOrder: String,
        priceChanged: Boolean,
    ) -> Unit,
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var sku by remember(existing?.id) { mutableStateOf(existing?.sku.orEmpty()) }
    var unit by remember(existing?.id) { mutableStateOf(existing?.unit ?: "قطعة") }
    var price by remember(existing?.id) { mutableStateOf(existing?.price?.let { it.cents / 100.0 }.toString().takeIf { existing != null } ?: "") }
    var stock by remember(existing?.id) { mutableStateOf(existing?.stockQuantity?.toString() ?: "0") }
    var minOrder by remember(existing?.id) { mutableStateOf(existing?.minOrderQuantity?.toString() ?: "1") }

    val priceChanged = existing != null && runCatching {
        price.toDoubleOrNull()?.let { (it * 100).toLong() } != existing.price.cents
    }.getOrDefault(false)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "منتج جديد" else "تعديل منتج") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(value = name, onValueChange = { name = it }, label = "اسم المنتج")
                AppTextField(value = sku, onValueChange = { sku = it }, label = "الرمز")
                AppTextField(value = unit, onValueChange = { unit = it }, label = "الوحدة")
                AppTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = "السعر",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                )
                if (priceChanged) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.height(16.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = " تغيير السعر يحتاج تصريح مدير معتمد",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                AppTextField(
                    value = stock,
                    onValueChange = { stock = it },
                    label = "المخزون",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                )
                AppTextField(
                    value = minOrder,
                    onValueChange = { minOrder = it },
                    label = "أقل كمية",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, sku, unit, price, stock, minOrder, priceChanged) },
                enabled = !isSaving,
            ) { Text(if (isSaving) "جارٍ الحفظ…" else "حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } },
    )
}