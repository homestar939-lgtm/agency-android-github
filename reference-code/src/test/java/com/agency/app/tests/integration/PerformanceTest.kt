// tests/integration/PerformanceTest.kt
package com.agency.app.tests.integration

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import org.junit.Test
import kotlin.system.measureTimeMillis

class PerformanceTest {

    @Test
    fun `test sync latency under 1 second`() = runTest {
        val sessions = createMultipleSessions(count = 10)
        
        val latency = measureTimeMillis {
            sessions[0].createReceipt("buyer_001", 10000.0, "CASH")
            delay(500)
        }

        assert(latency < 1000) {
            "Sync latency too high: ${latency}ms"
        }
    }

    @Test
    fun `test app handles 1000 concurrent orders`() = runTest {
        val buyerSessions = createBuyerSessions(count = 1000)
        
        val time = measureTimeMillis {
            buyerSessions.forEach { session ->
                session.createOrder(
                    agencyId = "agency_001",
                    items = listOf(OrderItem("product_001", 1, "كغ"))
                )
            }
        }

        // التحقق: جميع الطلبات عالقة في أقل من 10 ثوانٍ
        assert(time < 10000) {
            "Creating 1000 orders took too long: ${time}ms"
        }

        // التحقق: المخزن استلم جميع الطلبات
        val warehouseSession = TestSession.login("warehouse@agency.com", "Store#2026!")
        assertEquals(1000, warehouseSession.getNewOrdersCount())
    }

    @Test
    fun `test offline mode preserves data`() = runTest {
        val cashierSession = TestSession.login("cashier@agency.com", "Cash@2026$")
        
        // قطع الاتصال
        cashierSession.disconnect()

        // إنشاء 10 سندات في وضع Offline
        repeat(10) { index ->
            cashierSession.createReceiptOffline(
                buyerId = "buyer_$index",
                amount = (index + 1) * 1000.0
            )
        }

        // التحقق: جميع السندات محفوظة محلياً
        assertEquals(10, cashierSession.getLocalReceiptsCount())

        // إعادة الاتصال
        cashierSession.reconnect()
        delay(3000)

        // التحقق: جميع السندات متزامنة
        val adminSession = TestSession.login("admin@agency.com", "Admin@2026#")
        assertEquals(10, adminSession.getRecentReceiptsCount())
    }

    @Test
    fun `test memory usage stays under 200MB`() {
        val runtime = Runtime.getRuntime()
        
        // تحميل 10000 منتج
        val warehouseSession = TestSession.login("warehouse@agency.com", "Store#2026!")
        warehouseSession.loadProducts(count = 10000)

        // التحقق: الذاكرة المستخدمة
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        assert(usedMemory < 200) {
            "Memory usage too high: ${usedMemory}MB"
        }
    }
}
