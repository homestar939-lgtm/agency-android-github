package com.agency.app.core.domain.model

/**
 * أدوار المستخدمين في نظام الوكالة.
 * كل دور له هوية بصرية وشاشات ومجموعة صلاحيات مستقلة.
 */
enum class UserRole {
    ADMIN,
    WAREHOUSE,
    BUYER,
    CASHIER,
    ACCOUNTANT;

    /** هل يملك هذا الدور حق الكتابة على البيانات المالية؟ */
    val canWriteFinancials: Boolean
        get() = this == ADMIN || this == WAREHOUSE || this == CASHIER

    /** المحاسب للقراءة فقط دائماً، وأي تعديل يمر عبر طلب صلاحية. */
    val isReadOnly: Boolean
        get() = this == ACCOUNTANT
}