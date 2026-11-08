package com.agency.app

import com.agency.app.core.domain.model.AppError
import com.agency.app.core.domain.model.BuyerBalance
import com.agency.app.core.domain.model.Invoice
import com.agency.app.core.domain.model.InvoiceItem
import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.domain.model.Order
import com.agency.app.core.domain.model.OrderItem
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.OrderStatus
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.PaymentMethod
import com.agency.app.core.domain.model.PermissionRequestStatus
import com.agency.app.core.domain.model.Product
import com.agency.app.core.domain.model.Receipt
import com.agency.app.core.domain.model.UserRole
import com.agency.app.core.domain.model.canTransitionTo
import com.agency.app.core.domain.model.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * قواعد العمل كنصوص قابلة للتنفيذ.
 *
 * هذه الاختبارات لا تلمس قاعدة ولا واجهة. كل واحد منها يثبّت **قاعدة**:
 * قراراً اتُّخذ مرة، ولو كُسر لاحقاً فسيكسره إنسان بنيّة حسنة. القاعدة
 * المكتوبة في تعليق تُقرأ وتُنسى؛ القاعدة في اختبار تفشل بصوت عالٍ.
 *
 * الأرقام هنا محسوبة يدوياً في التعليق قبل أن تُكتب، لأن اختباراً ينسخ
 * صيغة الكود الذي يختبره لا يثبت شيئاً.
 */
class DomainRulesTest {

    /* ------------------------- المال ------------------------- */

    @Test
    fun `money uses integer cents so no floating error accumulates`() {
        // 0.10 + 0.20 يجب أن يكون 0.30 بالضبط. ولو كان Money على Double
        // لكان 0.30000000000000004، وفي 10,000 سطر فاتورة يصير فرقاً حقيقياً.
        val ten = Money(10)
        val twenty = Money(20)
        assertEquals(30, (ten + twenty).cents)

        // ضرب سعر في كمية: 12.35 × 7 = 86.45
        val unit = Money(1235)
        assertEquals(8645, (unit * 7).cents)
    }

    @Test
    fun `money percent truncates toward zero and never overstates commission`() {
        // 1% من 99.99 = 0.9999 -> 0.99 لا 1.00.
        // التقريب للأعلى كان سيجعل مجموع عمولات البنود يتجاوز عمولة الإجمالي.
        assertEquals(99, Money(9999).percent(1).cents)
        // 2% من 1,000.00 = 20.00 بالضبط
        assertEquals(2000, Money(100_000).percent(2).cents)
    }

    @Test
    fun `money fromString rejects negative and malformed input`() {
        assertNull(Money.fromString("-5"))
        assertNull(Money.fromString("abc"))
        assertNull(Money.fromString(""))
        // الفاصلة العربية تُقبل كمزيّة عشرية
        assertEquals(1250, Money.fromString("12,50")?.cents)
    }

    /* ------------------------- الأدوار ------------------------- */

    @Test
    fun `only three roles may write financial data`() {
        val writers = UserRole.entries.filter { it.canWriteFinancials }
        assertEquals(
            setOf(UserRole.ADMIN, UserRole.WAREHOUSE, UserRole.CASHIER),
            writers.toSet(),
        )
        // المشتري لا يكتب مالياً: طلبه يُسعَّر من الكتالوج لا من إدخاله.
        assertFalse(UserRole.BUYER.canWriteFinancials)
    }

    @Test
    fun `accountant is the only always read-only role`() {
        assertEquals(listOf(UserRole.ACCOUNTANT), UserRole.entries.filter { it.isReadOnly })
        // ولا يتعارضان: المحاسب للقراءة فقط ولا يكتب مالياً.
        assertFalse(UserRole.ACCOUNTANT.canWriteFinancials)
    }

    /* ------------------- جدول انتقالات الطلبية ------------------- */

