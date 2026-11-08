package com.agency.app.core.data.repository

import com.agency.app.core.data.local.dao.BuyerDao
import com.agency.app.core.data.local.dao.InvoiceDao
import com.agency.app.core.data.local.dao.ReceiptDao
import com.agency.app.core.data.local.mapper.buildInvoice
import com.agency.app.core.data.local.mapper.toDomain
import com.agency.app.core.data.local.mapper.toEntity
import com.agency.app.core.data.local.mapper.toEpochMs
import com.agency.app.core.data.local.mapper.toInstant
import com.agency.app.core.domain.model.AppError
import com.agency.app.core.domain.model.Buyer
import com.agency.app.core.domain.model.BuyerBalance
import com.agency.app.core.domain.model.BuyerStatementEntry
import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.repository.BuyerRepository
import com.agency.app.core.util.TimeSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * تنفيذ المشترين وذممهم المالية.
 *
 * القاعدة المالية الحاكمة (من البرومبت):
 * «ذمة المشتري = مجموع الفواتير المعتمدة − مجموع سندات القبض»،
 * ولا تُحسب من أي مصدر آخر ولا تُخزَّن كرقم ثابت يمكن أن يفسد.
 */
@Singleton
class BuyerRepositoryImpl @Inject constructor(
    private val buyerDao: BuyerDao,
    private val invoiceDao: InvoiceDao,
    private val receiptDao: ReceiptDao
) : BuyerRepository {

    override fun observeBuyers(): Flow<List<Buyer>> =
        buyerDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeBuyer(id: String): Flow<Buyer?> =
        buyerDao.observeById(id).map { it?.toDomain() }

    override suspend fun getBuyer(id: String): Outcome<Buyer> = guarded {
        val entity = buyerDao.findById(id)
            ?: return@guarded Outcome.Failure(AppError.NotFound("مشترٍ", id))
        Outcome.Success(entity.toDomain())
    }

    override suspend fun upsertBuyer(buyer: Buyer): Outcome<Unit> = guarded {
        if (buyer.name.isBlank()) {
            return@guarded Outcome.Failure(
                AppError.Validation("name", "اسم المشتري مطلوب")
            )
        }
        if (buyer.phone.isBlank()) {
            return@guarded Outcome.Failure(
                AppError.Validation("phone", "رقم الجوال مطلوب")
            )
        }
        if (buyer.creditLimit.cents < 0L) {
            return@guarded Outcome.Failure(
                AppError.Validation("creditLimit", "الحد الائتماني لا يكون سالباً")
            )
        }
        buyerDao.upsert(buyer.toEntity())
        Outcome.Success(Unit)
    }

    override suspend fun setActive(buyerId: String, active: Boolean): Outcome<Unit> = guarded {
        val entity = buyerDao.findById(buyerId)
            ?: return@guarded Outcome.Failure(AppError.NotFound("مشترٍ", buyerId))
        buyerDao.upsert(entity.copy(isActive = active, updatedAt = TimeSource.now().toEpochMs()))
        Outcome.Success(Unit)
    }

    /**
     * ذمة مشترٍ واحد — تُحسب حساباً حياً من مصدر الحقيقة، لا من حقل مخزَّن.
     * تُعدّ الفواتير المعتمدة فقط هي المدينة، والسندات كلها دائنة.
     */
    override fun observeBalance(buyerId: String): Flow<BuyerBalance> = combine(
        buyerDao.observeById(buyerId),
        invoiceDao.observeByBuyer(buyerId),
        receiptDao.observeByBuyer(buyerId)
    ) { buyerEntity, invoices, receipts ->
        val buyer = buyerEntity?.toDomain()
        val invoiced = invoices
            .map { buildInvoice(it.invoice, it.items) }
            .filter { it.status == InvoiceStatus.APPROVED }
            .fold(Money.ZERO) { acc, invoice -> acc + invoice.grandTotal }
        val paid = receipts.fold(Money.ZERO) { acc, row -> acc + Money(row.amountCents) }
        BuyerBalance(
            buyerId = buyerId,
            buyerName = buyer?.name ?: "—",
            totalInvoiced = invoiced,
            totalPaid = paid,
            creditLimit = buyer?.creditLimit ?: Money.ZERO
        )
    }

    /** ذمم كل المشترين في مرور تجميعي واحد (لشاشة المحاسب). */
    override fun observeBalances(): Flow<List<BuyerBalance>> =
        combine(
            buyerDao.observeAll(),
            invoiceDao.observeAllDetailed(),
            receiptDao.observeAll()
        ) { buyerRows, invoiceRows, receiptRows ->
            val invoicedByBuyer = HashMap<String, Long>(buyerRows.size)
            invoiceRows.forEach { row ->
                val invoice = buildInvoice(row.invoice, row.items)
                if (invoice.status == InvoiceStatus.APPROVED) {
                    invoicedByBuyer[invoice.buyerId] =
                        (invoicedByBuyer[invoice.buyerId] ?: 0L) + invoice.grandTotal.cents
                }
            }
            val paidByBuyer = HashMap<String, Long>(buyerRows.size)
            receiptRows.forEach { row ->
                paidByBuyer[row.buyerId] = (paidByBuyer[row.buyerId] ?: 0L) + row.amountCents
            }
            buyerRows.map { entity ->
                val buyer = entity.toDomain()
                BuyerBalance(
                    buyerId = buyer.id,
                    buyerName = buyer.name,
                    totalInvoiced = Money(invoicedByBuyer[buyer.id] ?: 0L),
                    totalPaid = Money(paidByBuyer[buyer.id] ?: 0L),
                    creditLimit = buyer.creditLimit
                )
            }.sortedByDescending { it.outstanding.cents }
        }

    /**
     * كشف حساب المشتري: ندمج المدين (الفواتير المعتمدة) والدائن (سندات القبض)
     * مرتبين زمنياً، ثم نحسب الرصيد التراكمي بمرور واحد، ونعرض الأحدث أولاً.
     */
    override fun observeStatement(buyerId: String): Flow<List<BuyerStatementEntry>> =
        combine(
            buyerDao.observeById(buyerId),
            invoiceDao.observeByBuyer(buyerId),
            receiptDao.observeByBuyer(buyerId)
        ) { buyerEntity, invoiceRows, receiptRows ->
            val buyerName = buyerEntity?.toDomain()?.name ?: "—"
            val debits = invoiceRows
                .map { buildInvoice(it.invoice, it.items) }
                .filter { it.status == InvoiceStatus.APPROVED }
                .map { invoice ->
                    StatementSeed(
                        id = invoice.id,
                        referenceId = invoice.id,
                        description = "فاتورة ${invoice.invoiceNumber} — $buyerName",
                        debit = invoice.grandTotal,
                        credit = Money.ZERO,
                        atMs = invoice.createdAt.toEpochMs()
                    )
                }
            val credits = receiptRows.map { row ->
                val receipt = row.toDomain()
                StatementSeed(
                    id = receipt.id,
                    referenceId = receipt.id,
                    description = "سند قبض ${receipt.receiptNumber}",
                    debit = Money.ZERO,
                    credit = receipt.amount,
                    atMs = receipt.collectedAt.toEpochMs()
                )
            }
            var running = 0L
            (debits + credits)
                .sortedBy { it.atMs }
                .map { seed ->
                    running += seed.debit.cents - seed.credit.cents
                    BuyerStatementEntry(
                        id = seed.id,
                        buyerId = buyerId,
                        description = seed.description,
                        debit = seed.debit,
                        credit = seed.credit,
                        balanceAfter = Money(running),
                        at = seed.atMs.toInstant(),
                        referenceId = seed.referenceId
                    )
                }
                .sortedByDescending { it.at }
        }

    /** صف وسيط يُبنى منه كشف الحساب قبل حساب الرصيد التراكمي. */
    private data class StatementSeed(
        val id: String,
        val referenceId: String?,
        val description: String,
        val debit: Money,
        val credit: Money,
        val atMs: Long
    )
}
