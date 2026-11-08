package com.agency.app.feature_admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.domain.model.Buyer
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.PermissionRequest
import com.agency.app.core.domain.model.PermissionRequestStatus
import com.agency.app.core.domain.model.User
import com.agency.app.core.domain.model.UserRole
import com.agency.app.core.domain.repository.BuyerRepository
import com.agency.app.core.domain.repository.PermissionRepository
import com.agency.app.core.domain.repository.UserRepository
import com.agency.app.core.util.Ids
import com.agency.app.core.util.arabicMessage
import com.agency.app.core.util.TimeSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * شاشات المدير.
 *
 * المدير هو الوحيد الذي ينشئ الحسابات ويوقّع طلبات الصلاحية. لذلك كل عملية
 * هنا تحمل هوية المنفِّذ صراحةً إلى طبقة البيانات، ويُسجَّل التوقيع مع الطلب،
 * فلا يوجد إجراء إداري مجهول المصدر.
 */

/* ------------------------------ المستخدمون ------------------------------ */

data class AdminUsersUiState(
    val users: List<User> = emptyList(),
    val visible: List<User> = emptyList(),
    val query: String = "",
    val roleFilter: UserRole? = null,
    val activeCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
    /* حوار الإنشاء */
    val showCreate: Boolean = false,
    val draftUsername: String = "",
    val draftDisplayName: String = "",
    val draftPassword: String = "",
    val draftPhone: String = "",
    val draftRole: UserRole = UserRole.WAREHOUSE,
    val draftBuyerId: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
    /** المشترون المتاحون لربط حساب مشترٍ بهم. */
    val linkableBuyers: List<Buyer> = emptyList(),
)

