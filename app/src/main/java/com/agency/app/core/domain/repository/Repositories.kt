package com.agency.app.core.domain.repository

import com.agency.app.core.domain.model.Buyer
import com.agency.app.core.domain.model.BuyerBalance
import com.agency.app.core.domain.model.BuyerStatementEntry
import com.agency.app.core.domain.model.Invoice
import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.Order
import com.agency.app.core.domain.model.OrderStatus
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.PaymentMethod
import com.agency.app.core.domain.model.PermissionRequest
import com.agency.app.core.domain.model.Product
import com.agency.app.core.domain.model.Receipt
import com.agency.app.core.domain.model.User
import com.agency.app.core.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * عقود الطبقة الوسطى.
 *
 * القاعدة الحاكمة: كل استعلام قراءة يُعيد [Flow] يخرج من Room،
 * وكل عملية كتابة تُعيد [Outcome] مغلَّفاً بـ AppError.
 * لا يُسرَّب أي استثناء خام ولا أي نوع من أنواع الشبكة إلى الواجهة.
 */

/* ------------------------------------------------------------------ */
/*  المستخدمون والمصادقة                                              */
/* ------------------------------------------------------------------ */

/** واجهة إدارة المستخدمين. إنشاء الحسابات حصراً للمدير. */
interface UserRepository {

    fun observeUsers(): Flow<List<User>>

    suspend fun getUser(id: String): Outcome<User>

    suspend fun findByUsername(username: String): Outcome<User?>

    /** إنشاء مستخدم جديد بكلمة مرور مُجزَّأة، لا تُخزَّن أبداً كنص صريح. */
    suspend fun createUser(
        username: String,
        displayName: String,
        password: String,
        role: UserRole,
        phone: String? = null,
        buyerId: String? = null
    ): Outcome<User>

    suspend fun setActive(userId: String, active: Boolean): Outcome<Unit>
    suspend fun changePassword(userId: String, newPassword: String): Outcome<Unit>

    /**
     * يتحقق من كلمة المرور الحالية بلا أي أثر جانبي.
     *
     * يلزم لسببين: تغيير كلمة المرور يتطلب تأكيد القديمة، وفحص
     * «هل ما زال المدير على كلمة المرور الافتراضية؟» يجب أن يُقرأ من
     * حقيقة القاعدة لا من علامة محلية في الجهاز — وإلا أعاد تثبيت التطبيق
     * كلمة المرور الافتراضية بلا أن يطلب تغييرها.
     */
    suspend fun verifyPassword(userId: String, password: String): Outcome<Boolean>

    /** يتحقق من كلمة المرور ويحدّث وقت آخر دخول. */
    suspend fun authenticate(username: String, password: String): Outcome<User>

    /** يزرع حساب المدير الأول عند أول تشغيل فقط. */
    suspend fun ensureSeedAdmin(password: String): Outcome<Unit>

    suspend fun touchLastLogin(userId: String, at: Instant): Outcome<Unit>
}

/* ------------------------------------------------------------------ */
/*  المشترون                                                          */
/* ------------------------------------------------------------------ */

interface BuyerRepository {

    fun observeBuyers(): Flow<List<Buyer>>

    fun observeBuyer(id: String): Flow<Buyer?>

    suspend fun getBuyer(id: String): Outcome<Buyer>

    suspend fun upsertBuyer(buyer: Buyer): Outcome<Unit>

    suspend fun setActive(buyerId: String, active: Boolean): Outcome<Unit>

    /** ملخّص مالي لمشترٍ واحد: إجمالي المفوتر، المدفوع، المتبقي، الحد الائتماني. */
    fun observeBalance(buyerId: String): Flow<BuyerBalance>

    /** كل ذمم المشترين — شاشة المحاسب الرئيسية. */
    fun observeBalances(): Flow<List<BuyerBalance>>

    /** كشف حساب مفصّل مرتّب زمنياً مع الرصيد التراكمي. */
    fun observeStatement(buyerId: String): Flow<List<BuyerStatementEntry>>
}

/* ------------------------------------------------------------------ */
/*  المنتجات                                                          */
/* ------------------------------------------------------------------ */

interface ProductRepository {

    /** المنتجات النشطة فقط — يستخدمها كتالوج المشتري. */
    fun observeActiveProducts(): Flow<List<Product>>

    /** كل المنتجات بما فيها المعطّلة — لوحة المدير والمخزن. */
    fun observeAllProducts(): Flow<List<Product>>

    fun observeProduct(id: String): Flow<Product?>

    fun search(query: String): Flow<List<Product>>

    suspend fun getProduct(id: String): Outcome<Product>

    suspend fun upsert(product: Product): Outcome<Unit>

    suspend fun setActive(productId: String, active: Boolean): Outcome<Unit>

    suspend fun setStock(productId: String, quantity: Int): Outcome<Unit>

    /** المسار الوحيد لخصم المخزون؛ يرفض الكمية غير المتاحة. */
    suspend fun decrementStock(productId: String, quantity: Int): Outcome<Unit>

    suspend fun delete(productId: String): Outcome<Unit>
}

/* ------------------------------------------------------------------ */
/*  الطلبيات                                                          */
/* ------------------------------------------------------------------ */

