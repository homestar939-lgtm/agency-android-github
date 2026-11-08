package com.agency.app.core.data.repository

import androidx.room.withTransaction
import com.agency.app.core.data.local.AppDatabase
import com.agency.app.core.data.local.dao.InvoiceDao
import com.agency.app.core.data.local.dao.OrderDao
import com.agency.app.core.data.local.dao.UserDao
import com.agency.app.core.data.local.mapper.buildInvoice
import com.agency.app.core.data.local.mapper.buildOrder
import com.agency.app.core.data.local.mapper.toDomain
import com.agency.app.core.data.local.mapper.toEntity
import com.agency.app.core.domain.model.AppError
import com.agency.app.core.domain.model.Invoice
import com.agency.app.core.domain.model.InvoiceItem
import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.OrderStatus
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.repository.InvoiceRepository
import com.agency.app.core.util.Ids
import com.agency.app.core.util.TimeSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * تنفيذ الفواتير.
 *
 * قواعد محاسبية صارمة:
 * 1) الفاتورة تُنشأ فقط من طلبية في مرحلة قابلة للفوترة ([OrderStatus.isInvoiceable]).
 * 2) تُجمَّد الأسعار وقت الإنشاء؛ تغيّر سعر المنتج لاحقاً لا يمسّ فاتورة قائمة.
 * 3) العمولة تُحسب بـ [Money.percent] على الصافي، وتُضاف إلى إجمالي الفاتورة.
 * 4) لا تدخل الفاتورة في ذمّة المشتري إلا بعد الاعتماد (APPROVED).
 */
