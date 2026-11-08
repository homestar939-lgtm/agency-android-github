package com.agency.app.feature_buyer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agency.app.core.ui.components.AppButton
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.BadgePill
import com.agency.app.core.ui.components.EmptyState
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.SectionHeader
import com.agency.app.core.ui.components.formatted

/**
 * السلة.
 *
 * الزر معطّل ما دام أي سطر مخالفاً ([CartStore.canCheckout])، والسبب مكتوب
 * بجانب السطر نفسه، فلا يُترك المشتري يضغط زرّاً يفشل بلا تفسير.
 */
@Composable
fun BuyerCartScreen(
    viewModel: CartViewModel,
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        if (state.lines.isEmpty()) {
            Column(modifier = Modifier.padding(innerPadding)) {
                EmptyState(
                    title = "السلة فارغة",
                    description = "أضف منتجات من الكتالوج لتبدأ طلبية جديدة.",
                    actionLabel = "العودة للكتالوج",
                    onAction = onBack,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.lines, key = { it.productId }) { line ->
                        AppCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = line.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (line.showUnit) {
                                        Text(
                                            text = "الوحدة: ${line.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.remove(line.productId) }) {
                                    Icon(
                                        Icons.Filled.DeleteOutline,
                                        contentDescription = "حذف",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.setQuantity(line.productId, line.quantity - 1) }
                                    ) {
                                        Icon(Icons.Filled.Remove, contentDescription = "تقليل", modifier = Modifier.size(20.dp))
                                    }
                                    Text(
                                        text = line.quantity.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    IconButton(
                                        onClick = { viewModel.setQuantity(line.productId, line.quantity + 1) },
                                        enabled = line.quantity < line.availableStock,
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "زيادة", modifier = Modifier.size(20.dp))
                                    }
                                }
                                if (line.showPrice) {
                                    Text(
                                        text = line.gross.formatted(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            when {
                                line.exceedsStock -> BadgePill(
                                    text = "المتاح ${line.availableStock} فقط",
                                    color = MaterialTheme.colorScheme.error,
                                )
                                !line.meetsMinimum -> BadgePill(
                                    text = "الحد الأدنى ${line.minOrderQuantity}",
                                    color = MaterialTheme.colorScheme.error,
                                )
                                else -> Unit
                            }
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                AppCard {
                    SectionHeader(title = "ملخص الطلبية")
                    InfoRow(label = "عدد القطع", value = state.itemCount.toString())
                    InfoRow(
                        label = "الإجمالي قبل الفوترة",
                        value = state.total.formatted(),
                        emphasise = true,
                        valueColor = MaterialTheme.colorScheme.primary,
                    )
                    val balance = state.balance
                    if (balance != null) {
                        InfoRow(
                            label = "ذمتك الحالية",
                            value = balance.outstanding.formatted(),
                            valueColor = if (balance.isOverLimit) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        if (balance.isOverLimit) {
                            Text(
                                text = "تجاوزت الحد الائتماني — قد يتوقف اعتماد الطلبية.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    if (state.error != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = state.error.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    AppButton(
                        text = "تأكيد الطلبية",
                        onClick = viewModel::submit,
                        enabled = state.canCheckout,
                        loading = state.isSubmitting,
                        icon = Icons.Filled.ShoppingCartCheckout,
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (state.lastCreatedOrderId != null) {
                    AppCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            text = "تم إنشاء الطلبية ${state.lastCreatedOrderNumber.orEmpty()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        AppButton(
                            text = "تتبّع الطلبية",
                            onClick = {
                                val created = state.lastCreatedOrderId
                                viewModel.dismissCreated()
                                if (created != null) onCreated(created)
                            },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}