// tests/integration/PermissionsTest.kt
package com.agency.app.tests.integration

import org.junit.Test
import org.junit.Assert.*

class PermissionsTest {

    @Test
    fun `test warehouse cannot edit prices without permission`() {
        val warehouseSession = TestSession.login("warehouse@agency.com", "Store#2026!")
        
        // محاولة تعديل السعر بدون تصريح
        val result = warehouseSession.tryEditPrice("product_001", newPrice = 2000.0)
        
        // التحقق: العملية مرفوضة
        assertFalse(result.success)
        assertEquals("Permission denied", result.error)
    }

    @Test
    fun `test accountant cannot edit documents without approval`() {
        val accountantSession = TestSession.login("accountant@agency.com", "Acc@2026%")
        
        // محاولة تعديل فاتورة
        val result = accountantSession.tryEditInvoice("invoice_001", newAmount = 50000.0)
        
        // التحقق: العملية مرفوضة
        assertFalse(result.success)
        assertEquals("View-only access", result.error)
    }

    @Test
    fun `test accountant can request permission and get approval`() {
        val accountantSession = TestSession.login("accountant@agency.com", "Acc@2026%")
        val adminSession = TestSession.login("admin@agency.com", "Admin@2026#")

        // المحاسب يطلب تصريح
        val request = accountantSession.requestPermission(
            documentId = "invoice_001",
            editType = "AMOUNT",
            reason = "خطأ في الحساب"
        )

        // التحقق: الطلب وصل للمدير
        assert(adminSession.hasPendingPermissionRequest(request.id))

        // المدير يوافق
        adminSession.approvePermissionRequest(request.id)

        // التحقق: المحاسب يمكنه التعديل الآن
        val result = accountantSession.tryEditInvoice("invoice_001", newAmount = 50000.0)
        assertTrue(result.success)
    }

    @Test
    fun `test warehouse cannot delete product after first invoice`() {
        val warehouseSession = TestSession.login("warehouse@agency.com", "Store#2026!")
        
        // منتج له فاتورة سابقة
        val result = warehouseSession.tryDeleteProduct("product_with_invoice")
        
        // التحقق: الحذف مرفوض
        assertFalse(result.success)
        assertEquals("Cannot delete product with existing invoices", result.error)
    }

    @Test
    fun `test biometric authentication for sensitive operations`() {
        val cashierSession = TestSession.login("cashier@agency.com", "Cash@2026$")
        
        // محاولة إنشاء سند كبير يتطلب بصمة
        val result = cashierSession.createLargeReceipt(
            buyerId = "buyer_001",
            amount = 1000000.0,
            biometricVerified = false
        )
        
        // التحقق: العملية مرفوضة بدون بصمة
        assertFalse(result.success)
        assertEquals("Biometric authentication required", result.error)

        // مع البصمة
        val resultWithBiometric = cashierSession.createLargeReceipt(
            buyerId = "buyer_001",
            amount = 1000000.0,
            biometricVerified = true
        )
        
        assertTrue(resultWithBiometric.success)
    }
}
