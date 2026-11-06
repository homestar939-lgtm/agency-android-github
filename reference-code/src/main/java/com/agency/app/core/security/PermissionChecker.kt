// core/security/PermissionChecker.kt
package com.agency.app.core.security

import com.agency.app.core.ui.theme.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionChecker @Inject constructor() {

    fun canEditPrice(role: UserRole, hasExplicitPermission: Boolean): Boolean {
        return when (role) {
            UserRole.ADMIN -> true
            UserRole.WAREHOUSE -> hasExplicitPermission
            else -> false
        }
    }

    fun canDeleteProduct(role: UserRole, hasFirstInvoice: Boolean): Boolean {
        return when (role) {
            UserRole.ADMIN -> !hasFirstInvoice // لا يمكن الحذف بعد أول فاتورة
            else -> false
        }
    }

    fun canApproveFinancialEdit(role: UserRole): Boolean {
        return role == UserRole.ADMIN
    }

    fun canViewAuditLog(role: UserRole): Boolean {
        return role == UserRole.ADMIN
    }

    fun canCreateReceipt(role: UserRole): Boolean {
        return role in listOf(UserRole.ADMIN, UserRole.CASHIER)
    }

    fun getLockedFields(role: UserRole): List<String> {
        return when (role) {
            UserRole.WAREHOUSE -> listOf("price", "currency") // الحقول المالية مقفلة
            UserRole.CASHIER -> listOf("invoice_amount") // لا يمكن تعديل الفواتير
            else -> emptyList()
        }
    }
}