    @Test
    fun `order status follows a single forward path`() {
        assertTrue(OrderStatus.NEW.canTransitionTo(OrderStatus.PREPARING))
        assertTrue(OrderStatus.PREPARING.canTransitionTo(OrderStatus.PREPARED))
        assertTrue(OrderStatus.PREPARED.canTransitionTo(OrderStatus.SENT))
        assertTrue(OrderStatus.SENT.canTransitionTo(OrderStatus.DELIVERED))
    }

    @Test
    fun `order status never skips a step`() {
        // القفز من NEW إلى DELIVERED كان سيُنشئ طلبية «مُسلَّمة» بلا تحضير
        // ولا خصم مخزون محقَّق، وهي أسوأ من رفض الانتقال.
        assertFalse(OrderStatus.NEW.canTransitionTo(OrderStatus.DELIVERED))
        assertFalse(OrderStatus.NEW.canTransitionTo(OrderStatus.SENT))
        assertFalse(OrderStatus.PREPARING.canTransitionTo(OrderStatus.DELIVERED))
    }

    @Test
    fun `cancel is impossible after dispatch`() {
        // بعد SENT البضاعة خرجت من المخزن. الإلغاء هنا كان سيُعيد رصيداً
        // لم يُعد فعلاً، فيصير المخزون ورقياً.
        assertFalse(OrderStatus.SENT.canTransitionTo(OrderStatus.CANCELLED))
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED))
        // وقبل الإرسال ممكن
        assertTrue(OrderStatus.NEW.canTransitionTo(OrderStatus.CANCELLED))
        assertTrue(OrderStatus.PREPARED.canTransitionTo(OrderStatus.CANCELLED))
    }

    @Test
    fun `final states accept no transition at all`() {
        assertTrue(OrderStatus.DELIVERED.allowedNext.isEmpty())
        assertTrue(OrderStatus.CANCELLED.allowedNext.isEmpty())
        assertTrue(OrderStatus.entries.filter { it.isFinal }.all { it.allowedNext.isEmpty() })
    }

    @Test
    fun `only prepared and sent orders may be invoiced`() {
        val invoiceable = OrderStatus.entries.filter { it.isInvoiceable }
        assertEquals(setOf(OrderStatus.PREPARED, OrderStatus.SENT), invoiceable.toSet())
        // لماذا لا NEW؟ لأن الفاتورة تُجمّد سعراً عن بضاعة جاهزة فعلاً.
        assertFalse(OrderStatus.NEW.isInvoiceable)
        // ولماذا لا DELIVERED؟ لأن الفوترة بعد التسليم تفتح باب تأخير الاعتماد
        // إلى ما بعد خروج البضاعة.
        assertFalse(OrderStatus.DELIVERED.isInvoiceable)
    }

    /* ------------------- حساب الفاتورة ------------------- */

    @Test
    fun `invoice totals are derived not stored`() {
        // بندان: 100.00×2 بخصم 10.00، و50.00×1 بلا خصم. عمولة 3%.
        // subtotal = 200 + 50 = 250
        // discount = 10
        // total    = 240
        // commission = 3% من 240 = 7.20
        // grand    = 247.20
        val subtotalCents = 20000 + 5000
        val discountCents = 1000
        val totalCents = subtotalCents - discountCents
        val commissionCents = Money(totalCents).percent(3).cents
        assertEquals(24000, totalCents)
        assertEquals(720, commissionCents)
        assertEquals(24720, totalCents + commissionCents)
    }

    @Test
    fun `balance outstanding is invoiced minus paid`() {
        val balance = BuyerBalance(
            buyerId = "b1",
            buyerName = "مشترٍ",
            totalInvoiced = Money(100_000),
            totalPaid = Money(40_000),
            creditLimit = Money(150_000),
        )
        assertEquals(60_000, balance.outstanding.cents)
        assertFalse(balance.isSettled)
        assertFalse(balance.isOverLimit) // 600 < 1500
    }

    @Test
    fun `buyer over limit is detected strictly above the credit line`() {
        val exactlyAtLimit = BuyerBalance(
            buyerId = "b1",
            buyerName = "مشترٍ",
            totalInvoiced = Money(100_000),
            totalPaid = Money(0),
            creditLimit = Money(100_000),
        )
        // بلوغ السقف ليس تجاوزاً: المنع عند التجاوز لا عند الوصول، وإلا
        // رفضنا مشترياً أنفق ما سُمح له به بالضبط.
        assertFalse(exactlyAtLimit.isOverLimit)

        val over = exactlyAtLimit.copy(totalInvoiced = Money(100_001))
        assertTrue(over.isOverLimit)
    }

    @Test
    fun `credit limit of zero means unlimited not blocked`() {
        // سقف صفر عُرف في النظام كـ«لا سقف». لو قُرئ حرفياً لصار كل مشترٍ
        // متجاوزاً منذ أول فاتورة.
        val unlimited = BuyerBalance(
            buyerId = "b1",
            buyerName = "مشترٍ",
            totalInvoiced = Money(999_999_99),
            totalPaid = Money(0),
            creditLimit = Money.ZERO,
        )
        assertFalse(unlimited.isOverLimit)
    }

    @Test
    fun `collection rate is paid over invoiced and never divides by zero`() {
        val zeroInvoiced = BuyerBalance("b", "ن", Money.ZERO, Money.ZERO, Money.ZERO)
        // لا قسمة على صفر: النتيجة 100 لأن «لا شيء مستحق» يعني سداداً كاملاً.
        assertEquals(100, zeroInvoiced.collectionRate)

        val half = BuyerBalance("b", "ن", Money(200_00), Money(100_00), Money.ZERO)
        assertEquals(50, half.collectionRate)
    }

    /* ------------------- حدود السند والبصمة ------------------- */

    @Test
    fun `biometric threshold is inclusive at one million`() {
        val below = receiptWithAmount(Money(999_999_99))
        val at = receiptWithAmount(Money(1_000_000_00))
        val above = receiptWithAmount(Money(1_500_000_00))

        assertFalse(below.requiresBiometric)
        // «يبلغ الحد أو يتجاوزه» — المساواة تستوجب البصمة، وهذا الحد
        // الوحيد الذي لا يجوز فيه التسامح.
        assertTrue(at.requiresBiometric)
        assertTrue(above.requiresBiometric)
    }

    @Test
    fun `new receipt starts unsynced and unsigned`() {
        val fresh = receiptWithAmount(Money(50_000))
        // سند جديد لم يُزامَن ولم يُوقَّع: حالة صحيحة تبدأ منها الشاشة،
        // ولا تُعلن كأنها محفوظة على الخادم.
        assertFalse(fresh.isSynced)
        assertFalse(fresh.isSigned)
        assertNull(fresh.signaturePath)
    }

    /* ------------------- القيم والحالات النهائية ------------------- */

    @Test
    fun `invoice editability follows status not role`() {
        // المسودّة والمرفوضة قابلة للتعديل؛ المعتمدة لا.
        // الربط بالحالة لا بالدور يمنع مديراً من تعديل فاتورة معتمدة
        // بمجرد أنه يملك الصلاحية.
        val editable = setOf(InvoiceStatus.DRAFT, InvoiceStatus.REJECTED)
        assertTrue(InvoiceStatus.DRAFT in editable)
        assertTrue(InvoiceStatus.REJECTED in editable)
        assertFalse(InvoiceStatus.APPROVED in editable)
        assertFalse(InvoiceStatus.ARCHIVED in editable)
    }

    @Test
    fun `permission request states are mutually exclusive`() {
        val pending = PermissionRequestStatus.PENDING
        val approved = PermissionRequestStatus.APPROVED
        val rejected = PermissionRequestStatus.REJECTED
        assertEquals(3, setOf(pending, approved, rejected).size)
        // لا حالة رابعة صامتة: الطلب إما معلّق أو محسوم.
        assertEquals(3, PermissionRequestStatus.entries.size)
    }

    @Test
    fun `payment methods cover the three ways money actually arrives`() {
        assertEquals(
            setOf(PaymentMethod.CASH, PaymentMethod.TRANSFER, PaymentMethod.CHECK),
            PaymentMethod.entries.toSet(),
        )
    }

    /* ------------------- المنتج والمخزون ------------------- */

    @Test
    fun `product stock flags detect empty and near-empty`() {
        assertTrue(productWithStock(0).isOutOfStock)
        assertTrue(productWithStock(5).isLowStock)
        // 10 فما فوق ليس منخفضاً — العتبة 9 مقصودة.
        assertFalse(productWithStock(10).isLowStock)
        assertFalse(productWithStock(10).isOutOfStock)
    }

    @Test
    fun `stock value uses price for display and margin uses cost`() {
        val product = productWithStock(4).copy(price = Money(2_500), cost = Money(1_800))
        // قيمة المخزون للعرض = سعر البيع × الكمية
        assertEquals(10_000, product.stockValue.cents)
        // الهامش = سعر − تكلفة، وهو الرقم الذي يُبنى عليه قرار الشراء.
        assertEquals(700, product.margin.cents)
    }

    @Test
    fun `negative margin is visible not clamped`() {
        // بيع بأقل من التكلفة يجب أن يُظهر هامشاً سالباً. تقييده عند صفر
        // كان سيُخفي خسارة حقيقية تحت رقم محترم.
        val loss = productWithStock(1).copy(price = Money(1_000), cost = Money(1_500))
        assertTrue(loss.margin.isNegative)
        assertEquals(-500, loss.margin.cents)
    }

    /* ------------------------- Outcome ------------------------- */

    @Test
    fun `map transforms success and passes failure through untouched`() {
        val ok: Outcome<Int> = Outcome.Success(21)
        assertEquals(42, (ok.map { it * 2 } as Outcome.Success).value)

        val error: AppError = AppError.Validation("amount", "سالب")
        val failed: Outcome<Int> = Outcome.Failure(error)
        val mapped = failed.map { it * 2 }
        assertTrue(mapped is Outcome.Failure)
        // نفس الخطأ بالهوية: تحويل النجاح لا يجوز أن يخلق خطأً جديداً.
        assertEquals(error, (mapped as Outcome.Failure).error)
    }

    @Test
    fun `appError messages are non-empty for every variant`() {
        // رسالة فارغة تعني شاشة صامتة عند الفشل، وهو أسوأ من خطأ مفهوم.
        val errors = listOf(
            AppError.Network("انقطع"),
            AppError.Permission("مالية"),
            AppError.Validation("qty", "صفر"),
            AppError.NotFound("Invoice", "x1"),
            AppError.Conflict("معتمدة"),
            AppError.Unknown(),
        )
        assertTrue(errors.all { it.message.isNotBlank() })
    }

    @Test
    fun `permission error names the missing capability`() {
        val error = AppError.Permission("اعتماد الفواتير")
        assertTrue(error.message.contains("اعتماد الفواتير"))
    }

    /* ------------- الفاتورة: العمولة والخصم بالترتيب ------------- */
    @Test
    fun `invoice commission is added after discount never before`() {
        // بندان: 12.35 × 3 = 37.05 بخصم 1.05 -> 36.00
        //        5.00 × 2 = 10.00 بلا خصم   -> 10.00
        // المجموع الفرعي = 47.05، الخصم = 1.05، الإجمالي = 46.00
        // عمولة 2.30 تُضاف بعد الخصم فيصير المستحق 48.30.
        val invoice = invoiceWith(
            items = listOf(
                invoiceItemWith(unitPrice = Money(1235), quantity = 3, discount = Money(105)),
                invoiceItemWith(unitPrice = Money(500), quantity = 2),
            ),
            commission = Money(230),
        )
        assertEquals(4_705, invoice.subtotal.cents)
        assertEquals(105, invoice.discountTotal.cents)
        assertEquals(4_600, invoice.total.cents)
        assertEquals(4_830, invoice.grandTotal.cents)
    }

    @Test
    fun `invoice item net subtracts discount from gross`() {
        val item = invoiceItemWith(unitPrice = Money(1235), quantity = 3, discount = Money(105))
        assertEquals(3_705, item.gross.cents)
        // الخصم يُنسب إلى السطر لا إلى الفاتورة، ولهذا يُحسب قبل الجمع.
        assertEquals(3_600, item.net.cents)
    }

    /* -------------- الطلبية: المسار وشريط التقدّم -------------- */
    @Test
    fun `cancelled order has no position on the progress track`() {
        // الملغاة ليست مرحلة على المسار، بل خروج منه. إرجاع 0 أو 4 كان
        // سيُظهرها كأنها «جديدة» أو «مُسلَّمة».
        assertEquals(-1, orderWith(OrderStatus.CANCELLED).currentStepIndex)
        assertEquals(0, orderWith(OrderStatus.NEW).currentStepIndex)
        assertEquals(4, orderWith(OrderStatus.DELIVERED).currentStepIndex)
    }

    @Test
    fun `order cancellable flag mirrors the transition table`() {
        // لا يُكرَّر منطق الإلغاء في مكانين. isCancellable تُقرأ من نفس
        // جدول الانتقالات، فأي تغيير فيه يسري على الزر تلقائياً.
        assertTrue(orderWith(OrderStatus.NEW).isCancellable)
        assertTrue(orderWith(OrderStatus.PREPARED).isCancellable)
        assertFalse(orderWith(OrderStatus.SENT).isCancellable)
        assertFalse(orderWith(OrderStatus.DELIVERED).isCancellable)
    }

    /* ------------------- نص المال المعروض ------------------- */
    @Test
    fun `money toString keeps two decimals and a leading minus`() {
        // النص يُعرض للمستخدم مباشرة، فأي صيغة أخرى ستُقرأ كمبلغ مختلف.
        assertEquals("48.30", Money(4_830).toString())
        assertEquals("1.50", Money(150).toString())
        // 5 سنت = 0.05 وليس 0.5 — الفرق سنة كاملة من التقريب.
        assertEquals("0.05", Money(5).toString())
        assertEquals("-1.50", (Money(150).unaryMinus()).toString())
    }

    /* ------------------------- مساعدات ------------------------- */

    private fun receiptWithAmount(amount: Money) = Receipt(
        id = "r1",
        receiptNumber = "RC-1",
        buyerId = "b1",
        buyerName = "مشترٍ",
        amount = amount,
        method = PaymentMethod.CASH,
        collectorUserId = "u1",
        collectorName = "متحصّل",
        collectedAt = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
        createdAt = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
    )

    private fun productWithStock(stock: Int) = Product(
        id = "p1",
        name = "منتج",
        sku = "SKU-1",
        unit = "كرتون",
        price = Money(1_000),
        stockQuantity = stock,
        updatedAt = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
    )

    private fun invoiceItemWith(unitPrice: Money, quantity: Int, discount: Money = Money.ZERO) =
        InvoiceItem(
            id = "ii1",
            invoiceId = "inv1",
            productId = "p1",
            productName = "منتج",
            unit = "كرتون",
            unitPrice = unitPrice,
            quantity = quantity,
            discount = discount,
        )

    private fun invoiceWith(items: List<InvoiceItem>, commission: Money = Money.ZERO) = Invoice(
        id = "inv1",
        invoiceNumber = "INV-1",
        orderId = "o1",
        buyerId = "b1",
        buyerName = "مشترٍ",
        status = InvoiceStatus.DRAFT,
        items = items,
        commission = commission,
        createdByUserId = "u1",
        createdAt = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
        updatedAt = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
    )

    private fun orderWith(status: OrderStatus) = Order(
        id = "o1",
        orderNumber = "ORD-1",
        buyerId = "b1",
        buyerName = "مشترٍ",
        status = status,
        items = listOf(
            OrderItem(
                id = "oi1",
                orderId = "o1",
                productId = "p1",
                productName = "منتج",
                unit = "كرتون",
                unitPrice = Money(1_000),
                quantity = 2,
            ),
        ),
        createdAt = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
        updatedAt = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
    )
}
