package com.agency.app.core.domain.model

import kotlinx.datetime.Instant

/**
 * مستخدم النظام. يُنشأ حساب المشتري من لوحة المدير فقط،
 * ولا يمكن لأي مستخدم إنشاء حساب لنفسه.
 */
data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val role: UserRole,
    val phone: String? = null,
    val isActive: Boolean = true,
    val buyerId: String? = null,
    val createdAt: Instant,
    val lastLoginAt: Instant? = null
) {
    val requiresBuyerLink: Boolean
        get() = role == UserRole.BUYER && buyerId == null
}