package com.agency.app.core.domain.model

import kotlinx.datetime.Instant

/**
 * دورة حياة الطلبية. الترتيب هنا هو الترتيب المنطقي للانتقالات،
 * ولا يُسمح بأي انتقال خارج هذا التسلسل (إلا الإلغاء قبل التسليم).
 */
enum class OrderStatus {
    NEW,
    PREPARING,
    PREPARED,
    SENT,
    DELIVERED,
    CANCELLED;

    /** الحالات التي يمكن الانتقال إليها من الحالة الحالية. */
    val allowedNext: Set<OrderStatus>
        get() = when (this) {
            NEW -> setOf(PREPARING, CANCELLED)
            PREPARING -> setOf(PREPARED, CANCELLED)
            PREPARED -> setOf(SENT, CANCELLED)
            SENT -> setOf(DELIVERED)
            DELIVERED -> emptySet()
            CANCELLED -> emptySet()
        }

    val isFinal: Boolean get() = this == DELIVERED || this == CANCELLED

    /** هل الطلبية في مرحلة يمكن فيها إنشاء فاتورة؟ */
    val isInvoiceable: Boolean get() = this == PREPARED || this == SENT
}

fun OrderStatus.canTransitionTo(target: OrderStatus): Boolean = target in allowedNext

/** سطر واحد داخل الطلبية. */
data class OrderItem(
    val id: String,
    val orderId: String,
    val productId: String,
    val productName: String,
    val unit: String,
    val unitPrice: Money,
    val quantity: Int,
    val discount: Money = Money.ZERO
) {
    val gross: Money get() = unitPrice * quantity
    val net: Money get() = gross - discount
}

/** حدث في الخط الزمني للطلبية — يُستخدم في شاشة تتبع الطلب. */
data class OrderTimeline(
    val status: OrderStatus,
    val at: Instant,
    val actorId: String?,
    val actorName: String,
    val note: String? = null
)

/** الطلبية نفسها، مع كل أسطرها وخطها الزمني. */
data class Order(
    val id: String,
    val orderNumber: String,
    val buyerId: String,
    val buyerName: String,
    val status: OrderStatus,
    val items: List<OrderItem>,
    val timeline: List<OrderTimeline> = emptyList(),
    val note: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val itemsCount: Int get() = items.sumOf { it.quantity }

    val subtotal: Money
        get() = items.fold(Money.ZERO) { acc, item -> acc + item.gross }

    val discountTotal: Money
        get() = items.fold(Money.ZERO) { acc, item -> acc + item.discount }

    val total: Money get() = subtotal - discountTotal

    val isCancellable: Boolean get() = OrderStatus.CANCELLED in status.allowedNext

    val currentStepIndex: Int
        get() = when (status) {
            OrderStatus.NEW -> 0
            OrderStatus.PREPARING -> 1
            OrderStatus.PREPARED -> 2
            OrderStatus.SENT -> 3
            OrderStatus.DELIVERED -> 4
            OrderStatus.CANCELLED -> -1
        }
}
