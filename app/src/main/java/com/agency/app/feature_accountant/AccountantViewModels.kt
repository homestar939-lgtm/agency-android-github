package com.agency.app.feature_accountant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.domain.model.BuyerBalance
import com.agency.app.core.domain.model.BuyerStatementEntry
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.PermissionRequest
import com.agency.app.core.domain.model.UserRole
import com.agency.app.core.domain.repository.BuyerRepository
import com.agency.app.core.domain.repository.PermissionRepository
import com.agency.app.core.util.arabicMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * شاشات المحاسب.
 *
 * قاعدة هذا الدور ليست «لا أزرار» بل «لا كتابة مباشرة»: كل ما يراه المحاسب
 * مقروء، وأي إجراء يغيّر رقماً في ذمة مشترٍ يمرّ عبر [PermissionRepository]
 * بانتظار توقيع المدير. لهذا لا يستدعي أي مستدعٍ هنا `setActive` أو `upsert`
 * أو `collect` — بل `request` فقط.
 */

/* ------------------------- أرصدة المشترين ------------------------- */

/** فلتر الأرصدة: الكل، المدينون فقط، المتجاوزون سقفهم. */
enum class BalanceFilter(val label: String) {
    ALL("الكل"),
    OUTSTANDING("عليهم رصيد"),
    OVER_LIMIT("تجاوزوا السقف"),
}

data class AccountantBalancesUiState(
    val balances: List<BuyerBalance> = emptyList(),
    val visible: List<BuyerBalance> = emptyList(),
    val query: String = "",
    val filter: BalanceFilter = BalanceFilter.ALL,
    val totalOutstanding: Money = Money.ZERO,
    val totalInvoiced: Money = Money.ZERO,
    val totalPaid: Money = Money.ZERO,
    val overLimitCount: Int = 0,
    val isLoading: Boolean = true,
    val myRequests: List<PermissionRequest> = emptyList(),
    /** الطلب الذي يجري كتابة سببه الآن؛ `null` يعني الحوار مغلق. */
    val requestTarget: BuyerBalance? = null,
    val requestReason: String = "",
    val requestBusy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class AccountantBalancesViewModel @Inject constructor(
    buyerRepository: BuyerRepository,
    private val permissionRepository: PermissionRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(BalanceFilter.ALL)
    private val requests = MutableStateFlow<List<PermissionRequest>>(emptyList())
    private val local = MutableStateFlow(AccountantBalancesUiState())

    private val role: UserRole get() = authManager.currentRole ?: UserRole.ACCOUNTANT
    private val me = authManager.currentUser

    init {
        viewModelScope.launch {
            permissionRepository.observeAll().collect { all ->
                requests.value = all.filter { it.requesterUserId == me?.id }
            }
        }
    }

    val ui: StateFlow<AccountantBalancesUiState> = combine(
        buyerRepository.observeBalances(),
        query,
        filter,
        requests,
        local,
    ) { balances, q, f, myReqs, l ->
        val searched = if (q.isBlank()) balances else balances.filter {
            it.buyerName.contains(q.trim(), ignoreCase = true)
        }
        val filtered = when (f) {
            BalanceFilter.ALL -> searched
            BalanceFilter.OUTSTANDING -> searched.filter { !it.isSettled }
            BalanceFilter.OVER_LIMIT -> searched.filter { it.isOverLimit }
        }
        l.copy(
            balances = balances,
            visible = filtered.sortedByDescending { it.outstanding.cents },
            query = q,
            filter = f,
            totalOutstanding = balances.fold(Money.ZERO) { acc, b -> acc + b.outstanding },
            totalInvoiced = balances.fold(Money.ZERO) { acc, b -> acc + b.totalInvoiced },
            totalPaid = balances.fold(Money.ZERO) { acc, b -> acc + b.totalPaid },
            overLimitCount = balances.count { it.isOverLimit },
            myRequests = myReqs.sortedByDescending { it.createdAt },
            isLoading = false,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AccountantBalancesUiState()
    )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: BalanceFilter) {
        filter.value = value
    }

    /* --------------------- طلب صلاحية بدل التنفيذ --------------------- */

    fun openRequest(target: BuyerBalance) {
        local.value = local.value.copy(requestTarget = target, requestReason = "", message = null)
    }

    fun dismissRequest() {
        local.value = local.value.copy(requestTarget = null, requestReason = "", requestBusy = false)
    }

    fun onReasonChange(value: String) {
        local.value = local.value.copy(requestReason = value)
    }

    fun clearMessage() {
        local.value = local.value.copy(message = null)
    }

    /**
     * إرسال طلب صلاحية على رصيد مشترٍ.
     *
     * السبب إلزامي: طلب بلا سبب لا يمكن للمدير أن يوافق عليه بوعي،
     * ورفضُ الطلب الفارغ هنا يوفّر دورة موافقة كاملة عقيمة.
     */
    fun submitRequest() {
        val target = local.value.requestTarget ?: return
        val reason = local.value.requestReason.trim()
        val user = me ?: return
        if (reason.length < MIN_REASON_LENGTH) {
            local.value = local.value.copy(
                message = "اكتب سبباً واضحاً لا يقل عن $MIN_REASON_LENGTH أحرف قبل الإرسال."
            )
            return
        }
        local.value = local.value.copy(requestBusy = true, message = null)
        viewModelScope.launch {
            val result = permissionRepository.request(
                requesterUserId = user.id,
                requesterName = user.displayName,
                requesterRole = user.role,
                action = ACTION_ADJUST_BALANCE,
                targetEntity = ENTITY_BUYER,
                targetId = target.buyerId,
                reason = reason,
            )
            when (result) {
                is Outcome.Success -> local.value = local.value.copy(
                    requestBusy = false,
                    requestTarget = null,
                    requestReason = "",
                    message = "أُرسل الطلب للمدير. لن يتغيّر رصيد «${target.buyerName}» قبل توقيعه.",
                )
                is Outcome.Failure -> local.value = local.value.copy(
                    requestBusy = false,
                    message = result.error.arabicMessage(),
                )
            }
        }
    }

    companion object {
        const val ACTION_ADJUST_BALANCE: String = "ADJUST_BALANCE"
        const val ENTITY_BUYER: String = "buyer"
        const val MIN_REASON_LENGTH: Int = 10
    }
}

/* ------------------------- كشف حساب مشترٍ ------------------------- */

data class AccountantStatementUiState(
    val buyerId: String = "",
    val balance: BuyerBalance? = null,
    val entries: List<BuyerStatementEntry> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class AccountantStatementViewModel @Inject constructor(
    buyerRepository: BuyerRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** المفتاح يجب أن يطابق اسم وسيط `Route.AccountantStatement` حرفياً. */
    private val buyerId: String = savedStateHandle.get<String>("buyerId").orEmpty()

    val ui: StateFlow<AccountantStatementUiState> = combine(
        buyerRepository.observeBalance(buyerId),
        buyerRepository.observeStatement(buyerId),
    ) { balance, entries ->
        AccountantStatementUiState(
            buyerId = buyerId,
            balance = balance,
            entries = entries,
            isLoading = false,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AccountantStatementUiState(buyerId = buyerId)
    )
}
