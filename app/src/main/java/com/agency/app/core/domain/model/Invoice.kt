package com.agency.app.core.domain.model

import kotlinx.datetime.Instant

/** حالة الفاتورة. لا تُرسل للمشتري قبل اعتمادها. */
enum class InvoiceStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    ARCHIVED
}

/** سطر الفاتورة — نسخة مجمّدة من سطر الطلبية وقت الفوترة. */
data class InvoiceItem(
    val id: String,
    val invoiceId: String,
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

/**
 * الفاتورة. مرتبطة بطلبية واحدة، وتحمل نسخة ثابتة من الأسعار،
 * بحيث لا يتغير مبلغ الفاتورة لو تغيّر سعر المنتج لاحقاً.
 */
data class Invoice(
    val id: String,
    val invoiceNumber: String,
    val orderId: String,
    val buyerId: String,
    val buyerName: String,
    val status: InvoiceStatus,
    val items: List<InvoiceItem>,
    val commission: Money = Money.ZERO,
    val note: String? = null,
    val createdByUserId: String,
    val approvedByUserId: String? = null,
    val approvedAt: Instant? = null,
    val rejectionReason: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    val subtotal: Money get() = items.fold(Money.ZERO) { acc, item -> acc + item.gross }
    val discountTotal: Money get() = items.fold(Money.ZERO) { acc, item -> acc + item.discount }

    /** الإجمالي قبل العمولة. */
    val total: Money get() = subtotal - discountTotal

    /** الإجمالي النهائي المستحق على المشتري. */
    val grandTotal: Money get() = total + commission

    val isEditable: Boolean get() = status == InvoiceStatus.DRAFT || status == InvoiceStatus.REJECTED

    val isApproved: Boolean get() = status == InvoiceStatus.APPROVED

    val canDelete: Boolean get() = status == InvoiceStatus.DRAFT
}
