package com.agency.app.core.domain.model

import kotlinx.datetime.Instant

/** طريقة الدفع المقبولة في سند القبض. */
enum class PaymentMethod {
    CASH,
    TRANSFER,
    CHECK
}

/**
 * سند قبض — كل سند يخصم تلقائياً من ذمة المشتري.
 * [amount] دائماً موجب، والخصم يُنفَّذ بمنطق واحد في طبقة البيانات.
 */
data class Receipt(
    val id: String,
    val receiptNumber: String,
    val buyerId: String,
    val buyerName: String,
    val amount: Money,
    val method: PaymentMethod,
    val invoiceId: String? = null,
    val reference: String? = null,
    val note: String? = null,
    val collectorUserId: String,
    val collectorName: String,
    val collectedAt: Instant,
    val isSigned: Boolean = false,
    val signaturePath: String? = null,
    val isSynced: Boolean = false,
    val createdAt: Instant
) {
    /** السندات التي تتجاوز مليون وحدة تحتاج تحققاً حيوياً قبل الحفظ. */
    val requiresBiometric: Boolean get() = amount >= BIOMETRIC_THRESHOLD

    companion object {
        val BIOMETRIC_THRESHOLD = Money(100_000_000L) // 1,000,000.00
    }
}
