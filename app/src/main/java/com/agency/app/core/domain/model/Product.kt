package com.agency.app.core.domain.model

import kotlinx.datetime.Instant

/** إعدادات ظهور المنتج للمشتري، يتحكم بها المدير لكل منتج على حدة. */
data class ProductVisibility(
    val showPrice: Boolean = true,
    val showUnit: Boolean = true,
    val showExpiry: Boolean = true,
    val showMinOrder: Boolean = true,
    val showStock: Boolean = false
) {
    companion object {
        val FULL = ProductVisibility()
    }
}

/** منتج في كتالوج الوكالة. */
data class Product(
    val id: String,
    val name: String,
    val sku: String,
    val unit: String,
    val price: Money,
    val cost: Money = Money.ZERO,
    val stockQuantity: Int = 0,
    val minOrderQuantity: Int = 1,
    val expiryDate: Instant? = null,
    val categoryId: String? = null,
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val visibility: ProductVisibility = ProductVisibility.FULL,
    val updatedAt: Instant
) {
    val isOutOfStock: Boolean get() = stockQuantity <= 0
    val isLowStock: Boolean get() = stockQuantity in 1..9
    val stockValue: Money get() = price * stockQuantity
    val margin: Money get() = price - cost

    /** هل يمكن للمشتري طلب هذه الكمية؟ */
    fun canOrder(quantity: Int): Boolean =
        isActive && !isOutOfStock && quantity >= minOrderQuantity && quantity <= stockQuantity
}