@Singleton
class InvoiceRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val invoiceDao: InvoiceDao,
    private val orderDao: OrderDao,
    private val userDao: UserDao
) : InvoiceRepository {

    private fun rowToDomain(
        row: com.agency.app.core.data.local.dao.InvoiceWithDetails
    ): Invoice = buildInvoice(row.invoice, row.items)

    override fun observeInvoices(): Flow<List<Invoice>> =
        invoiceDao.observeAllDetailed().map { rows -> rows.map { rowToDomain(it) } }

    override fun observeByStatus(status: InvoiceStatus): Flow<List<Invoice>> =
        invoiceDao.observeByStatus(status.name).map { rows -> rows.map { rowToDomain(it) } }

    override fun observeByBuyer(buyerId: String): Flow<List<Invoice>> =
        invoiceDao.observeByBuyer(buyerId).map { rows -> rows.map { rowToDomain(it) } }

    override fun observeInvoice(id: String): Flow<Invoice?> =
        invoiceDao.observeById(id).map { row -> row?.let { rowToDomain(it) } }

    override suspend fun getInvoice(id: String): Outcome<Invoice> = guarded {
        val row = invoiceDao.findById(id)
            ?: return@guarded Outcome.Failure(AppError.NotFound("فاتورة", id))
        Outcome.Success(rowToDomain(row))
    }

    override suspend fun findByOrderId(orderId: String): Outcome<Invoice?> = guarded {
        val row = invoiceDao.findByOrderId(orderId)
        Outcome.Success(row?.let { rowToDomain(it) })
    }

    /**
     * إنشاء فاتورة من طلبية: نأخذ أسطر الطلبية كما هي (سعرها مجمّد أصلاً وقت الطلب)،
     * ونحسب العمولة على الصافي.
     */
    override suspend fun createFromOrder(
        orderId: String,
        commissionPercent: Int,
        actorId: String,
        note: String?
    ): Outcome<Invoice> = guarded {
        if (commissionPercent < 0 || commissionPercent > 100) {
            return@guarded Outcome.Failure(
                AppError.Validation("commissionPercent", "نسبة العمولة يجب أن تكون بين 0 و100")
            )
        }
        val existing = invoiceDao.findByOrderId(orderId)
        if (existing != null) {
            return@guarded Outcome.Failure(
                AppError.Conflict("الطلبية مفوترة بالفعل: ${existing.invoice.invoiceNumber}")
            )
        }
        val orderRow = orderDao.findById(orderId)
            ?: return@guarded Outcome.Failure(AppError.NotFound("طلبية", orderId))
        val order = buildOrder(orderRow.order, orderRow.items, orderRow.timeline)

        if (!order.status.isInvoiceable) {
            return@guarded Outcome.Failure(
                AppError.Validation(
                    "orderStatus",
                    "لا يمكن الفوترة في حالة ${order.status}؛ المطلوب PREPARED أو SENT"
                )
            )
        }
        if (order.items.isEmpty()) {
            return@guarded Outcome.Failure(
                AppError.Validation("items", "الطلبية بلا أسطر، لا يمكن فوترتها")
            )
        }

        val now = TimeSource.now()
        val invoiceId = Ids.new()
        val items = order.items.mapIndexed { index, item ->
            InvoiceItem(
                id = "$invoiceId-i${index + 1}",
                invoiceId = invoiceId,
                productId = item.productId,
                productName = item.productName,
                unit = item.unit,
                unitPrice = item.unitPrice,
                quantity = item.quantity,
                discount = item.discount
            )
        }
        val net = items.fold(Money.ZERO) { acc, item -> acc + item.net }
        val commission = net.percent(commissionPercent)

        val invoice = Invoice(
            id = invoiceId,
            invoiceNumber = Ids.invoiceNumber(),
            orderId = orderId,
            buyerId = order.buyerId,
            buyerName = order.buyerName,
            status = InvoiceStatus.DRAFT,
            items = items,
            commission = commission,
            note = note,
            createdByUserId = actorId,
            createdAt = now,
            updatedAt = now
        )

        db.withTransaction {
            invoiceDao.upsertDetailed(invoice.toEntity(), items.map { it.toEntity() })
        }

        Outcome.Success(invoice)
    }

    /** تعديل المسودة فقط — الفاتورة المعتمدة مستند محاسبي لا يُعدّل. */
    override suspend fun updateDraft(invoice: Invoice): Outcome<Unit> = guarded {
        val row = invoiceDao.findById(invoice.id)
            ?: return@guarded Outcome.Failure(AppError.NotFound("فاتورة", invoice.id))
        val current = rowToDomain(row)
        if (!current.isEditable) {
            return@guarded Outcome.Failure(
                AppError.Conflict(
                    "لا يمكن تعديل فاتورة في حالة ${current.status}"
                )
            )
        }
        if (invoice.items.isEmpty()) {
            return@guarded Outcome.Failure(
                AppError.Validation("items", "الفاتورة يجب أن تحوي سطراً واحداً على الأقل")
            )
        }
        if (invoice.items.any { it.quantity <= 0 }) {
            return@guarded Outcome.Failure(
                AppError.Validation("quantity", "كمية السطر يجب أن تكون أكبر من صفر")
            )
        }
        val now = TimeSource.now()
        val items = invoice.items.map { item -> item.copy(invoiceId = invoice.id) }
        val net = items.fold(Money.ZERO) { acc, item -> acc + item.net }
        // نحافظ على نسبة العمولة الأصلية بدل تخمينها من المبلغ.
        val ratio = if (current.commission.isZero || current.total.isZero) 0
        else ((current.commission.cents * 100L) / current.total.cents).toInt()
        val updated = invoice.copy(
            status = InvoiceStatus.DRAFT,
            items = items,
            commission = net.percent(ratio),
            approvedByUserId = null,
            approvedAt = null,
            rejectionReason = null,
            updatedAt = now
        )
        db.withTransaction {
            invoiceDao.upsertDetailed(updated.toEntity(), items.map { it.toEntity() })
        }
        Outcome.Success(Unit)
    }

    /**
     * الاعتماد: يقتصر على دور يملك الكتابة المالية، ويُسجَّل باسم المعتمد ووقته.
     * بعد الاعتماد تصبح الفاتورة ديناً على المشتري في [BuyerRepositoryImpl.observeBalance].
     */
    override suspend fun approve(invoiceId: String, approverId: String): Outcome<Invoice> =
        guarded {
            val row = invoiceDao.findById(invoiceId)
                ?: return@guarded Outcome.Failure(AppError.NotFound("فاتورة", invoiceId))
            val current = rowToDomain(row)

            if (current.status == InvoiceStatus.APPROVED) {
                return@guarded Outcome.Failure(
                    AppError.Conflict("الفاتورة ${current.invoiceNumber} معتمدة مسبقاً")
                )
            }
            if (current.status == InvoiceStatus.ARCHIVED) {
                return@guarded Outcome.Failure(
                    AppError.Conflict("الفاتورة ${current.invoiceNumber} مؤرشفة")
                )
            }
            if (current.items.isEmpty()) {
                return@guarded Outcome.Failure(
                    AppError.Validation("items", "لا يمكن اعتماد فاتورة بلا أسطر")
                )
            }

            val approver = userDao.findById(approverId)
                ?: return@guarded Outcome.Failure(AppError.NotFound("مستخدم", approverId))
            val approverRole = approver.toDomain().role
            if (!approverRole.canWriteFinancials) {
                return@guarded Outcome.Failure(
                    AppError.Permission(
                        "الدور $approverRole لا يملك صلاحية اعتماد الفواتير"
                    )
                )
            }

            val now = TimeSource.now()
            val approved = current.copy(
                status = InvoiceStatus.APPROVED,
                approvedByUserId = approverId,
                approvedAt = now,
                rejectionReason = null,
                updatedAt = now
            )
            db.withTransaction {
                invoiceDao.updateInvoice(approved.toEntity())
            }
            Outcome.Success(approved)
        }

    override suspend fun reject(
        invoiceId: String,
        approverId: String,
        reason: String
    ): Outcome<Invoice> = guarded {
        if (reason.isBlank()) {
            return@guarded Outcome.Failure(
                AppError.Validation("reason", "سبب الرفض مطلوب")
            )
        }
        val row = invoiceDao.findById(invoiceId)
            ?: return@guarded Outcome.Failure(AppError.NotFound("فاتورة", invoiceId))
        val current = rowToDomain(row)
        if (current.status == InvoiceStatus.APPROVED) {
            return@guarded Outcome.Failure(
                AppError.Conflict("لا يمكن رفض فاتورة معتمدة؛ يجب إصدار إشعار دائن")
            )
        }
        if (current.status == InvoiceStatus.ARCHIVED) {
            return@guarded Outcome.Failure(
                AppError.Conflict("الفاتورة ${current.invoiceNumber} مؤرشفة")
            )
        }
        val approver = userDao.findById(approverId)
            ?: return@guarded Outcome.Failure(AppError.NotFound("مستخدم", approverId))
        if (!approver.toDomain().role.canWriteFinancials) {
            return@guarded Outcome.Failure(
                AppError.Permission("لا تملك صلاحية مراجعة الفواتير")
            )
        }

        val now = TimeSource.now()
        val rejected = current.copy(
            status = InvoiceStatus.REJECTED,
            approvedByUserId = approverId,
            approvedAt = now,
            rejectionReason = reason,
            updatedAt = now
        )
        db.withTransaction { invoiceDao.updateInvoice(rejected.toEntity()) }
        Outcome.Success(rejected)
    }

    /** حذف المسودة فقط. الفاتورة المعتمدة تبقى أثراً محاسبياً دائماً. */
    override suspend fun deleteDraft(invoiceId: String): Outcome<Unit> = guarded {
        val row = invoiceDao.findById(invoiceId)
            ?: return@guarded Outcome.Failure(AppError.NotFound("فاتورة", invoiceId))
        val current = rowToDomain(row)
        if (!current.canDelete) {
            return@guarded Outcome.Failure(
                AppError.Conflict(
                    "لا يُحذف إلا المسودة؛ حالة الفاتورة الحالية ${current.status}"
                )
            )
        }
        db.withTransaction {
            invoiceDao.deleteItems(invoiceId)
            invoiceDao.updateInvoice(
                current.copy(status = InvoiceStatus.ARCHIVED, updatedAt = TimeSource.now())
                    .toEntity()
            )
        }
        Outcome.Success(Unit)
    }
}