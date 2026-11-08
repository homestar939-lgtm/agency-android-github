package com.agency.app.core.data.repository

import androidx.room.withTransaction
import com.agency.app.core.data.local.AppDatabase
import com.agency.app.core.data.local.dao.BuyerDao
import com.agency.app.core.data.local.dao.InvoiceDao
import com.agency.app.core.data.local.dao.ReceiptDao
import com.agency.app.core.data.local.mapper.toDomain
import com.agency.app.core.data.local.mapper.toEntity
import com.agency.app.core.domain.model.AppError
import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.PaymentMethod
import com.agency.app.core.domain.model.Receipt
import com.agency.app.core.domain.repository.ReceiptRepository
import com.agency.app.core.util.Ids
import com.agency.app.core.util.TimeSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * تنفيذ سندات القبض.
 *
 * سند القبض هو المسار الوحيد لتخفيض ذمّة المشتري. لا نكتب رصيداً في أي جدول؛
 * نحن نكتب السند فقط، و[BuyerRepositoryImpl.observeBalance] يشتق الرصيد تلقائياً.
 */
@Singleton
class ReceiptRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val receiptDao: ReceiptDao,
    private val buyerDao: BuyerDao,
    private val invoiceDao: InvoiceDao
) : ReceiptRepository {

    override fun observeReceipts(): Flow<List<Receipt>> =
        receiptDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeByBuyer(buyerId: String): Flow<List<Receipt>> =
        receiptDao.observeByBuyer(buyerId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getReceipt(id: String): Outcome<Receipt> = guarded {
        val entity = receiptDao.findById(id)
            ?: return@guarded Outcome.Failure(AppError.NotFound("سند قبض", id))
        Outcome.Success(entity.toDomain())
    }

    /**
     * تسجيل التحصيل.
     * التحققات: المشتري موجود ونشط، المبلغ موجب، والفواتير المرتبطة (إن وُجدت)
     * تخص نفس المشتري ومعتمدة — ثم يُكتب السند في معاملة واحدة.
     */
    override suspend fun collect(
        buyerId: String,
        amount: Money,
        method: PaymentMethod,
        collectorUserId: String,
        collectorName: String,
        invoiceId: String?,
        reference: String?,
        note: String?,
        isSigned: Boolean,
        signaturePath: String?
    ): Outcome<Receipt> = guarded {
        if (amount.cents <= 0L) {
            return@guarded Outcome.Failure(
                AppError.Validation("amount", "مبلغ التحصيل يجب أن يكون أكبر من صفر")
            )
        }
        val buyerEntity = buyerDao.findById(buyerId)
            ?: return@guarded Outcome.Failure(AppError.NotFound("مشترٍ", buyerId))
        val buyer = buyerEntity.toDomain()
        if (!buyer.isActive) {
            return@guarded Outcome.Failure(
                AppError.Conflict("المشتري «${buyer.name}» موقوف ولا يقبل تحصيلاً")
            )
        }
        if (collectorUserId.isBlank()) {
            return@guarded Outcome.Failure(
                AppError.Validation("collector", "هوية المحصّل مطلوبة لتسجيل السند")
            )
        }
        if (invoiceId != null) {
            val invoiceRow = invoiceDao.findById(invoiceId)
                ?: return@guarded Outcome.Failure(AppError.NotFound("فاتورة", invoiceId))
            val invoice = com.agency.app.core.data.local.mapper.buildInvoice(
                invoiceRow.invoice,
                invoiceRow.items
            )
            if (invoice.buyerId != buyerId) {
                return@guarded Outcome.Failure(
                    AppError.Validation("invoiceId", "الفاتورة لا تخص هذا المشتري")
                )
            }
            if (invoice.status != InvoiceStatus.APPROVED) {
                return@guarded Outcome.Failure(
                    AppError.Validation("invoiceId", "لا يُحصَّل على فاتورة غير معتمدة")
                )
            }
        }

        val now = TimeSource.now()
        val receipt = Receipt(
            id = Ids.new(),
            receiptNumber = Ids.receiptNumber(),
            buyerId = buyerId,
            buyerName = buyer.name,
            amount = amount,
            method = method,
            invoiceId = invoiceId,
            reference = reference,
            note = note,
            collectorUserId = collectorUserId,
            collectorName = collectorName,
            collectedAt = now,
            isSigned = isSigned,
            signaturePath = signaturePath,
            isSynced = false,
            createdAt = now
        )

        db.withTransaction { receiptDao.upsert(receipt.toEntity()) }
        Outcome.Success(receipt)
    }

    override suspend fun totalCollected(buyerId: String): Outcome<Money> = guarded {
        Outcome.Success(Money(receiptDao.totalCollectedForBuyer(buyerId)))
    }

    override suspend fun markSynced(receiptId: String): Outcome<Unit> = guarded {
        receiptDao.findById(receiptId)
            ?: return@guarded Outcome.Failure(AppError.NotFound("سند قبض", receiptId))
        receiptDao.markSynced(receiptId)
        Outcome.Success(Unit)
    }
}