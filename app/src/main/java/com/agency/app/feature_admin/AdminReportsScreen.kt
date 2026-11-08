package com.agency.app.feature_admin

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.agency.app.core.domain.model.Money
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.CurrencyText
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.SectionHeader
import com.agency.app.core.ui.theme.LocalAgencyAccents

/**
 * تقارير الإدارة.
 *
 * الشاشة تعرض المعادلة المحاسبية صريحةً: `المفوتر = المحصّل + القائم`.
 * وهذا ليس تنسيقاً جمالياً — هو ما يجعل الخطأ المحاسبي مرئياً. حين تتوازن
 * الأرقام يقول ذلك بعلامة تحقّق، وحين لا تتوازن يقول إن الفترة جزئية
 * والمقارنة غير معلَّقة، بدل أن يُخفي التعارض.
 */
@Composable
fun AdminReportsScreen(
    viewModel: AdminReportsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val metrics = state.metrics
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReportRange.entries.forEach { range ->
                    FilterChip(
                        selected = range == state.range,
                        onClick = { viewModel.setRange(range) },
                        label = { Text(range.label) },
                    )
                }
            }

            val gap = Money(metrics.invoicedTotal.cents - metrics.collectedTotal.cents)

            AppCard {
                SectionHeader(title = "المعادلة المحاسبية")
                InfoRow(label = "إجمالي المفوتر المعتمد", value = metrics.invoicedTotal.formattedOrDash())
                InfoRow(label = "إجمالي المحصّل", value = metrics.collectedTotal.formattedOrDash())
                InfoRow(
                    label = "الفارق (المفوتر − المحصّل)",
                    value = gap.formattedOrDash(),
                    valueColor = if (gap.isZero) accents.success else accents.warning,
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                InfoRow(
                    label = "الذمم القائمة (كل السجل)",
                    value = metrics.outstandingTotal.formattedOrDash(),
                    emphasise = true,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (metrics.isBalanced) Icons.Filled.CheckCircle
                        else Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = if (metrics.isBalanced) accents.success else accents.warning,
                    )
                    Text(
                        text = if (metrics.isBalanced) {
                            "  متوازنة: المحصّل + القائم = المفوتر"
                        } else {
                            "  غير معلَّقة: الذمم راهنة لا مقيَّدة بالفترة المختارة"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (metrics.isBalanced) accents.success else accents.warning,
                    )
                }
            }

            AppCard {
                SectionHeader(title = "التحصيل")
                InfoRow(label = "معدّل التحصيل", value = "${metrics.collectionRate}%", emphasise = true)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { metrics.collectionRate / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (metrics.collectionRate >= 80) accents.success else accents.warning,
                )
                Spacer(Modifier.height(8.dp))
                InfoRow(label = "إجمالي العمولات", value = metrics.commissionTotal.formattedOrDash())
                InfoRow(
                    label = "مشترون تجاوزوا الحد",
                    value = "${metrics.buyersOverLimit} من ${metrics.buyersTotal}",
                    valueColor = if (metrics.buyersOverLimit > 0) accents.warning
                    else MaterialTheme.colorScheme.onSurface,
                )
            }

            AppCard {
                SectionHeader(title = "الطلبيات والفواتير")
                InfoRow(label = "إجمالي الطلبيات", value = metrics.ordersTotal.toString())
                InfoRow(label = "طلبيات مسلَّمة", value = metrics.ordersDelivered.toString())
                InfoRow(
                    label = "طلبيات ملغاة",
                    value = metrics.ordersCancelled.toString(),
                    valueColor = if (metrics.ordersCancelled > 0) accents.warning
                    else MaterialTheme.colorScheme.onSurface,
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                InfoRow(label = "فواتير معتمدة", value = metrics.invoicesApproved.toString())
                InfoRow(
                    label = "فواتير مرفوضة",
                    value = metrics.invoicesRejected.toString(),
                    valueColor = if (metrics.invoicesRejected > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
                )
            }

            AppCard {
                SectionHeader(title = "المخزون")
                InfoRow(
                    label = "قيمة المخزون بسعر البيع",
                    value = metrics.stockValueTotal.formattedOrDash(),
                    emphasise = true,
                )
                InfoRow(
                    label = "أصناف منخفضة أو نافدة",
                    value = metrics.lowStockCount.toString(),
                    valueColor = if (metrics.lowStockCount > 0) accents.warning
                    else accents.success,
                )
            }

            if (state.topBuyers.isNotEmpty()) {
                SectionHeader(title = "أعلى المشترين ذمّةً")
                state.topBuyers.forEach { balance ->
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = balance.buyerName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "سداد ${balance.collectionRate}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            CurrencyText(
                                amount = balance.outstanding,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (balance.isOverLimit) accents.warning
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            if (state.topProducts.isNotEmpty()) {
                SectionHeader(title = "الأكثر مبيعاً")
                state.topProducts.forEach { line ->
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = line.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${line.quantitySold} وحدة مباعة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            CurrencyText(
                                amount = line.revenue,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Assessment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = "  كل رقم هنا مجموع من مستندات في القاعدة، ولا يُحفظ رصيد يدوياً.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** صفر مالي يُعرض كشرطة في التقرير — «0.00» تُوهم بوجود حركة. */
private fun Money.formattedOrDash(): String = if (isZero) "—" else toString()