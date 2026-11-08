package com.agency.app.feature_warehouse

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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agency.app.core.domain.model.Order
import com.agency.app.core.domain.model.OrderStatus
import com.agency.app.core.ui.components.AppButton
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.BadgePill
import com.agency.app.core.ui.components.EmptyState
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.SectionHeader
import com.agency.app.core.ui.components.StatusBadge
import com.agency.app.core.ui.components.formatted
import com.agency.app.core.util.formattedDateTime
import com.agency.app.core.util.relative

/**
 * تفاصيل طلبية المخزن — شاشة التحضير والتجهيز.
 *
 * أزرار الانتقال مشتقة من [OrderStatus.allowedNext] حرفياً، فلا يظهر زر
 * لعملية سترفضها طبقة البيانات. زر الإلغاء منفصل وبلون تحذيري لأنه يعيد
 * المخزون ويؤثر على ذمّة المشتري.
 */
@Composable
fun WarehouseOrderDetailScreen(
    viewModel: WarehouseOrderDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val order = state.order

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        if (order == null) {
            Column(modifier = Modifier.padding(innerPadding)) {
                EmptyState(
                    title = "الطلبية غير متاحة",
                    description = "قد تكون محذوفة أو غير قابلة للعرض.",
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
                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = order.orderNumber,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = order.buyerName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        StatusBadge(status = order.status)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (order.status != OrderStatus.CANCELLED) {
                        LinearProgressIndicator(
                            progress = { order.currentStepIndex / 4f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                        )
                    }
                }

                AppCard {
                    SectionHeader(title = "قائمة التحضير")
                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "الوحدة: ${item.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            BadgePill(
                                text = "×${item.quantity}",
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    InfoRow(
                        label = "إجمالي القطع",
                        value = order.itemsCount.toString(),
                        emphasise = true,
                    )
                }

                AppCard {
                    SectionHeader(title = "معلومات المشتري")
                    InfoRow(label = "الاسم", value = order.buyerName)
                    InfoRow(label = "أُنشئت", value = order.createdAt.formattedDateTime())
                    InfoRow(label = "آخر تحديث", value = order.updatedAt.relative())
                    InfoRow(label = "قيمة الطلبية", value = order.total.formatted())
                }

                AppCard {
                    SectionHeader(title = "الخط الزمني")
                    order.timeline.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = statusLabel(entry.status),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = entry.at.relative(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (state.error != null) {
                    AppCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                        Text(
                            text = state.error.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }

                if (!order.status.isFinal) {
                    AppCard {
                        SectionHeader(title = "الإجراء التالي")
                        Spacer(Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            order.status.allowedNext
                                .sortedBy { it.ordinal }
                                .forEach { target ->
                                    val isCancel = target == OrderStatus.CANCELLED
                                    AppButton(
                                        text = actionLabel(target),
                                        onClick = { viewModel.transition(target) },
                                        loading = state.isWorking,
                                        icon = if (isCancel) Icons.Filled.Cancel else Icons.Filled.CheckCircle,
                                        containerColor = if (isCancel) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        },
                                    )
                                }
                        }
                    }
                } else {
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Inventory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "الطلبية في حالة نهائية (${statusLabel(order.status)}) ولا تقبل إجراءً جديداً.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                AppButton(text = "رجوع", onClick = onBack)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private fun actionLabel(target: OrderStatus): String = when (target) {
    OrderStatus.PREPARING -> "ابدأ التحضير"
    OrderStatus.PREPARED -> "تأكيد اكتمال التحضير"
    OrderStatus.SENT -> "تم الإرسال"
    OrderStatus.DELIVERED -> "تأكيد التسليم"
    OrderStatus.CANCELLED -> "إلغاء الطلبية وإرجاع المخزون"
    OrderStatus.NEW -> "إعادة إلى جديدة"
}

private fun statusLabel(status: OrderStatus): String = when (status) {
    OrderStatus.NEW -> "جديدة"
    OrderStatus.PREPARING -> "قيد التحضير"
    OrderStatus.PREPARED -> "جاهزة"
    OrderStatus.SENT -> "مرسلة"
    OrderStatus.DELIVERED -> "مسلَّمة"
    OrderStatus.CANCELLED -> "ملغاة"
}