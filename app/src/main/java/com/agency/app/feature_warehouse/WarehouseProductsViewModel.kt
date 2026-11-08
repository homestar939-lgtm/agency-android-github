package com.agency.app.feature_warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.domain.model.AppError
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.PermissionRequestStatus
import com.agency.app.core.domain.model.Product
import com.agency.app.core.domain.repository.PermissionRepository
import com.agency.app.core.domain.repository.ProductRepository
import com.agency.app.core.util.Ids
import com.agency.app.core.util.TimeSource
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
 * إدارة منتجات المخزن.
 *
 * قاعدتان ماليتان مطبَّقتان هنا حرفياً:
 *  1. تعديل السعر ليس متاحاً إلا بطلب صلاحية معتمد من المدير —
 *     فالحفظ يفحص [PermissionRepository.hasApproval] قبل كتابة السعر.
 *  2. الحذف الناعم فقط. المنتج لا يُمحى من قاعدة البيانات إطلاقاً،
 *     حفاظاً على سلامة الفواتير التاريخية.
 */
data class WarehouseProductsUiState(
    val products: List<Product> = emptyList(),
    val query: String = "",
    val editing: Product? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val priceLocked: Boolean = true,
)

@HiltViewModel
class WarehouseProductsViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val permissionRepository: PermissionRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val _state = MutableStateFlow(WarehouseProductsUiState())
    val ui: StateFlow<WarehouseProductsUiState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")

    private val allProducts: StateFlow<List<Product>> = productRepository.observeAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            combine(allProducts, _query) { rows, text ->
                val visible = if (text.isBlank()) {
                    rows
                } else {
                    rows.filter {
                        it.name.contains(text.trim(), ignoreCase = true) ||
                            it.sku.contains(text.trim(), ignoreCase = true)
                    }
                }
                _state.value = _state.value.copy(products = visible, query = text)
            }.collect { }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun startEdit(product: Product?) {
        _state.value = _state.value.copy(editing = product, error = null, notice = null)
    }

    fun cancelEdit() {
        _state.value = _state.value.copy(editing = null, error = null)
    }

    /**
     * ينشئ منتجاً جديداً أو يحدّث موجوداً.
     * [priceChanged] يُمرَّر من الشاشة لأنها تعرف السعر الأصلي الذي عدّله المستخدم.
     */
    fun save(
        existing: Product?,
        name: String,
        sku: String,
        unit: String,
        priceText: String,
        stockText: String,
        minOrderText: String,
        priceChanged: Boolean,
    ) {
        if (_state.value.isSaving) return
        val user = authManager.currentUser ?: return
        val price = Money.fromString(priceText)
        val stock = stockText.trim().toIntOrNull() ?: -1
        val minOrder = minOrderText.trim().toIntOrNull() ?: 1

        if (name.isBlank()) return fail("اسم المنتج مطلوب")
        if (sku.isBlank()) return fail("رمز المنتج مطلوب")
        if (price == null) return fail("السعر غير صالح")
        if (stock < 0) return fail("الكمية يجب أن تكون صفراً أو أكثر")
        if (minOrder < 1) return fail("أقل كمية يجب أن تكون 1 على الأقل")

        _state.value = _state.value.copy(isSaving = true, error = null, notice = null)
        viewModelScope.launch {
            if (priceChanged && existing != null) {
                val approved = permissionRepository.hasApproval(
                    requesterUserId = user.id,
                    action = ACTION_EDIT_PRICE,
                    targetEntity = "product",
                    targetId = existing.id,
                )
                val isAuthorised = when (approved) {
                    is Outcome.Success -> approved.value || user.role == com.agency.app.core.domain.model.UserRole.ADMIN
                    is Outcome.Failure -> false
                }
                if (!isAuthorised) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = "تعديل السعر يتطلب تصريحاً من المدير. أرسل طلب صلاحية أولاً.",
                    )
                    return@launch
                }
            }

            val now = TimeSource.now()
            val product = existing?.copy(
                name = name.trim(),
                sku = sku.trim(),
                unit = unit.trim().ifBlank { "قطعة" },
                price = price,
                stockQuantity = stock,
                minOrderQuantity = minOrder,
                updatedAt = now,
            ) ?: Product(
                id = Ids.new(),
                name = name.trim(),
                sku = sku.trim(),
                unit = unit.trim().ifBlank { "قطعة" },
                price = price,
                stockQuantity = stock,
                minOrderQuantity = minOrder,
                updatedAt = now,
            )

            when (val result = productRepository.upsert(product)) {
                is Outcome.Success -> _state.value = _state.value.copy(
                    isSaving = false,
                    editing = null,
                    notice = if (existing == null) "أُضيف المنتج «${product.name}»" else "حُدِّث المنتج «${product.name}»",
                )
                is Outcome.Failure -> _state.value = _state.value.copy(
                    isSaving = false,
                    error = describe(result.error),
                )
            }
        }
    }

    /** حذف ناعم: إيقاف المنتج لا محوه. */
    fun deactivate(product: Product) {
        viewModelScope.launch {
            when (val result = productRepository.delete(product.id)) {
                is Outcome.Success -> _state.value = _state.value.copy(
                    notice = "أُوقف المنتج «${product.name}» (لم يُحذف من السجلات)"
                )
                is Outcome.Failure -> _state.value = _state.value.copy(error = describe(result.error))
            }
        }
    }

    fun reactivate(product: Product) {
        viewModelScope.launch {
            when (val result = productRepository.setActive(product.id, true)) {
                is Outcome.Success -> _state.value = _state.value.copy(notice = "أُعيد تنشيط «${product.name}»")
                is Outcome.Failure -> _state.value = _state.value.copy(error = describe(result.error))
            }
        }
    }

    /** يقدّم طلب صلاحية لتعديل سعر منتج — لا يعدّل السعر بنفسه. */
    fun requestPricePermission(product: Product, reason: String) {
        val user = authManager.currentUser ?: return
        if (reason.isBlank()) return fail("اذكر سبب تعديل السعر")
        viewModelScope.launch {
            when (
                val result = permissionRepository.request(
                    requesterUserId = user.id,
                    requesterName = user.displayName,
                    requesterRole = user.role,
                    action = ACTION_EDIT_PRICE,
                    targetEntity = "product",
                    targetId = product.id,
                    reason = reason,
                )
            ) {
                is Outcome.Success -> _state.value = _state.value.copy(
                    notice = "أُرسل طلب تصريح تعديل سعر «${product.name}» إلى المدير"
                )
                is Outcome.Failure -> _state.value = _state.value.copy(error = describe(result.error))
            }
        }
    }

    fun dismissMessages() {
        _state.value = _state.value.copy(error = null, notice = null)
    }

    private fun fail(message: String) {
        _state.value = _state.value.copy(error = message)
    }

    private fun describe(error: AppError): String = when (error) {
        is AppError.Conflict -> error.reason
        is AppError.Permission -> error.required
        is AppError.Validation -> "${error.field}: ${error.reason}"
        is AppError.NotFound -> "المنتج غير موجود"
        is AppError.Network -> "تعذّر الاتصال: ${error.reason}"
        is AppError.Unknown -> "خطأ غير متوقع، أعد المحاولة"
    }

    companion object {
        /** إجراء تعديل السعر — يستخدمه المخزن في طلب الصلاحية والمدير في الاعتماد. */
        const val ACTION_EDIT_PRICE: String = "EDIT_PRICE"
    }
}