/** سطر مطلوب عند إنشاء طلبية جديدة (قبل أن يصبح OrderItem). */
data class NewOrderLine(
    val productId: String,
    val quantity: Int,
    val discount: Money = Money.ZERO
)

interface OrderRepository {

    fun observeOrders(): Flow<List<Order>>

    fun observeByStatus(status: OrderStatus): Flow<List<Order>>

    fun observeByBuyer(buyerId: String): Flow<List<Order>>

    fun observeOrder(id: String): Flow<Order?>

    suspend fun getOrder(id: String): Outcome<Order>

    /**
     * ينشئ طلبية جديدة بحالة NEW مع خطها الزمني الأول،
     * ويخصم المخزون لكل سطر بشكل ذرّي.
     */
    suspend fun createOrder(
        buyerId: String,
        lines: List<NewOrderLine>,
        actorId: String,
        actorName: String,
        note: String? = null
    ): Outcome<Order>

    /**
     * ينقل الطلبية إلى الحالة التالية بعد التحقق من جدول الانتقالات المسموح
     * ([OrderStatus.allowedNext])، ويضيف سطراً للخط الزمني.
     */
    suspend fun transition(
        orderId: String,
        target: OrderStatus,
        actorId: String,
        actorName: String,
        note: String? = null
    ): Outcome<Order>

    suspend fun countByStatus(status: OrderStatus): Outcome<Int>
}

/* ------------------------------------------------------------------ */
/*  الفواتير                                                          */
/* ------------------------------------------------------------------ */

interface InvoiceRepository {

    fun observeInvoices(): Flow<List<Invoice>>

    fun observeByStatus(status: InvoiceStatus): Flow<List<Invoice>>

    fun observeByBuyer(buyerId: String): Flow<List<Invoice>>

    fun observeInvoice(id: String): Flow<Invoice?>

    suspend fun getInvoice(id: String): Outcome<Invoice>

    suspend fun findByOrderId(orderId: String): Outcome<Invoice?>

    /**
     * يولّد فاتورة DRAFT من طلبية جاهزة أو مرسلة، مع نسخة مجمّدة من الأسعار
     * والعمولة محسوبة بالنقطة المئوية المطلوبة.
     */
    suspend fun createFromOrder(
        orderId: String,
        commissionPercent: Int,
        actorId: String,
        note: String? = null
    ): Outcome<Invoice>

    suspend fun updateDraft(invoice: Invoice): Outcome<Unit>

    /** اعتماد الفاتورة — يجعلها ملزمة وتدخل في ذمّة المشتري. */
    suspend fun approve(invoiceId: String, approverId: String): Outcome<Invoice>

    suspend fun reject(invoiceId: String, approverId: String, reason: String): Outcome<Invoice>

    suspend fun deleteDraft(invoiceId: String): Outcome<Unit>
}

/* ------------------------------------------------------------------ */
/*  سندات القبض                                                       */
/* ------------------------------------------------------------------ */

interface ReceiptRepository {

    fun observeReceipts(): Flow<List<Receipt>>

    fun observeByBuyer(buyerId: String): Flow<List<Receipt>>

    suspend fun getReceipt(id: String): Outcome<Receipt>

    /**
     * يسجّل سند قبض ويخصمه تلقائياً من ذمّة المشتري.
     * أي تعديل على الذمة يمر من هنا وليس من أي مكان آخر.
     */
    suspend fun collect(
        buyerId: String,
        amount: Money,
        method: PaymentMethod,
        collectorUserId: String,
        collectorName: String,
        invoiceId: String? = null,
        reference: String? = null,
        note: String? = null,
        isSigned: Boolean = false,
        signaturePath: String? = null
    ): Outcome<Receipt>

    suspend fun totalCollected(buyerId: String): Outcome<Money>

    suspend fun markSynced(receiptId: String): Outcome<Unit>
}

/* ------------------------------------------------------------------ */
/*  طلبات الصلاحية                                                    */
/* ------------------------------------------------------------------ */

interface PermissionRepository {

    fun observeAll(): Flow<List<PermissionRequest>>

    fun observeByStatus(status: com.agency.app.core.domain.model.PermissionRequestStatus): Flow<List<PermissionRequest>>

    /** عدد الطلبات المعلّقة — تُعرض كشارة على شاشة المدير. */
    fun observePendingCount(): Flow<Int>

    suspend fun request(
        requesterUserId: String,
        requesterName: String,
        requesterRole: UserRole,
        action: String,
        targetEntity: String,
        targetId: String?,
        reason: String
    ): Outcome<PermissionRequest>

    /** الموافقة تتطلب توقيعاً رقمياً من المدير، وإلا تُرفض العملية. */
    suspend fun approve(
        requestId: String,
        reviewerUserId: String,
        reviewerName: String,
        signatureData: String,
        reviewNote: String? = null
    ): Outcome<PermissionRequest>

    suspend fun reject(
        requestId: String,
        reviewerUserId: String,
        reviewerName: String,
        reviewNote: String
    ): Outcome<PermissionRequest>

    /** هل يوجد طلب معتمد وساري لهذا الإجراء على هذا الكيان؟ */
    suspend fun hasApproval(
        requesterUserId: String,
        action: String,
        targetEntity: String,
        targetId: String?
    ): Outcome<Boolean>
}