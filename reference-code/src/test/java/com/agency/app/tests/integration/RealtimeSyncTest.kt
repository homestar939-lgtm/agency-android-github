// tests/integration/RealtimeSyncTest.kt
package com.agency.app.tests.integration

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import org.junit.Test

class RealtimeSyncTest {

    @Test
    fun `test instant sync across all roles`() = runTest {
        val sessions = listOf(
            TestSession.login("admin@agency.com", "Admin@2026#"),
            TestSession.login("warehouse@agency.com", "Store#2026!"),
            TestSession.login("cashier@agency.com", "Cash@2026$"),
            TestSession.login("accountant@agency.com", "Acc@2026%")
        )

        // إنشاء سند قبض من المتحصل
        val cashierSession = sessions[2]
        val receipt = cashierSession.createReceipt(
            buyerId = "buyer_001",
            amount = 50000.0,
            paymentMethod = "CASH"
        )

        // انتظار 500 مللي ثانية للمزامنة
        delay(500)

        // التحقق: جميع الجلسات استلمت التحديث
        sessions.forEach { session ->
            assert(session.hasNewReceipt(receipt.id)) {
                "Session ${session.role} did not receive receipt update"
            }
        }

        // التحقق: زمن الاستجابة أقل من ثانية
        val syncTime = sessions.maxOf { it.getLastSyncTime() }
        assert(syncTime < 1000) {
            "Sync took too long: ${syncTime}ms"
        }
    }

    @Test
    fun `test offline mode and reconnection`() = runTest {
        val cashierSession = TestSession.login("cashier@agency.com", "Cash@2026$")
        
        // قطع الاتصال
        cashierSession.disconnect()

        // إنشاء سند في وضع Offline
        val offlineReceipt = cashierSession.createReceiptOffline(
            buyerId = "buyer_001",
            amount = 30000.0
        )

        // التحقق: السند محفوظ محلياً
        assert(cashierSession.hasLocalReceipt(offlineReceipt.id))

        // إعادة الاتصال
        cashierSession.reconnect()

        // انتظار المزامنة التلقائية
        delay(2000)

        // التحقق: السند وصل للخادم
        val adminSession = TestSession.login("admin@agency.com", "Admin@2026#")
        assert(adminSession.hasNewReceipt(offlineReceipt.id))
    }
}
