@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.agency.app.feature_buyer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agency.app.core.ui.arabic
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.BadgePill
import com.agency.app.core.ui.components.CurrencyText
import com.agency.app.core.ui.components.EmptyState
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.LoadingShimmer
import com.agency.app.core.ui.components.SectionHeader
import com.agency.app.core.ui.components.formatted
import com.agency.app.core.ui.theme.LocalAgencyAccents
import com.agency.app.core.util.formattedDate

/* ==========================================================================
 * شاشات المشتري الإضافية — الملف الشخصي والأرشيف.
 * ========================================================================== */

@Composable
fun BuyerProfileScreen(
    viewModel: BuyerProfileViewModel,
    onBack: () -> Unit = {},
) {
    val state by viewModel.ui.collectAsState()
    val accents = LocalAgencyAccents.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ملفي وذمّتي", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding)) { LoadingShimmer() }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "@${state.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BadgePill(text = "مشتري", color = accents.primary)
                }
                Spacer(Modifier.height(10.dp))
                InfoRow(label = "الهاتف", value = state.phone.ifBlank { "—" })
                InfoRow(label = "تاريخ التسجيل", value = state.joinedAt.ifBlank { "—" })
            }

            if (!state.isLinked) {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = accents.warning,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "لا ذمّة معروضة",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = accents.warning,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.linkHint ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                AppCard {
                    SectionHeader("ذمّتي")
                    Spacer(Modifier.height(10.dp))
                    CurrencyText(
                        amount = state.outstanding,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isOverLimit) accents.warning else accents.success,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (state.outstanding.isZero) "لا مستحق عليك" else "المبلغ المستحق عليك",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    InfoRow(label = "إجمالي المفوتر", value = state.totalInvoiced.formatted())
                    InfoRow(label = "إجمالي المدفوع", value = state.totalPaid.formatted())
                    InfoRow(label = "سقف الدين", value = state.creditLimit.formatted())
                    InfoRow(
                        label = "نسبة استخدام السقف",
                        value = "${state.creditUsedPercent}%",
                        valueColor = if (state.isOverLimit) accents.warning else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (state.creditUsedPercent.coerceIn(0, 100)) / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (state.isOverLimit) accents.warning else accents.success,
                    )
                    if (state.isOverLimit) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "تجاوزت السقف. لن تُعتمد طلبيات جديدة حتى تُسوّي المستحق.",
                            style = MaterialTheme.typography.bodySmall,
                            color = accents.warning,
                        )
                    }
                }

                AppCard {
                    SectionHeader("نسبة السداد")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${state.collectionRate}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "النسبة = المدفوع ÷ المفوتر. 100% تعني أن كل ما فُوتر عليك سُدّد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun BuyerArchiveScreen(
    viewModel: BuyerArchiveViewModel,
    onBack: () -> Unit = {},
) {
    val state by viewModel.ui.collectAsState()
    val accents = LocalAgencyAccents.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الأرشيف", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding)) { LoadingShimmer() }
            !state.isLinked -> EmptyState(
                title = "حسابك غير مربوط",
                description = "لا يمكن عرض أرشيف بلا سجل مشترٍ مرتبط بحسابك.",
                modifier = Modifier.padding(padding),
            )
            state.isEmpty -> EmptyState(
                title = "الأرشيف فارغ",
                description = "تظهر هنا الفواتير المسدَّدة والمؤرشفة والطلبيات المنتهية.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    AppCard {
                        SectionHeader("الملخّص")
                        Spacer(Modifier.height(6.dp))
                        InfoRow(
                            label = "إجمالي المفوتر المسدَّد",
                            value = state.totalSettled.formatted(),
                            emphasise = true,
                        )
                        InfoRow(label = "فواتير مسدَّدة", value = "${state.settledInvoices.size}")
                        InfoRow(label = "فواتير مؤرشفة", value = "${state.archivedInvoices.size}")
                        InfoRow(label = "طلبيات منتهية", value = "${state.settledOrders.size}")
                        InfoRow(label = "سندات قبض", value = "${state.receipts.size}")
                    }
                }

                if (state.settledInvoices.isNotEmpty()) {
                    item { SectionHeader("فواتير مسدَّدة") }
                    items(state.settledInvoices) { invoice ->
                        AppCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    invoice.invoiceNumber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                BadgePill(text = "مسدَّدة", color = accents.success)
                            }
                            Spacer(Modifier.height(6.dp))
                            InfoRow(label = "الإجمالي", value = invoice.grandTotal.formatted())
                            InfoRow(label = "التاريخ", value = invoice.createdAt.formattedDate())
                        }
                    }
                }

                if (state.archivedInvoices.isNotEmpty()) {
                    item { SectionHeader("فواتير مؤرشفة") }
                    items(state.archivedInvoices) { invoice ->
                        AppCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    invoice.invoiceNumber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                BadgePill(text = invoice.status.arabic(), color = accents.info)
                            }
                            Spacer(Modifier.height(6.dp))
                            InfoRow(label = "الإجمالي", value = invoice.grandTotal.formatted())
                            InfoRow(label = "التاريخ", value = invoice.createdAt.formattedDate())
                        }
                    }
                }

                if (state.settledOrders.isNotEmpty()) {
                    item { SectionHeader("طلبيات منتهية") }
                    items(state.settledOrders) { order ->
                        AppCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    order.orderNumber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                BadgePill(text = order.status.arabic(), color = accents.info)
                            }
                            Spacer(Modifier.height(6.dp))
                            InfoRow(label = "الإجمالي", value = order.total.formatted())
                            InfoRow(label = "التاريخ", value = order.createdAt.formattedDate())
                        }
                    }
                }
            }
        }
    }
}