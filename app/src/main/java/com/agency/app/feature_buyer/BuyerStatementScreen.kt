package com.agency.app.feature_buyer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.HorizontalDivider
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
import com.agency.app.core.domain.model.BuyerStatementEntry
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.EmptyState
import com.agency.app.core.ui.components.InfoRow
import com.agency.app.core.ui.components.SectionHeader
import com.agency.app.core.ui.components.formatted
import com.agency.app.core.util.formattedDateTime

/**
 * كشف الحساب.
 *
 * كل سطر يُظهر مديناً أو دائناً مع الرصيد بعد الحركة، وهو الرصيد المحسوب
 * في [com.agency.app.core.data.repository.BuyerRepositoryImpl.observeStatement]
 * لا رصيداً يعيد حسابه العرض.
 */
@Composable
fun BuyerStatementScreen(
    viewModel: BuyerStatementViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val balance = state.balance

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            if (balance != null) {
                AppCard {
                    SectionHeader(title = "الرصيد")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "الذمة القائمة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = balance.outstanding.formatted(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (balance.isOverLimit) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    InfoRow(label = "إجمالي المفوتر", value = balance.totalInvoiced.formatted())
                    InfoRow(label = "إجمالي المسدَّد", value = balance.totalPaid.formatted())
                    InfoRow(label = "الحد الائتماني", value = balance.creditLimit.formatted())
                    InfoRow(label = "نسبة السداد", value = "${balance.collectionRate}%")
                }
            }
            Spacer(Modifier.height(12.dp))
            if (state.entries.isEmpty()) {
                EmptyState(
                    title = "لا حركات مالية",
                    description = "سيظهر هنا كل ما يُفوتر عليك وكل ما تسدده.",
                    icon = Icons.Filled.AccountBalanceWallet,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.entries, key = { it.id }) { entry ->
                        StatementRow(entry)
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StatementRow(entry: BuyerStatementEntry) {
    val isDebit = entry.debit.isPositive
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = entry.at.formattedDateTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = if (isDebit) "+${entry.debit.formatted()}" else "−${entry.credit.formatted()}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isDebit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
        HorizontalDivider(Modifier.padding(vertical = 6.dp))
        InfoRow(label = "الرصيد بعد الحركة", value = entry.balanceAfter.formatted())
    }
}