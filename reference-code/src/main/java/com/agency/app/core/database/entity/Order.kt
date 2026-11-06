// core/database/entity/Order.kt
package com.agency.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey
    val id: String,
    val buyerId: String,
    val buyerName: String,
    val status: OrderStatus,
    val items: List<OrderItem>,
    val totalAmount: Double,
    val currency: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val preparedAt: Instant? = null,
    val sentAt: Instant? = null,
    val deliveredAt: Instant? = null,
    val deliveryContact: String? = null,
    val notes: String? = null
)

enum class OrderStatus {
    NEW,           // جديد
    PREPARING,     // قيد التحضير
    PREPARED,      // تم التحضير
    SENT,          // تم الإرسال
    DELIVERED,     // تم التسليم
    CANCELLED      // ملغي
}

data class OrderItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unit: String,
    val unitPrice: Double,
    val totalPrice: Double
)
