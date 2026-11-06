// feature_buyer/archive/ArchiveViewModel.kt
package com.agency.app.feature_buyer.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArchiveUiState(
    val isLoading: Boolean = true,
    val allItems: List<ArchiveItem> = emptyList(),
    val debitItems: List<ArchiveItem> = emptyList(),
    val creditItems: List<ArchiveItem> = emptyList(),
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0,
    val currentBalance: Double = 0.0
)

@HiltViewModel
class ArchiveViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    fun loadArchive() {
        viewModelScope.launch {
            val items = listOf(
                ArchiveItem(
                    id = "1",
                    type = TransactionType.DEBIT,
                    title = "فاتورة #1024",
                    description = "رز، زيت، سكر - وكالة النور",
                    amount = 28500.0,
                    date = "07 نوفمبر 2026",
                    reference = "INV-1024"
                ),
                ArchiveItem(
                    id = "2",
                    type = TransactionType.CREDIT,
                    title = "سند قبض #501",
                    description = "دفعة عبر المتحصل أحمد",
                    amount = 15000.0,
                    date = "06 نوفمبر 2026",
                    reference = "RCP-501"
                ),
                ArchiveItem(
                    id = "3",
                    type = TransactionType.DEBIT,
                    title = "فاتورة #1018",
                    description = "معلبات متنوعة",
                    amount = 12000.0,
                    date = "01 نوفمبر 2026",
                    reference = "INV-1018"
                )
            )

            val debitItems = items.filter { it.type == TransactionType.DEBIT }
            val creditItems = items.filter { it.type == TransactionType.CREDIT }
            val totalDebit = debitItems.sumOf { it.amount }
            val totalCredit = creditItems.sumOf { it.amount }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    allItems = items,
                    debitItems = debitItems,
                    creditItems = creditItems,
                    totalDebit = totalDebit,
                    totalCredit = totalCredit,
                    currentBalance = totalDebit - totalCredit
                )
            }
        }
    }
}
