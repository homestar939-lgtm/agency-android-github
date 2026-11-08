package com.agency.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.agency.app.core.data.local.AppDatabase
import com.agency.app.core.data.repository.BuyerRepositoryImpl
import com.agency.app.core.data.repository.InvoiceRepositoryImpl
import com.agency.app.core.data.repository.OrderRepositoryImpl
import com.agency.app.core.data.repository.ProductRepositoryImpl
import com.agency.app.core.data.repository.ReceiptRepositoryImpl
import com.agency.app.core.data.repository.UserRepositoryImpl
import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.repository.NewOrderLine
import com.agency.app.core.domain.model.OrderStatus
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.PaymentMethod
import com.agency.app.core.domain.model.Product
import com.agency.app.core.domain.model.UserRole
import com.agency.app.core.util.Ids
import com.agency.app.core.util.TimeSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * اختبار التكامل للدورة الكاملة — من الطلبية إلى إغلاق الذمّة.
 *
 * هذا الاختبار لا يستخدم mocks: يبني قاعدة Room حقيقية في الذاكرة ويمرّر
 * إليها مستودعات حقيقية، ثم ينفّذ السلسلة التي يقوم عليها النظام فعلياً:
 *
 * طلبية ← خصم مخزون ← فاتورة ← اعتماد ← سند قبض ← ذمّة صفر.
 *
 * سبب وجوده: كل قاعدة مالية في هذا التطبيق تعيش في طبقة البيانات، واختبار
 * الواجهة لا يثبتها. لو انكسر خصم المخزون أو رصيد الذمّة، سيكشفه هذا الملف
 * قبل أن يصل إلى جهاز مستخدم.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OrderFlowIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var buyerRepository: BuyerRepositoryImpl
    private lateinit var productRepository: ProductRepositoryImpl
    private lateinit var orderRepository: OrderRepositoryImpl
    private lateinit var invoiceRepository: InvoiceRepositoryImpl
    private lateinit var receiptRepository: ReceiptRepositoryImpl

    private lateinit var adminId: String

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        userRepository = UserRepositoryImpl(db.userDao())
        buyerRepository = BuyerRepositoryImpl(db.buyerDao(), db.invoiceDao(), db.receiptDao())
        productRepository = ProductRepositoryImpl(db.productDao())
        orderRepository = OrderRepositoryImpl(db, db.orderDao(), db.productDao())
        invoiceRepository = InvoiceRepositoryImpl(db, db.invoiceDao(), db.orderDao(), db.userDao())
        receiptRepository = ReceiptRepositoryImpl(
            db, db.receiptDao(), db.buyerDao(), db.invoiceDao()
        )

        // المدير يوقّع الاعتمادات لاحقاً؛ بلا مستخدم في القاعدة ترفض الفوترة.
        userRepository.ensureSeedAdmin("Admin@2024")
        adminId = userRepository.observeUsers().first().first { it.role == UserRole.ADMIN }.id
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * السلسلة الكاملة: نتحقق بعد كل خطوة أن أثرها وقع فعلاً في القاعدة،
     * لا أن الدالة أعادت نجاحاً فقط.
     */
    @Test
    fun fullFlow_orderToSettledBalance() = runTest {
        /* ---------------------- 1) مشترٍ ومنتج ---------------------- */
        val buyer = com.agency.app.core.domain.model.Buyer(
            id = Ids.new(),
            name = "بقالة النور",
            phone = "0500000001",
            creditLimit = Money.fromMajor(5000),
            createdAt = TimeSource.now(),
            updatedAt = TimeSource.now(),
        )
        assertTrue(buyerRepository.upsertBuyer(buyer) is Outcome.Success)

        val product = Product(
            id = Ids.new(),
            name = "أرز بسمتي 10كجم",
            sku = "RICE-10",
            unit = "كيس",
            price = Money.fromMajor(120),
            cost = Money.fromMajor(100),
            stockQuantity = 50,
            minOrderQuantity = 2,
            updatedAt = TimeSource.now(),
        )
        assertTrue(productRepository.upsert(product) is Outcome.Success)

        /* ---------------------- 2) إنشاء الطلبية ---------------------- */
        // الكمية 10 والمخزون 50: بعد الإنشاء يجب أن يصبح 40 بالضبط.
        val created = orderRepository.createOrder(
            buyerId = buyer.id,
            lines = listOf(NewOrderLine(productId = product.id, quantity = 10)),
            actorId = buyer.id,
            actorName = buyer.name,
            note = "طلبية اختبار التكامل",
        )
        assertTrue("إنشاء الطلبية فشل: $created", created is Outcome.Success)
        val order = (created as Outcome.Success).value
        assertEquals(OrderStatus.NEW, order.status)
        assertEquals(Money.fromMajor(120) * 10, order.total) // 1200.00

        // الأثر الحقيقي: المخزون نقص.
        val afterOrderStock = productRepository.getProduct(product.id)
        assertTrue(afterOrderStock is Outcome.Success)
        assertEquals(40, (afterOrderStock as Outcome.Success).value.stockQuantity)

        /* ------------- 3) دورة الحالة: NEW ← PREPARING ← PREPARED ------------- */
        val toPreparing = orderRepository.transition(
            order.id, OrderStatus.PREPARING, adminId, "مدير الوكالة", "بدء التحضير"
        )
        assertTrue("انتقال PREPARING فشل: $toPreparing", toPreparing is Outcome.Success)

        val toPrepared = orderRepository.transition(
            order.id, OrderStatus.PREPARED, adminId, "مدير الوكالة", "تم التحضير"
        )
        assertTrue("انتقال PREPARED فشل: $toPrepared", toPrepared is Outcome.Success)
        assertEquals(OrderStatus.PREPARED, (toPrepared as Outcome.Success).value.status)

        // الانتقال غير المشروع يجب أن يُرفض: PREPARED لا تسمح بـ DELIVERED مباشرة.
        val illegal = orderRepository.transition(
            order.id, OrderStatus.DELIVERED, adminId, "مدير الوكالة", null
        )
        assertTrue("قبل النظام انتقالاً غير مشروع PREPARED→DELIVERED", illegal is Outcome.Failure)

        /* ---------------------- 4) توليد الفاتورة ---------------------- */
        // عمولة 5% على 1200.00 = 60.00، فيصبح الإجمالي 1260.00
        val invoiced = invoiceRepository.createFromOrder(
            orderId = order.id,
            commissionPercent = 5,
            actorId = adminId,
            note = "فاتورة اختبار",
        )
        assertTrue("توليد الفاتورة فشل: $invoiced", invoiced is Outcome.Success)
        val draft = (invoiced as Outcome.Success).value
        assertEquals(InvoiceStatus.DRAFT, draft.status)
        assertEquals(Money.fromMajor(1200), draft.total)
        assertEquals(Money.fromMajor(60), draft.commission)
        assertEquals(Money.fromMajor(1260), draft.grandTotal)

        /* ---------------------- 5) اعتماد الفاتورة ---------------------- */
        val approved = invoiceRepository.approve(draft.id, adminId)
        assertTrue("اعتماد الفاتورة فشل: $approved", approved is Outcome.Success)
        assertEquals(InvoiceStatus.APPROVED, (approved as Outcome.Success).value.status)

        // الذمّة صارت 1260.00 — الفاتورة وحدها هي ما يحرّك الرصيد.
        val balanceAfterInvoice = buyerRepository.observeBalance(buyer.id).first()
        assertEquals(Money.fromMajor(1260), balanceAfterInvoice.outstanding)
        assertTrue(balanceAfterInvoice.isOverLimit.not()) // 1260 < 5000

        /* ---------------------- 6) سند قبض جزئي ---------------------- */
        val partial = receiptRepository.collect(
            buyerId = buyer.id,
            amount = Money.fromMajor(260),
            method = PaymentMethod.CASH,
            collectorUserId = adminId,
            collectorName = "مدير الوكالة",
            invoiceId = draft.id,
            note = "دفعة أولى",
        )
        assertTrue("سند القبض فشل: $partial", partial is Outcome.Success)

        val balanceAfterPartial = buyerRepository.observeBalance(buyer.id).first()
        assertEquals(Money.fromMajor(1000), balanceAfterPartial.outstanding)

        /* ---------------------- 7) سداد الباقي ---------------------- */
        val settlement = receiptRepository.collect(
            buyerId = buyer.id,
            amount = Money.fromMajor(1000),
            method = PaymentMethod.TRANSFER,
            collectorUserId = adminId,
            collectorName = "مدير الوكالة",
            invoiceId = draft.id,
            reference = "TRX-1",
            note = "سداد الباقي",
        )
        assertTrue("سند السداد فشل: $settlement", settlement is Outcome.Success)

        /* ------------------- 8) الذمّة صفر وكشف الحساب متسق ------------------- */
        val finalBalance = buyerRepository.observeBalance(buyer.id).first()
        assertEquals(Money.ZERO, finalBalance.outstanding)
        assertTrue("الذمّة لم تُغلق", finalBalance.isSettled)

        val statement = buyerRepository.observeStatement(buyer.id).first()
        assertNotNull(statement)
        assertTrue("كشف الحساب فارغ", statement.isNotEmpty())

        // آخر سطر في الكشف يجب أن ينتهي برصيد صفر، لا أن يعيد العرض حسابه.
        val lastEntry = statement.last()
        assertEquals(Money.ZERO, lastEntry.balanceAfter)

        // مجموع السندات يساوي إجمالي الفاتورة: لا رصيد معلّق ولا زيادة.
        val totalCollected = receiptRepository.totalCollected(buyer.id)
        assertTrue(totalCollected is Outcome.Success)
        assertEquals(
            Money.fromMajor(1260),
            (totalCollected as Outcome.Success).value
        )

        // إجمالي الفواتير المعتمدة في الذمّة.
        assertEquals(Money.fromMajor(1260), finalBalance.totalInvoiced)
        assertEquals(Money.fromMajor(1260), finalBalance.totalPaid)
        assertEquals(100, finalBalance.collectionRate)
    }

    /**
     * حارس المخزون: لا يمكن طلب كمية أكبر من المتاح، والفشل يجب ألا يخصم
     * شيئاً. هذا هو الفرق بين رفض العملية وخصمٍ جزئي فاشل.
     */
    @Test
    fun createOrder_beyondStock_isRejectedAndStockUnchanged() = runTest {
        val buyer = com.agency.app.core.domain.model.Buyer(
            id = Ids.new(),
            name = "مؤسسة الأمانة",
            phone = "0500000002",
            createdAt = TimeSource.now(),
            updatedAt = TimeSource.now(),
        )
        buyerRepository.upsertBuyer(buyer)

        val product = Product(
            id = Ids.new(),
            name = "سكر 5كجم",
            sku = "SUGAR-5",
            unit = "كيس",
            price = Money.fromMajor(30),
            stockQuantity = 3,
            minOrderQuantity = 1,
            updatedAt = TimeSource.now(),
        )
        productRepository.upsert(product)

        val result = orderRepository.createOrder(
            buyerId = buyer.id,
            lines = listOf(NewOrderLine(productId = product.id, quantity = 10)),
            actorId = buyer.id,
            actorName = buyer.name,
            note = null,
        )
        assertTrue("قُبلت طلبية تتجاوز المخزون", result is Outcome.Failure)

        val stockNow = productRepository.getProduct(product.id)
        assertEquals(3, (stockNow as Outcome.Success).value.stockQuantity)
    }

    /**
     * الطلبية الملغاة تُرجع المخزون: الإلغاء ليس مجرد تغيير حالة،
     * بل عملية مخزنية كاملة.
     */
    @Test
    fun cancelOrder_restoresStock() = runTest {
        val buyer = com.agency.app.core.domain.model.Buyer(
            id = Ids.new(),
            name = "سوق الخير",
            phone = "0500000003",
            createdAt = TimeSource.now(),
            updatedAt = TimeSource.now(),
        )
        buyerRepository.upsertBuyer(buyer)

        val product = Product(
            id = Ids.new(),
            name = "زيت ذرة",
            sku = "OIL-1",
            unit = "عبوة",
            price = Money.fromMajor(45),
            stockQuantity = 20,
            minOrderQuantity = 1,
            updatedAt = TimeSource.now(),
        )
        productRepository.upsert(product)

        val created = orderRepository.createOrder(
            buyerId = buyer.id,
            lines = listOf(NewOrderLine(productId = product.id, quantity = 5)),
            actorId = buyer.id,
            actorName = buyer.name,
            note = null,
        )
        val order = (created as Outcome.Success).value

        val afterCreate = productRepository.getProduct(product.id)
        assertEquals(15, (afterCreate as Outcome.Success).value.stockQuantity)

        val cancelled = orderRepository.transition(
            order.id, OrderStatus.CANCELLED, adminId, "مدير الوكالة", "إلغاء بطلب المشتري"
        )
        assertTrue("الإلغاء فشل: $cancelled", cancelled is Outcome.Success)

        val afterCancel = productRepository.getProduct(product.id)
        assertEquals(
            "الإلغاء لم يُرجع المخزون",
            20,
            (afterCancel as Outcome.Success).value.stockQuantity
        )
    }
}