package com.agency.app.feature_buyer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.data.auth.SessionState
import com.agency.app.core.domain.model.BuyerBalance
import com.agency.app.core.domain.model.Invoice
import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.Order
import com.agency.app.core.domain.model.Receipt
import com.agency.app.core.domain.repository.BuyerRepository
import com.agency.app.core.domain.repository.InvoiceRepository
import com.agency.app.core.domain.repository.OrderRepository
import com.agency.app.core.domain.repository.ReceiptRepository
import com.agency.app.core.util.formattedDate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/* ==========================================================================
 * شاشات المشتري الإضافية — الملف الشخصي والأرشيف.
 *
 * المشتري يرى بياناته هو فقط. التصفية بـ `buyerId` من الجلسة، لا من وسيط
 * شاشة: وسيط يأتي من الرابط، والرابط يمكن أن يُكتب يدوياً. أي شاشة مشتري
 * تقرأ `buyerId` من مدخل خارجي هي ثغرة عرض، لا ميزة.
 * ========================================================================== */

/** الملف الشخصي للمشتري — الهوية والذمة في مكان واحد. */
data class BuyerProfileUiState(
    val displayName: String = "",
    val username: String = "",
    val shopName: String? = null,
    val phone: String = "",
    val address: String? = null,
    val joinedAt: String = "",
    val creditLimit: Money = Money.ZERO,
    val totalInvoiced: Money = Money.ZERO,
    val totalPaid: Money = Money.ZERO,
    val outstanding: Money = Money.ZERO,
    val creditUsedPercent: Int = 0,
    val isOverLimit: Boolean = false,
    val collectionRate: Int = 0,
    val isLinked: Boolean = false,
    val isLoading: Boolean = true,
    val linkHint: String? = null,
)

@HiltViewModel
class BuyerProfileViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val buyerRepository: BuyerRepository,
) : ViewModel() {

    private val balance = MutableStateFlow<BuyerBalance?>(null)

    init {
        launchBalance()
    }

    /**
     * الجلسة تحمل `buyerId` فقط عند ربط المستخدم بمشترٍ. إن لم يُربط،
     * تُعرض الشاشة بحالة صريحة بدل أرقام صفرية تُقرأ خطأً كـ«لا ذمّة».
     */
    private fun launchBalance() {
        viewModelScope.launch {
            authManager.session.collectLatest { session ->
                val buyerId = (session as? SessionState.Active)?.user?.buyerId
                if (buyerId == null) {
                    balance.value = null
                    return@collectLatest
                }
                buyerRepository.observeBalance(buyerId).collect { fresh ->
                    balance.value = fresh
                }
            }
        }
    }

    val ui: StateFlow<BuyerProfileUiState> =
        combine(authManager.session, balance) { session, bal ->
            val user = (session as? SessionState.Active)?.user
            val buyerId = user?.buyerId
            val usedPercent = if (bal == null || bal.creditLimit.cents <= 0L) 0
            else ((bal.outstanding.cents * 100L) / bal.creditLimit.cents).toInt()

            BuyerProfileUiState(
                displayName = user?.displayName ?: "غير مسجّل",
                username = user?.username ?: "",
                shopName = null,
                phone = user?.phone ?: "",
                address = null,
                joinedAt = user?.createdAt?.formattedDate() ?: "",
                creditLimit = bal?.creditLimit ?: Money.ZERO,
                totalInvoiced = bal?.totalInvoiced ?: Money.ZERO,
                totalPaid = bal?.totalPaid ?: Money.ZERO,
                outstanding = bal?.outstanding ?: Money.ZERO,
                creditUsedPercent = usedPercent,
                isOverLimit = bal?.isOverLimit ?: false,
                collectionRate = bal?.collectionRate ?: 0,
                isLinked = buyerId != null,
                isLoading = session is SessionState.Loading,
                linkHint = if (buyerId == null) {
                    "حسابك غير مربوط بسجل مشترٍ، لذلك لا تُعرض ذمّة. " +
                        "اطلب من المدير ربط الحساب."
                } else {
                    null
                },
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            BuyerProfileUiState(),
        )
}

/**
 * الأرشيف — ما انتهى، مفصولاً عمّا يجري.
 *
 * «أرشيف» هنا تعريف دقيق: فواتير مؤرشفة أو مسدَّدة بالكامل. لم أستخدم
 * «كل شيء» لأن شاشة الأرشيف التي تعرض كل شيء هي قائمة، لا أرشيف. المستخدم
 * يفتح الأرشيف ليجد ما انتهى، فإظهار المفتوح فيه يُفقد الغرض.
 */
data class BuyerArchiveUiState(
    val settledInvoices: List<Invoice> = emptyList(),
    val archivedInvoices: List<Invoice> = emptyList(),
    val settledOrders: List<Order> = emptyList(),
    val receipts: List<Receipt> = emptyList(),
    val totalSettled: Money = Money.ZERO,
    val isLoading: Boolean = true,
    val isLinked: Boolean = true,
) {
    val isEmpty: Boolean
        get() = !isLoading && settledInvoices.isEmpty() &&
            archivedInvoices.isEmpty() && settledOrders.isEmpty()
}

@HiltViewModel
class BuyerArchiveViewModel @Inject constructor(
    invoiceRepository: InvoiceRepository,
    orderRepository: OrderRepository,
    receiptRepository: ReceiptRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    val ui: StateFlow<BuyerArchiveUiState> =
        combine(
            invoiceRepository.observeInvoices(),
            orderRepository.observeOrders(),
            receiptRepository.observeReceipts(),
            authManager.session,
        ) { invoiceRows, orderRows, receiptRows, session ->
            val buyerId = (session as? SessionState.Active)?.user?.buyerId
            if (buyerId == null) {
                return@combine BuyerArchiveUiState(isLoading = false, isLinked = false)
            }

            val myInvoices = invoiceRows.filter { it.buyerId == buyerId }
            val myOrders = orderRows.filter { it.buyerId == buyerId }
            val myReceipts = receiptRows.filter { it.buyerId == buyerId }

            /* سداد كامل = مجموع السندات يغطي إجمالي الفاتورة المعتمدة. */
            val paidPerInvoice = myReceipts
                .filter { it.invoiceId != null }
                .groupBy { it.invoiceId!! }
                .mapValues { (_, rows) -> rows.fold(Money.ZERO) { acc, r -> acc + r.amount } }

            val settled = myInvoices.filter { invoice ->
                invoice.status == InvoiceStatus.APPROVED &&
                    (paidPerInvoice[invoice.id] ?: Money.ZERO).cents >= invoice.grandTotal.cents
            }
            val archived = myInvoices.filter { it.status == InvoiceStatus.ARCHIVED }

            BuyerArchiveUiState(
                settledInvoices = settled.sortedByDescending { it.createdAt },
                archivedInvoices = archived.sortedByDescending { it.createdAt },
                settledOrders = myOrders
                    .filter { it.status.isFinal }
                    .sortedByDescending { it.createdAt },
                receipts = myReceipts.sortedByDescending { it.collectedAt },
                totalSettled = settled.fold(Money.ZERO) { acc, inv -> acc + inv.grandTotal },
                isLoading = false,
                isLinked = true,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            BuyerArchiveUiState(),
        )
}