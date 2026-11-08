package com.agency.app.feature_warehouse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.SectionHeader
import com.agency.app.core.ui.components.StatusBadge
import com.agency.app.core.ui.components.formatted
import com.agency.app.core.ui.theme.LocalAgencyAccents

/**
 * شاشة المسح.
 *
 * الحقل اليدوي بارز لا مخفي: في مخزن حقيقي تُغلق الكاميرا أحياناً بعدسة
 * متّسخة أو ملصق ممزّق، وحجب البديل اليدوي يجعل الشاشة عديمة الفائدة في
 * تلك اللحظة. المسار واحد والمنطق واحد، والفرق في المدخل فقط.
 *
 * ولا زر «خصم مخزون» هنا إطلاقاً — عمداً. الخصم يقع في لحظة إنشاء الطلبية،
 * وإضافة زر خصم ثانٍ كانت ستخلق مصدر حقيقة ثانياً للأرصدة.
 */
@Composable
fun WarehouseScanScreen(
    viewModel: WarehouseScanViewModel,
    onOpenOrder: (String) -> Unit,
    onOpenProducts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val accents = LocalAgencyAccents.current

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "مسح الباركود",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "امسح ملصق الصنف أو رقم الطلبية. المسح يعرض المعلومة فقط " +
                    "ولا يخصم من المخزون: الخصم يقع عند إنشاء الطلبية وحدها.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AppCard {
                AppTextField(
                    value = state.manualCode,
                    onValueChange = viewModel::onManualCodeChange,
                    label = "رمز الصنف أو رقم الطلبية",
                    supportingText = "موظف المخزن يكتبه عند تعذّر قراءة الملصق",
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppButton(
                        text = "بحث",
                        onClick = { viewModel.submitCode(state.manualCode) },
                        enabled = state.manualCode.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (state.result !is ScanResult.Idle) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = viewModel::clear) { Text("مسح النتيجة") }
                }
            }

            when (val result = state.result) {
                ScanResult.Idle -> Unit

                is ScanResult.ProductFound -> {
                    SectionHeader(title = "الصنف")
                    ProductResultCard(product = result.product, onOpenProducts = onOpenProducts)
                }

                is ScanResult.OrderFound -> {
                    SectionHeader(title = "الطلبية")
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = result.order.orderNumber,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            StatusBadge(status = result.order.status)
                        }
                        Spacer(Modifier.height(8.dp))
                        InfoRow(label = "المشتري", value = result.order.buyerName)
                        InfoRow(label = "عدد القطع", value = result.order.itemsCount.toString())
                        InfoRow(label = "الإجمالي", value = result.order.total.formatted())
                        Spacer(Modifier.height(12.dp))
                        AppButton(
                            text = "فتح الطلبية",
                            onClick = { onOpenOrder(result.order.id) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                is ScanResult.NotFound -> {
                    AppCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = "لا يوجد صنف ولا طلبية بالرمز «${result.raw}»",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "تحقّق من الرمز، أو ابحث عن الصنف في إدارة المنتجات.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            if (state.recentScans.isNotEmpty()) {
                SectionHeader(title = "آخر ما مُسح")
                AppCard {
                    state.recentScans.forEachIndexed { index, code ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "${index + 1}. $code",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            TextButton(onClick = { viewModel.submitCode(code) }) {
                                Text("إعادة", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            SectionHeader(title = "طلبيات تنتظر التحضير")
            if (state.ordersAwaitingPick.isEmpty()) {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = accents.success,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = "لا طلبيات معلّقة — رفّ التحضير فارغ.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                state.ordersAwaitingPick.take(6).forEach { order ->
                    AppCard(onClick = { onOpenOrder(order.id) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = order.orderNumber,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = order.buyerName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BadgePill(
                                    text = "${order.itemsCount} قطعة",
                                    color = accents.info,
                                )
                                Spacer(Modifier.size(6.dp))
                                StatusBadge(status = order.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductResultCard(product: Product, onOpenProducts: () -> Unit) {
    val accents = LocalAgencyAccents.current
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "SKU: ${product.sku}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BadgePill(
                text = when {
                    product.isOutOfStock -> "نفد"
                    product.isLowStock -> "منخفض"
                    else -> "متوفر"
                },
                color = when {
                    product.isOutOfStock -> MaterialTheme.colorScheme.error
                    product.isLowStock -> accents.warning
                    else -> accents.success
                },
            )
        }
        Spacer(Modifier.height(8.dp))
        InfoRow(label = "المخزون", value = "${product.stockQuantity} ${product.unit}")
        InfoRow(label = "السعر", value = product.price.formatted())
        InfoRow(label = "أقل كمية للطلب", value = product.minOrderQuantity.toString())
        Spacer(Modifier.height(12.dp))
        AppButton(
            text = "فتح إدارة المنتجات",
            onClick = onOpenProducts,
            containerColor = accents.secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}