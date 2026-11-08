package com.agency.app.feature_warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.domain.model.Order
import com.agency.app.core.domain.model.OrderStatus
import com.agency.app.core.domain.model.Product
import com.agency.app.core.domain.repository.OrderRepository
import com.agency.app.core.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** نتيجة قراءة باركود. */
sealed interface ScanResult {
    data object Idle : ScanResult
    data class ProductFound(val product: Product) : ScanResult
    data class OrderFound(val order: Order) : ScanResult
    data class NotFound(val raw: String) : ScanResult
}

data class ScanUiState(
    val manualCode: String = "",
    val result: ScanResult = ScanResult.Idle,
    val recentScans: List<String> = emptyList(),
    val ordersAwaitingPick: List<Order> = emptyList(),
)

/**
 * المسح بالباركود في المخزن.
 *
 * الرمز الممسوح يُطابق أولاً على `sku` المنتج ثم على `orderNumber` الطلبية،
 * لأن ملصق المخزن قد يكون على الكرتون (صنف) أو على ورقة الأمر (طلبية).
 *
 * ولا يعدّل هذا الـViewModel أي مخزون: القراءة تُظهر فقط، والخصم يبقى حصراً
 * في `createOrder` و`transition`. لو خصم المسحُ المخزونَ لصار طريقاً خفياً
 * لتغيير الأرصدة بلا فاتورة ولا خط زمني.
 *
 * [submitCode] تستقبل ما يقرأه الماسح وما يكتبه الموظف يدوياً بنفس المسار،
 * لأن الكاميرا تفشل غالباً على ملصق ممزّق، والبديل اليدوي ليس مساراً ثانياً
 * بل نفس المنطق بمدخل آخر.
 */
@HiltViewModel
class WarehouseScanViewModel @Inject constructor(
    productRepository: ProductRepository,
    orderRepository: OrderRepository,
) : ViewModel() {

    private val products = productRepository.observeAllProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val orders = orderRepository.observeOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _manualCode = MutableStateFlow("")
    private val _result = MutableStateFlow<ScanResult>(ScanResult.Idle)
    private val _recent = MutableStateFlow<List<String>>(emptyList())

    val ui: StateFlow<ScanUiState> = combine(
        products, orders, _manualCode, _result, _recent
    ) { rows, orderRows, code, result, recent ->
        ScanUiState(
            manualCode = code,
            result = result,
            recentScans = recent,
            ordersAwaitingPick = orderRows.filter {
                it.status == OrderStatus.NEW || it.status == OrderStatus.PREPARING
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScanUiState())

    fun onManualCodeChange(value: String) {
        _manualCode.value = value
    }

    /**
     * يبحث عن الرمز في المنتجات ثم الطلبيات.
     *
     * البحث بحالة أحرف موحّدة لأن ملصقات المخزن تُطبع بأحرف كبيرة غالباً
     * بينما `sku` المخزَّن قد يكون مختلطاً؛ اختلاف الحالة ليس خطأ مستخدم
     * بل اختلاف طباعة، ومعاملته كخطأ تُنتج رفضاً محيّراً.
     */
    fun submitCode(raw: String) {
        val code = raw.trim()
        _manualCode.value = code
        if (code.isBlank()) return

        val normalized = code.lowercase()
        val product = products.value.firstOrNull {
            it.sku.lowercase() == normalized || it.id == code
        }
        if (product != null) {
            _result.value = ScanResult.ProductFound(product)
            remember(code)
            return
        }
        val order = orders.value.firstOrNull {
            it.orderNumber.lowercase() == normalized || it.id == code
        }
        if (order != null) {
            _result.value = ScanResult.OrderFound(order)
            remember(code)
            return
        }
        _result.value = ScanResult.NotFound(code)
    }

    fun clear() {
        _result.value = ScanResult.Idle
        _manualCode.value = ""
    }

    /** يحتفظ بآخر عشر قراءات فقط — السجل أداة تتبّع لا أرشيف. */
    private fun remember(code: String) {
        _recent.value = (listOf(code) + _recent.value).distinct().take(MAX_RECENT)
    }

    private companion object {
        const val MAX_RECENT = 10
    }
}