package com.agency.app.feature_buyer

import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.Product
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * سلة المشتري.
 *
 * سلة حيّة في الذاكرة خلال الجلسة فقط: لا تُكتب في Room لأنها ليست مستنداً
 * محاسبياً، ولا يُنشأ لها سجل إلا عند تأكيد الطلبية عبر
 * [com.agency.app.core.domain.repository.OrderRepository.createOrder].
 *
 * الأسعار تُجمَّد هنا وقت الإضافة (snapshot) عن قصد: لو غيّر المخزن السعر
 * بعد إضافة المنتج للسلة، يرى المشتري السعر الذي وافق عليه لحظة الإضافة.
 */
data class CartLine(
    val productId: String,
    val name: String,
    val unit: String,
    val unitPrice: Money,
    val quantity: Int,
    val minOrderQuantity: Int,
    val availableStock: Int,
    val showPrice: Boolean = true,
    val showUnit: Boolean = true,
) {
    val gross: Money get() = unitPrice * quantity

    /** هل بلغت الكمية الحد الأدنى الذي يفرضه المنتج؟ */
    val meetsMinimum: Boolean get() = quantity >= minOrderQuantity

    /** هل تجاوزت الكمية المتاح فعلياً في المخزن؟ */
    val exceedsStock: Boolean get() = quantity > availableStock

    val isValid: Boolean get() = meetsMinimum && !exceedsStock
}

@Singleton
class CartStore @Inject constructor() {

    private val _lines = MutableStateFlow<List<CartLine>>(emptyList())
    val lines: StateFlow<List<CartLine>> = _lines.asStateFlow()

    /** يضيف منتجاً أو يزيد كمية سطر قائم، دون تجاوز المخزون. */
    fun add(product: Product, quantity: Int) {
        if (quantity <= 0) return
        val current = _lines.value
        val existing = current.firstOrNull { it.productId == product.id }
        _lines.value = if (existing == null) {
            current + CartLine(
                productId = product.id,
                name = product.name,
                unit = product.unit,
                unitPrice = product.price,
                quantity = quantity.coerceAtMost(product.stockQuantity),
                minOrderQuantity = product.minOrderQuantity,
                availableStock = product.stockQuantity,
                showPrice = product.visibility.showPrice,
                showUnit = product.visibility.showUnit,
            )
        } else {
            current.map { line ->
                if (line.productId == product.id) {
                    line.copy(quantity = (line.quantity + quantity).coerceAtMost(line.availableStock))
                } else {
                    line
                }
            }
        }
    }

    /** يضبط كمية سطر صراحةً؛ الصفر يعني الحذف. */
    fun setQuantity(productId: String, quantity: Int) {
        _lines.value = if (quantity <= 0) {
            _lines.value.filterNot { it.productId == productId }
        } else {
            _lines.value.map { line ->
                if (line.productId == productId) {
                    line.copy(quantity = quantity.coerceAtMost(line.availableStock))
                } else {
                    line
                }
            }
        }
    }

    fun remove(productId: String) {
        _lines.value = _lines.value.filterNot { it.productId == productId }
    }

    /** يُفرَّغ بعد نجاح إنشاء الطلبية فقط. */
    fun clear() {
        _lines.value = emptyList()
    }

    fun quantityOf(productId: String): Int =
        _lines.value.firstOrNull { it.productId == productId }?.quantity ?: 0

    companion object {
        /** إجمالي السلة — جمع دقيق بالسنت، بلا أي حساب عائم. */
        fun total(lines: List<CartLine>): Money =
            lines.fold(Money.ZERO) { acc, line -> acc + line.gross }

        fun itemCount(lines: List<CartLine>): Int = lines.sumOf { it.quantity }

        /** لا يُسمح بالتأكيد ما دام سطر واحد مخالفاً للحد الأدنى أو للمخزون. */
        fun canCheckout(lines: List<CartLine>): Boolean =
            lines.isNotEmpty() && lines.all { it.isValid }
    }
}