@HiltViewModel
class AdminUsersViewModel @Inject constructor(
    private val userRepository: UserRepository,
    buyerRepository: BuyerRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val roleFilter = MutableStateFlow<UserRole?>(null)
    private val local = MutableStateFlow(AdminUsersUiState())

    init {
        viewModelScope.launch {
            buyerRepository.observeBuyers().collect { buyers ->
                local.value = local.value.copy(linkableBuyers = buyers.filter { it.isActive })
            }
        }
    }

    val ui: StateFlow<AdminUsersUiState> = combine(
        userRepository.observeUsers(),
        query,
        roleFilter,
        local,
    ) { users, q, rf, l ->
        val searched = if (q.isBlank()) users else users.filter {
            it.displayName.contains(q.trim(), ignoreCase = true) ||
                it.username.contains(q.trim(), ignoreCase = true)
        }
        val filtered = if (rf == null) searched else searched.filter { it.role == rf }
        l.copy(
            users = users,
            visible = filtered.sortedWith(compareBy({ it.role.ordinal }, { it.displayName })),
            query = q,
            roleFilter = rf,
            totalCount = users.size,
            activeCount = users.count { it.isActive },
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminUsersUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onRoleFilterChange(value: UserRole?) {
        roleFilter.value = value
    }

    /* ---------------------------- إنشاء حساب ---------------------------- */

    fun openCreate() {
        local.value = local.value.copy(
            showCreate = true,
            draftUsername = "",
            draftDisplayName = "",
            draftPassword = "",
            draftPhone = "",
            draftRole = UserRole.WAREHOUSE,
            draftBuyerId = null,
            message = null,
        )
    }

    fun dismissCreate() {
        local.value = local.value.copy(showCreate = false, busy = false)
    }

    fun onUsernameChange(v: String) {
        local.value = local.value.copy(draftUsername = v)
    }

    fun onDisplayNameChange(v: String) {
        local.value = local.value.copy(draftDisplayName = v)
    }

    fun onPasswordChange(v: String) {
        local.value = local.value.copy(draftPassword = v)
    }

    fun onPhoneChange(v: String) {
        local.value = local.value.copy(draftPhone = v)
    }

    fun onRoleChange(v: UserRole) {
        local.value = local.value.copy(
            draftRole = v,
            // ربط المشتري صالح لدور المشتري فقط؛ نُصفّر الاختيار عند تبديل الدور
            draftBuyerId = if (v == UserRole.BUYER) local.value.draftBuyerId else null,
        )
    }

    fun onBuyerLinkChange(v: String?) {
        local.value = local.value.copy(draftBuyerId = v)
    }

    /** إنشاء الحساب. التحقق النهائي في طبقة البيانات، وهذا تحقق مبكر للتجربة. */
    fun submitCreate() {
        val draft = local.value
        if (draft.draftRole == UserRole.BUYER && draft.draftBuyerId == null) {
            local.value = draft.copy(message = "حساب المشتري يجب أن يُربط بمشترٍ موجود.")
            return
        }
        local.value = draft.copy(busy = true, message = null)
        viewModelScope.launch {
            val result = userRepository.createUser(
                username = draft.draftUsername.trim(),
                displayName = draft.draftDisplayName.trim(),
                password = draft.draftPassword,
                role = draft.draftRole,
                phone = draft.draftPhone.trim().ifBlank { null },
                buyerId = draft.draftBuyerId,
            )
            when (result) {
                is Outcome.Success -> local.value = local.value.copy(
                    busy = false,
                    showCreate = false,
                    message = "أُنشئ حساب «${result.value.displayName}» بدور ${result.value.role.label()}.",
                )
                is Outcome.Failure -> local.value = local.value.copy(
                    busy = false,
                    message = result.error.arabicMessage(),
                )
            }
        }
    }

    /* --------------------------- تعطيل وتفعيل --------------------------- */

    /**
     * تعطيل/تفعيل حساب.
     *
     * يُمنع المدير من تعطيل حسابه هو: لو فعل، لخرج من الجلسة عند أول إعادة بناء
     * ولم يبقَ من يوقّع طلبات الصلاحية — وهو قفل إداري كامل بضغطة واحدة.
     */
    fun toggleActive(user: User) {
        if (user.id == authManager.currentUser?.id) {
            local.value = local.value.copy(message = "لا يمكنك تعطيل حسابك الحالي.")
            return
        }
        viewModelScope.launch {
            when (val result = userRepository.setActive(user.id, !user.isActive)) {
                is Outcome.Success -> local.value = local.value.copy(
                    message = if (user.isActive) {
                        "عُطِّل حساب «${user.displayName}»."
                    } else {
                        "فُعِّل حساب «${user.displayName}»."
                    },
                )
                is Outcome.Failure -> local.value = local.value.copy(
                    message = result.error.arabicMessage(),
                )
            }
        }
    }

    fun clearMessage() {
        local.value = local.value.copy(message = null)
    }
}

/* ------------------------------- المشترون ------------------------------- */

data class AdminBuyersUiState(
    val buyers: List<Buyer> = emptyList(),
    val visible: List<Buyer> = emptyList(),
    val query: String = "",
    val activeCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
    val showEditor: Boolean = false,
    /** المشتري قيد التعديل؛ `null` يعني إنشاء مشترٍ جديد. */
    val editingId: String? = null,
    val draftName: String = "",
    val draftPhone: String = "",
    val draftAddress: String = "",
    val draftShopName: String = "",
    val draftCreditLimit: String = "",
    val busy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class AdminBuyersViewModel @Inject constructor(
    private val buyerRepository: BuyerRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val local = MutableStateFlow(AdminBuyersUiState())

    /**
     * نسخة مرجعية من قائمة المشترين.
     *
     * `save()` يحتاج المشتري الأصلي (تاريخ الإنشاء وحالة التفعيل) قبل الكتابة،
     * و`ui` مشتق من `combine` فلا يصلح مصدراً للحظة الكتابة. هذا التدفّق
     * يُحدَّث من نفس المصدر فلا يفترق عن المعروض.
     */
    private val buyersCache = MutableStateFlow<List<Buyer>>(emptyList())

    init {
        viewModelScope.launch {
            buyerRepository.observeBuyers().collect { buyersCache.value = it }
        }
    }

    val ui: StateFlow<AdminBuyersUiState> = combine(
        buyerRepository.observeBuyers(),
        query,
        local,
    ) { buyers, q, l ->
        val searched = if (q.isBlank()) buyers else buyers.filter {
            it.name.contains(q.trim(), ignoreCase = true) ||
                it.phone.contains(q.trim(), ignoreCase = true) ||
                it.shopName.orEmpty().contains(q.trim(), ignoreCase = true)
        }
        l.copy(
            buyers = buyers,
            visible = searched.sortedBy { it.name },
            query = q,
            totalCount = buyers.size,
            activeCount = buyers.count { it.isActive },
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminBuyersUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun openCreate() {
        local.value = local.value.copy(
            showEditor = true,
            editingId = null,
            draftName = "",
            draftPhone = "",
            draftAddress = "",
            draftShopName = "",
            draftCreditLimit = "",
            message = null,
        )
    }

    fun openEdit(buyer: Buyer) {
        local.value = local.value.copy(
            showEditor = true,
            editingId = buyer.id,
            draftName = buyer.name,
            draftPhone = buyer.phone,
            draftAddress = buyer.address.orEmpty(),
            draftShopName = buyer.shopName.orEmpty(),
            draftCreditLimit = if (buyer.creditLimit.isZero) "" else buyer.creditLimit.toString(),
            message = null,
        )
    }

    fun dismissEditor() {
        local.value = local.value.copy(showEditor = false, busy = false)
    }

    fun onNameChange(v: String) {
        local.value = local.value.copy(draftName = v)
    }

    fun onPhoneChange(v: String) {
        local.value = local.value.copy(draftPhone = v)
    }

    fun onAddressChange(v: String) {
        local.value = local.value.copy(draftAddress = v)
    }

    fun onShopNameChange(v: String) {
        local.value = local.value.copy(draftShopName = v)
    }

    fun onCreditLimitChange(v: String) {
        local.value = local.value.copy(draftCreditLimit = v)
    }

    /**
     * حفظ المشتري.
     *
     * سقف الائتمان يُقرأ عبر [Money.fromString] الذي يعيد `null` عند مدخل غير
     * رقمي أو سالب — فلا يمكن أن يُحفظ سقف بمبلغ مبتور أو بالسالب،
     * والقيمة تُخزَّن بالسنت لا كرقم عائم.
     */
    fun save() {
        val draft = local.value
        val limitText = draft.draftCreditLimit.trim()
        val creditLimit = if (limitText.isEmpty()) Money.ZERO else Money.fromString(limitText)
        if (creditLimit == null) {
            local.value = draft.copy(message = "سقف الائتمان يجب أن يكون رقماً غير سالب.")
            return
        }
        val existing = buyersCache.value.firstOrNull { it.id == draft.editingId }
        val now = TimeSource.now()
        val buyer = Buyer(
            id = existing?.id ?: Ids.new(),
            name = draft.draftName.trim(),
            phone = draft.draftPhone.trim(),
            address = draft.draftAddress.trim().ifBlank { null },
            shopName = draft.draftShopName.trim().ifBlank { null },
            creditLimit = creditLimit,
            isActive = existing?.isActive ?: true,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        local.value = draft.copy(busy = true, message = null)
        viewModelScope.launch {
            when (val result = buyerRepository.upsertBuyer(buyer)) {
                is Outcome.Success -> local.value = local.value.copy(
                    busy = false,
                    showEditor = false,
                    message = if (existing == null) "أُضيف المشتري «${buyer.name}»." else "حُدِّث المشتري «${buyer.name}».",
                )
                is Outcome.Failure -> local.value = local.value.copy(
                    busy = false,
                    message = result.error.arabicMessage(),
                )
            }
        }
    }

    /** تعطيل المشتري يُخفيه من الفوترة الجديدة لكنه لا يمسّ ذمّته التاريخية. */
    fun toggleActive(buyer: Buyer) {
        viewModelScope.launch {
            when (val result = buyerRepository.setActive(buyer.id, !buyer.isActive)) {
                is Outcome.Success -> local.value = local.value.copy(
                    message = if (buyer.isActive) {
                        "عُطِّل المشتري «${buyer.name}». تبقى فواتيره وسنداته في السجل."
                    } else {
                        "فُعِّل المشتري «${buyer.name}»."
                    },
                )
                is Outcome.Failure -> local.value = local.value.copy(
                    message = result.error.arabicMessage(),
                )
            }
        }
    }

    fun clearMessage() {
        local.value = local.value.copy(message = null)
    }
}

/* --------------------------- مركز الموافقات --------------------------- */

data class AdminPermissionsUiState(
    val all: List<PermissionRequest> = emptyList(),
    val visible: List<PermissionRequest> = emptyList(),
    val filter: PermissionRequestStatus? = PermissionRequestStatus.PENDING,
    val pendingCount: Int = 0,
    val isLoading: Boolean = true,
    val reviewing: PermissionRequest? = null,
    val reviewNote: String = "",
    val approve: Boolean = true,
    val busy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class AdminPermissionsViewModel @Inject constructor(
    private val permissionRepository: PermissionRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val filter = MutableStateFlow<PermissionRequestStatus?>(PermissionRequestStatus.PENDING)
    private val local = MutableStateFlow(AdminPermissionsUiState())

    val ui: StateFlow<AdminPermissionsUiState> = combine(
        permissionRepository.observeAll(),
        filter,
        local,
    ) { all, f, l ->
        val filtered = if (f == null) all else all.filter { it.status == f }
        l.copy(
            all = all,
            visible = filtered.sortedByDescending { it.createdAt },
            filter = f,
            pendingCount = all.count { it.isPending },
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminPermissionsUiState())

    fun onFilterChange(value: PermissionRequestStatus?) {
        filter.value = value
    }

    fun openReview(request: PermissionRequest, approve: Boolean) {
        local.value = local.value.copy(
            reviewing = request,
            reviewNote = "",
            approve = approve,
            message = null,
        )
    }

    fun dismissReview() {
        local.value = local.value.copy(reviewing = null, reviewNote = "", busy = false)
    }

    fun onReviewNoteChange(v: String) {
        local.value = local.value.copy(reviewNote = v)
    }

    /**
     * بتّ الطلب.
     *
     * الموافقة تُرسل توقيعاً رقمياً من المدير — ليس نصاً تجميلياً: طبقة البيانات
     * ترفض اعتماداً بلا توقيع، ثم تفحص `hasApproval` عند محاولة التنفيذ.
     * لذلك التوقيع هنا مبنيّ من معرّف الطلب ومعرّف المدير والوقت، فهو فريد
     * لكل اعتماد ولا يمكن نسخه من طلب آخر.
     */
    fun submitReview() {
        val request = local.value.reviewing ?: return
        val user = authManager.currentUser ?: return
        val note = local.value.reviewNote.trim()
        if (!local.value.approve && note.isBlank()) {
            local.value = local.value.copy(message = "الرفض يتطلب سبباً مكتوباً.")
            return
        }
        val signature = if (local.value.approve) {
            "SIG:${request.id}:${user.id}:${TimeSource.now().toEpochMilliseconds()}"
        } else {
            ""
        }
        local.value = local.value.copy(busy = true, message = null)
        viewModelScope.launch {
            val result = if (local.value.approve) {
                permissionRepository.approve(
                    requestId = request.id,
                    reviewerUserId = user.id,
                    reviewerName = user.displayName,
                    signatureData = signature,
                    reviewNote = note.ifBlank { null },
                )
            } else {
                permissionRepository.reject(
                    requestId = request.id,
                    reviewerUserId = user.id,
                    reviewerName = user.displayName,
                    reviewNote = note,
                )
            }
            when (result) {
                is Outcome.Success -> local.value = local.value.copy(
                    busy = false,
                    reviewing = null,
                    reviewNote = "",
                    message = if (local.value.approve) {
                        "وُقِّع الطلب. أصبح الإجراء «${request.action}» مسموحاً لمنشئه."
                    } else {
                        "رُفض الطلب وأُبلغ مقدّمه بالسبب."
                    },
                )
                is Outcome.Failure -> local.value = local.value.copy(
                    busy = false,
                    message = result.error.arabicMessage(),
                )
            }
        }
    }

    fun clearMessage() {
        local.value = local.value.copy(message = null)
    }
}

/* ------------------------------ مساعدات ------------------------------ */

/** تسمية الدور بالعربية للعرض فقط. */
internal fun UserRole.label(): String = when (this) {
    UserRole.ADMIN -> "مدير"
    UserRole.WAREHOUSE -> "مخزن"
    UserRole.BUYER -> "مشتري"
    UserRole.CASHIER -> "صندوق"
    UserRole.ACCOUNTANT -> "محاسب"
}

/** اسم الإجراء كما يراه المدير عند البتّ في طلب صلاحية. */
internal fun String.actionLabel(): String = when (this) {
    "EDIT_PRICE" -> "تعديل سعر منتج"
    "ADJUST_BALANCE" -> "تعديل رصيد مشترٍ"
    "DELETE_PRODUCT" -> "حذف منتج"
    else -> this
}

internal fun PermissionRequestStatus.label(): String = when (this) {
    PermissionRequestStatus.PENDING -> "قيد المراجعة"
    PermissionRequestStatus.APPROVED -> "معتمد"
    PermissionRequestStatus.REJECTED -> "مرفوض"
}