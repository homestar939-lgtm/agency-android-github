package com.agency.app.feature_buyer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agency.app.core.domain.model.Order
import com.agency.app.core.domain.model.OrderStatus
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.BadgePill
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.SectionHeader
import com.agency.app.core.ui.components.StatusBadge
import com.agency.app.core.ui.components.formatted
import com.agency.app.core.ui.theme.LocalAgencyAccents
import com.agency.app.core.util.relative

/**
 * تتبّع الطلبية.
 *
 * التحديث هنا لحظي بلا أي كود شبكة في هذه الشاشة: [SyncManager] يكتب الحدث
 * في Room، و`observeOrder` يُصدر القيمة الجديدة، فتتغيّر الواجهة. كل تغيّر
 * حالة يصاحبه Haptic لأن المستخدم ينتظر هذا الانتقال فعلياً.
 */
@Composable
fun BuyerOrderTrackingScreen(
    viewModel: OrderTrackingViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val order by viewModel.order.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    val statusKey = order?.status
    LaunchedEffect(statusKey) {
        if (statusKey != null) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        val current = order
        if (current == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                com.agency.app.core.ui.components.LoadingShimmer(lines = 5, modifier = Modifier.padding(16.dp))
            }
        } else {
            OrderTrackingBody(order = current, onBack = onBack, modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun OrderTrackingBody(
    order: Order,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accents = LocalAgencyAccents.current
    val progress by animateFloatAsState(
        targetValue = order.currentStepIndex / 4f,
        label = "orderProgress",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
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
                        text = "أُنشئت ${order.createdAt.relative()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusBadge(status = order.status)
            }
            Spacer(Modifier.height(12.dp))
            if (order.status != OrderStatus.CANCELLED) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.small),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "الخطوة ${order.currentStepIndex + 1} من 5",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                BadgePill(text = "أُلغيت هذه الطلبية", color = MaterialTheme.colorScheme.error)
            }
        }

        AppCard {
            SectionHeader(title = "الأصناف")
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
                            text = "${item.quantity} × ${item.unitPrice.formatted()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = item.net.formatted(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            InfoRow(
                label = "الإجمالي",
                value = order.total.formatted(),
                emphasise = true,
                valueColor = MaterialTheme.colorScheme.primary,
            )
        }

        AppCard {
            SectionHeader(title = "سير الطلبية")
            order.timeline.forEachIndexed { index, entry ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = accents.success,
                        )
                        if (index != order.timeline.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .size(width = 2.dp, height = 28.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant),
                            )
                        }
                    }
                    Spacer(Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = statusLabel(entry.status),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${entry.actorName} — ${entry.at.relative()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!entry.note.isNullOrBlank()) {
                            Text(
                                text = entry.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        if (!order.note.isNullOrBlank()) {
            AppCard {
                SectionHeader(title = "ملاحظة")
                Text(order.note, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (order.status == OrderStatus.SENT) {
            AppCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "طلبك في الطريق إليك",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        com.agency.app.core.ui.components.AppButton(
            text = "رجوع",
            onClick = onBack,
        )
        Spacer(Modifier.height(16.dp))
    }
}

private fun statusLabel(status: OrderStatus): String = when (status) {
    OrderStatus.NEW -> "تم استلام الطلبية"
    OrderStatus.PREPARING -> "قيد التحضير في المخزن"
    OrderStatus.PREPARED -> "الطلبية جاهزة"
    OrderStatus.SENT -> "تم الإرسال"
    OrderStatus.DELIVERED -> "تم التسليم"
    OrderStatus.CANCELLED -> "أُلغيت"
}