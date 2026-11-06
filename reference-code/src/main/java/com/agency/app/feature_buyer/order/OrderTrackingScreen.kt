// feature_buyer/order/OrderTrackingScreen.kt
package com.agency.app.feature_buyer.order

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agency.app.core.database.entity.OrderStatus
import com.agency.app.core.ui.components.AppCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    orderId: String,
    onBackClick: () -> Unit,
    viewModel: OrderTrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHaptic.current
    val scrollState = rememberScrollState()

    LaunchedEffect(orderId) {
        viewModel.observeOrder(orderId)
    }

    // اهتزاز عند تغيير الحالة
    LaunchedEffect(uiState.status) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تتبع الطلبية", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { /* اتصال */ }) {
                        Icon(Icons.Filled.Call, "اتصال")
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
            // البطاقة العلوية مع الحالة الحالية
            StatusHeaderCard(
                status = uiState.status,
                orderId = orderId,
                orderDate = uiState.orderDate
            )

            // البطاقة المتحركة — Hero Animation للحالة
            AnimatedContent(
                targetState = uiState.status,
                transitionSpec = {
                    fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                },
                label = "StatusAnimation"
            ) { status ->
                CurrentStatusCard(status = status)
            }

            // خط الزمن
            AppCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        "تفاصيل الطلبية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    TimelineStep(
                        icon = Icons.Filled.FiberNew,
                        title = "تم استلام الطلبية",
                        timestamp = uiState.createdAt,
                        isCompleted = true,
                        isCurrent = uiState.status == OrderStatus.NEW,
                        isLast = false
                    )

                    TimelineStep(
                        icon = Icons.Filled.PendingActions,
                        title = "قيد التحضير",
                        timestamp = uiState.preparingAt,
                        isCompleted = uiState.status.ordinal >= OrderStatus.PREPARING.ordinal,
                        isCurrent = uiState.status == OrderStatus.PREPARING,
                        isLast = false
                    )

                    TimelineStep(
                        icon = Icons.Filled.TaskAlt,
                        title = "تم التحضير",
                        timestamp = uiState.preparedAt,
                        isCompleted = uiState.status.ordinal >= OrderStatus.PREPARED.ordinal,
                        isCurrent = uiState.status == OrderStatus.PREPARED,
                        isLast = false,
                        isHighlight = true
                    )

                    TimelineStep(
                        icon = Icons.Filled.Send,
                        title = "تم الإرسال للوصيل",
                        timestamp = uiState.sentAt,
                        description = uiState.deliveryContact?.let { "رقم الوصيل: $it" },
                        isCompleted = uiState.status.ordinal >= OrderStatus.SENT.ordinal,
                        isCurrent = uiState.status == OrderStatus.SENT,
                        isLast = false
                    )

                    TimelineStep(
                        icon = Icons.Filled.Verified,
                        title = "تم التسليم",
                        timestamp = uiState.deliveredAt,
                        isCompleted = uiState.status == OrderStatus.DELIVERED,
                        isCurrent = uiState.status == OrderStatus.DELIVERED,
                        isLast = true
                    )
                }
            }

            // تفاصيل الطلبية
            AppCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "معلومات الطلبية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    DetailRow("عدد الأصناف", "${uiState.itemsCount} صنف")
                    DetailRow("المبلغ الإجمالي", "${uiState.totalAmount} د.ع")
                    DetailRow("الوسيلة", uiState.deliveryMethod)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusHeaderCard(
    status: OrderStatus,
    orderId: String,
    orderDate: String
) {
    val (color, text, icon) = when (status) {
        OrderStatus.NEW -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            "طلبية جديدة",
            Icons.Filled.FiberNew
        )
        OrderStatus.PREPARING -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            "قيد التحضير",
            Icons.Filled.PendingActions
        )
        OrderStatus.PREPARED -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            "تم التحضير ✓",
            Icons.Filled.TaskAlt
        )
        OrderStatus.SENT -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            "تم الإرسال",
            Icons.Filled.Send
        )
        OrderStatus.DELIVERED -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            "تم التسليم ✓",
            Icons.Filled.Verified
        )
        OrderStatus.CANCELLED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            "ملغية",
            Icons.Filled.Cancel
        )
    }

    AppCard(
        containerColor = color
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "رقم الطلبية: #$orderId",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = orderDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        "الحالة",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentStatusCard(status: OrderStatus) {
    val message = when (status) {
        OrderStatus.NEW -> "تم استلام طلبيتك بنجاح، وسيتم البدء في تحضيرها قريباً"
        OrderStatus.PREPARING -> "جاري تحضير طلبيتك في المخزن الآن"
        OrderStatus.PREPARED -> "تم تحضير طلبيتك بالكامل وهي جاهزة للإرسال"
        OrderStatus.SENT -> "تم إرسال طلبيتك عبر الوصيل، وستصلك قريباً"
        OrderStatus.DELIVERED -> "تم تسليم طلبيتك بنجاح، شكراً لتعاملك معنا"
        OrderStatus.CANCELLED -> "تم إلغاء هذه الطلبية"
    }

    AppCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Info,
                "معلومات",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            )
        }
    }
}

@Composable
private fun TimelineStep(
    icon: ImageVector,
    title: String,
    timestamp: String?,
    description: String? = null,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isLast: Boolean,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        // الجانب الأيسر — الخط والأيقونة
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            // الأيقونة
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = when {
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isCompleted -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        title,
                        tint = when {
                            isCurrent || isCompleted -> Color.White
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // الخط العمودي
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(48.dp)
                        .background(
                            if (isCompleted) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        // الجانب الأيمن — المحتوى
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (!isLast) 16.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = when {
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isCompleted -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            if (!timestamp.isNullOrBlank()) {
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (!description.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = description,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
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
