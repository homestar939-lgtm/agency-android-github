// feature_cashier/stats/CollectionStatsScreen.kt
package com.agency.app.feature_cashier.stats

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agency.app.core.ui.components.AppCard

data class DailyCollection(
    val date: String,
    val amount: Double,
    val receiptsCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionStatsScreen(
    viewModel: CollectionStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var selectedPeriod by remember { mutableStateOf("MONTH") }

    LaunchedEffect(selectedPeriod) {
        viewModel.loadStats(selectedPeriod)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إحصائياتي", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { /* تصدير */ }) {
                        Icon(Icons.Filled.FileDownload, "تصدير")
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
            // محدد الفترة
            AppCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "الفترة الزمنية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PeriodChip(
                            text = "اليوم",
                            selected = selectedPeriod == "TODAY",
                            onClick = { selectedPeriod = "TODAY" },
                            modifier = Modifier.weight(1f)
                        )
                        PeriodChip(
                            text = "الشهر",
                            selected = selectedPeriod == "MONTH",
                            onClick = { selectedPeriod = "MONTH" },
                            modifier = Modifier.weight(1f)
                        )
                        PeriodChip(
                            text = "السنة",
                            selected = selectedPeriod == "YEAR",
                            onClick = { selectedPeriod = "YEAR" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        uiState.periodRange,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // البطاقة الرئيسية — إجمالي العمولة
            AppCard(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.EmojiEvents,
                        "العمولة",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        "إجمالي عمولتي",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        "${uiState.totalCommission.toInt()} د.ع",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        "من ${uiState.totalCollected.toInt()} د.ع محصّلة",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // شبكة الإحصائيات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Receipt,
                    label = "عدد السندات",
                    value = "${uiState.receiptsCount}",
                    color = MaterialTheme.colorScheme.primary
                )
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.People,
                    label = "مشتري خدمتهم",
                    value = "${uiState.buyersServed}",
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.TrendingUp,
                    label = "متوسط السند",
                    value = "${uiState.averageReceipt.toInt()} د.ع",
                    color = MaterialTheme.colorScheme.tertiary
                )
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Speed,
                    label = "أفضل يوم",
                    value = "${uiState.bestDayAmount.toInt()}",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // الرسم البياني (تمثيل نصي)
            AppCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "الأداء اليومي",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    uiState.dailyCollections.forEach { day ->
                        DailyBar(day = day, maxAmount = uiState.maxDailyAmount)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PeriodChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        modifier = modifier
    )
}

@Composable
private fun MiniStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    AppCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, label, tint = color, modifier = Modifier.size(24.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun DailyBar(
    day: DailyCollection,
    maxAmount: Double
) {
    val progress = if (maxAmount > 0) (day.amount / maxAmount).toFloat() else 0f
    
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                day.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${day.amount.toInt()} د.ع",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
