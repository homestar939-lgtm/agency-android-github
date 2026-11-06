// tests/integration/ClosedLoopAccountingTest.kt
package com.agency.app.tests.integration

import org.junit.Test
import org.junit.Assert.*

class ClosedLoopAccountingTest {

    @Test
    fun `test receipt auto-deducts from buyer debt`() {
        val cashierSession = TestSession.login("cashier@agency.com", "Cash@2026$")
        val buyerSession = TestSession.login("buyer@client.com", "Buyer@2026&")

        // ذمة المشتري الأولية
        val initialDebt = buyerSession.getBuyerDebt("buyer_001")
        assertEquals(100000.0, initialDebt)

        // إنشاء سند قبض
        val receipt = cashierSession.createReceipt(
            buyerId = "buyer_001",
            amount = 30000.0,
            paymentMethod = "CASH"
        )

        // التحقق: خصم تلقائي
        val newDebt = buyerSession.getBuyerDebt("buyer_001")
        assertEquals(70000.0, newDebt)

        // التحقق: الأرشيف يتحدث
        val archive = buyerSession.getArchive()
        assertTrue(archive.any { it.type == TransactionType.CREDIT && it.amount == 30000.0 })
    }

    @Test
    fun `test commission calculation is accurate`() {
        val cashierSession = TestSession.login("cashier@agency.com", "Cash@2026$")
        val adminSession = TestSession.login("admin@agency.com", "Admin@2026#")

        // تعيين نسبة العمولة
        adminSession.setCashierCommission("cashier_user", 2.5)

        // إنشاء سندات
        cashierSession.createReceipt("buyer_001", 50000.0, "CASH")
        cashierSession.createReceipt("buyer_002", 30000.0, "CASH")
        cashierSession.createReceipt("buyer_003", 20000.0, "CASH")

        // التحقق: حساب العمولة
        val totalCollected = 100000.0
        val expectedCommission = totalCollected * 0.025
        val actualCommission = cashierSession.getMyCommission()

        assertEquals(expectedCommission, actualCommission, 0.01)
    }

    @Test
    fun `test financial balance is consistent`() {
        val adminSession = TestSession.login("admin@agency.com", "Admin@2026#")
        val accountantSession = TestSession.login("accountant@agency.com", "Acc@2026%")

        // إنشاء فواتير وسندات
        val invoices = listOf(
            adminSession.createInvoice("buyer_001", 50000.0),
            adminSession.createInvoice("buyer_002", 30000.0)
        )

        val receipts = listOf(
            adminSession.createReceipt("buyer_001", 20000.0),
            adminSession.createReceipt("buyer_002", 15000.0)
        )

        // التحقق: المعادلة المالية
        val totalInvoices = invoices.sumOf { it.amount }
        val totalReceipts = receipts.sumOf { it.amount }
        val totalDebts = accountantSession.getTotalDebts()

        assertEquals(totalInvoices - totalReceipts, totalDebts, 0.01)
    }
}
