package com.agency.app.core.domain.model

import kotlinx.datetime.Instant

/**
 * المشتري (العميل). ذمته هي مجموع الفواتير المعتمدة ناقص مجموع سندات القبض.
 * الرصيد الموجب يعني أنه مدين للوكالة.
 */
data class Buyer(
    val id: String,
    val name: String,
    val phone: String,
    val address: String? = null,
    val shopName: String? = null,
    val creditLimit: Money = Money.ZERO,
    val isActive: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant
)

/** صف في كشف حساب المشتري. */
data class BuyerStatementEntry(
    val id: String,
    val buyerId: String,
    val description: String,
    val debit: Money,
    val credit: Money,
    val balanceAfter: Money,
    val at: Instant,
    val referenceId: String? = null
)

/** ملخّص مالي لمشترٍ واحد — يستخدمه المحاسب وشاشة التحصيل. */
data class BuyerBalance(
    val buyerId: String,
    val buyerName: String,
    val totalInvoiced: Money,
    val totalPaid: Money,
    val creditLimit: Money
) {
    val outstanding: Money get() = totalInvoiced - totalPaid
    val isSettled: Boolean get() = outstanding.isZero
    val isOverLimit: Boolean get() = creditLimit.isPositive && outstanding > creditLimit
    val collectionRate: Int
        get() = if (totalInvoiced.isZero) 100
        else ((totalPaid.cents * 100L) / totalInvoiced.cents).toInt()
}
