package com.agency.app.core.ui

import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.domain.model.OrderStatus
import com.agency.app.core.domain.model.PaymentMethod
import com.agency.app.core.domain.model.PermissionRequestStatus
import com.agency.app.core.domain.model.UserRole

/**
 * تسميات عربية مشتركة للحالات.
 *
 * مكان واحد لكل وحدة: كانت التسميات تُكتب داخل ملف الشاشة الذي يحتاجها،
 * فاحتاجت شاشة المخزن نسخة من تسمية الفاتورة لأنها في ملف وحدة المشتري
 * و`internal`. النتيجة المحتومة أن تتباعد النسخ: «مرفوضة» في شاشة و«مرفوض»
 * في أخرى، لنفس الحالة. الآن المصدر واحد.
 */
fun InvoiceStatus.arabic(): String = when (this) {
    InvoiceStatus.DRAFT -> "مسودّة"
    InvoiceStatus.PENDING_APPROVAL -> "بانتظار الاعتماد"
    InvoiceStatus.APPROVED -> "معتمدة"
    InvoiceStatus.REJECTED -> "مرفوضة"
    InvoiceStatus.ARCHIVED -> "مؤرشفة"
}

fun OrderStatus.arabic(): String = when (this) {
    OrderStatus.NEW -> "قيد التحضير"
    OrderStatus.PREPARING -> "تحت التحضير"
    OrderStatus.PREPARED -> "جاهزة"
    OrderStatus.SENT -> "مرسلة"
    OrderStatus.DELIVERED -> "مسلَّمة"
    OrderStatus.CANCELLED -> "ملغاة"
}

fun PaymentMethod.arabic(): String = when (this) {
    PaymentMethod.CASH -> "نقداً"
    PaymentMethod.TRANSFER -> "حوالة"
    PaymentMethod.CHECK -> "شيك"
}

fun PermissionRequestStatus.arabic(): String = when (this) {
    PermissionRequestStatus.PENDING -> "بانتظار المراجعة"
    PermissionRequestStatus.APPROVED -> "معتمد"
    PermissionRequestStatus.REJECTED -> "مرفوض"
}

fun UserRole.arabic(): String = when (this) {
    UserRole.ADMIN -> "مدير"
    UserRole.WAREHOUSE -> "مخزن"
    UserRole.CASHIER -> "متحصّل"
    UserRole.ACCOUNTANT -> "محاسب"
    UserRole.BUYER -> "مشتري"
}