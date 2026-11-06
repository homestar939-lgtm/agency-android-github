// tests/integration/OrderFlowIntegrationTest.kt
package com.agency.app.tests.integration

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Before

class OrderFlowIntegrationTest {

    private lateinit var adminSession: TestSession
    private lateinit var warehouseSession: TestSession
    private lateinit var buyerSession: TestSession
    private lateinit var cashierSession: TestSession
    private lateinit var accountantSession: TestSession

    @Before
    fun setup() {
        // تهيئة جلسات جميع المستخدمين
        adminSession = TestSession.login("admin@agency.com", "Admin@2026#")
        warehouseSession = TestSession.login("warehouse@agency.com", "Store#2026!")
        buyerSession = TestSession.login("buyer@client.com", "Buyer@2026&")
        cashierSession = TestSession.login("cashier@agency.com", "Cash@2026$")
        accountantSession = TestSession.login("accountant@agency.com", "Acc@2026%")
    }

    @Test
    fun `test complete order flow from buyer to delivery`() = runTest {
        // الخطوة 1: المشتري يطلب منتجات
        val order = buyerSession.createOrder(
            agencyId = "agency_001",
            items = listOf(
                OrderItem("product_001", quantity = 5, unit = "كغ"),
                OrderItem("product_002", quantity = 3, unit = "لتر")
            )
        )

        // التحقق: الطلبية وصلت للمخزن والمدير
        assert(warehouseSession.hasNewOrder(order.id))
        assert(adminSession.hasNewOrder(order.id))

        // الخطوة 2: المخزن يبدأ التحضير
        warehouseSession.startPreparation(order.id)
        
        // التحقق: الحالة تغيرت عند الجميع
        assert(buyerSession.getOrderStatus(order.id) == OrderStatus.PREPARING)
        assert(adminSession.getOrderStatus(order.id) == OrderStatus.PREPARING)

        // الخطوة 3: المخزن يكمل التحضير
        warehouseSession.completePreparation(order.id)

        // التحقق: Hero Animation عند المشتري
        assert(buyerSession.getOrderStatus(order.id) == OrderStatus.PREPARED)
        assert(buyerSession.receivedHeroAnimation("order_prepared"))

        // الخطوة 4: المخزن ينشئ الفاتورة
        val invoice = warehouseSession.createInvoice(order.id)

        // التحقق: الفاتورة وصلت للمحاسب
        assert(accountantSession.hasNewInvoice(invoice.id))
        assert(accountantSession.getInvoiceAmount(invoice.id) == invoice.totalAmount)

        // الخطوة 5: المخزن يرسل الطلبية للوصيل
        warehouseSession.dispatchOrder(
            orderId = order.id,
            contactNumber = "07701234567",
            deliveryMethod = "DIRECT"
        )

        // التحقق: حالة الإرسال عند المشتري
        assert(buyerSession.getOrderStatus(order.id) == OrderStatus.SENT)
        assert(buyerSession.getDeliveryContact(order.id) == "07701234567")

        // الخطوة 6: المخزن يؤكد التسليم
        warehouseSession.confirmDelivery(order.id)

        // التحقق: التسليم عند الجميع
        assert(buyerSession.getOrderStatus(order.id) == OrderStatus.DELIVERED)
        assert(adminSession.getOrderStatus(order.id) == OrderStatus.DELIVERED)
        assert(cashierSession.hasBuyerWithDebt(order.buyerId))

        // الخطوة 7: المتحصل يستلم المبلغ
        val receipt = cashierSession.createReceipt(
            buyerId = order.buyerId,
            amount = invoice.totalAmount,
            paymentMethod = "CASH"
        )

        // التحقق: سند القبض وصل للمحاسب والمدير
        assert(accountantSession.hasNewReceipt(receipt.id))
        assert(adminSession.hasNewReceipt(receipt.id))

        // التحقق: خصم تلقائي من ذمة المشتري
        assert(buyerSession.getBuyerDebt(order.buyerId) == 0.0)

        // التحقق: عمولة المتحصل
        assert(cashierSession.getMyCommission() > 0.0)

        // التحقق: سجل التدقيق عند المدير
        assert(adminSession.auditLogContains("order_created", order.id))
        assert(adminSession.auditLogContains("order_prepared", order.id))
        assert(adminSession.auditLogContains("order_sent", order.id))
        assert(adminSession.auditLogContains("order_delivered", order.id))
        assert(adminSession.auditLogContains("receipt_created", receipt.id))
    }
}
