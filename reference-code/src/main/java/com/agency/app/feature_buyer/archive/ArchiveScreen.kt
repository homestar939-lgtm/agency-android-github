// feature_buyer/archive/ArchiveScreen.kt
package com.agency.app.feature_buyer.archive

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.agency.app.core.ui.components.AppCard

enum class TransactionType {
    DEBIT,   // مدين (عليه)
    CREDIT   // دائن (له)
}

data class ArchiveItem(
    val id: String,
    val type: TransactionType,
    val title: String,
    val description: String,
    val amount: Double,
    val date: String,
    val reference: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: ArchiveViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadArchive()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أرشيفي", fontWeight = FontWeight.Bold) },
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
        ) {
            // بطاقة الملخص المالي
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    BalanceColumn(
                        label = "إجمالي المشتريات",
                        amount = uiState.totalDebit,
                        icon = Icons.Filled.ShoppingCart,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    VerticalDivider(
                        modifier = Modifier.height(60.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                    )
                    
                    BalanceColumn(
                        label = "إجمالي المدفوعات",
                        amount = uiState.totalCredit,
                        icon = Icons.Filled.Receipt,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // الرصيد الحالي
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "الرصيد المستحق",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${uiState.currentBalance} د.ع",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.currentBalance > 0) 
                                MaterialTheme.colorScheme.error 
                            else MaterialTheme.colorScheme.tertiary
                        )
                    }
                    
                    Surface(
                        color = if (uiState.currentBalance > 0) 
                            MaterialTheme.colorScheme.errorContainer 
                        else MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = if (uiState.currentBalance > 0) "مطلوب منك" else "لا يوجد مستحقات",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // التبويبات
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("الكل")
                            Badge { Text(uiState.allItems.size.toString()) }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("المشتريات")
                            Badge { Text(uiState.debitItems.size.toString()) }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("المدفوعات")
                            Badge { Text(uiState.creditItems.size.toString()) }
                        }
                    }
                )
            }

            // القائمة
            val items = when (selectedTab) {
                0 -> uiState.allItems
                1 -> uiState.debitItems
                else -> uiState.creditItems
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    ArchiveItemCard(item = item)
                }
            }
        }
    }
}

@Composable
private fun BalanceColumn(
    label: String,
    amount: Double,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, label, tint = color, modifier = Modifier.size(28.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Text(
            "${amount.toInt()} د.ع",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun ArchiveItemCard(item: ArchiveItem) {
    val isDebit = item.type == TransactionType.DEBIT
    val containerColor = if (isDebit) 
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    else 
        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    
    val iconColor = if (isDebit) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.tertiary

    AppCard(
        containerColor = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = iconColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isDebit) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                            item.type.name,
                            tint = iconColor
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        "${item.date} • #${item.reference}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isDebit) "-" else "+"} ${item.amount.toInt()} د.ع",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
                Text(
                    text = if (isDebit) "مدين" else "دائن",
                    style = MaterialTheme.typography.labelSmall,
                    color = iconColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}
