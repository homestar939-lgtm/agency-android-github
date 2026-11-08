package com.agency.app.core.data.sync

import com.agency.app.core.data.local.dao.BuyerDao
import com.agency.app.core.data.local.dao.InvoiceDao
import com.agency.app.core.data.local.dao.OrderDao
import com.agency.app.core.data.local.dao.ProductDao
import com.agency.app.core.data.local.dao.ReceiptDao
import com.agency.app.core.data.local.dao.SyncEventDao
import com.agency.app.core.data.local.entity.SyncEventEntity
import com.agency.app.core.domain.model.AppError
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.PaymentMethod
import com.agency.app.core.util.TimeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * مستقبل أحداث المزامنة.
 *
 * القاعدة (من البرومبت): كل حدث قادم من الشبكة يُكتب في Room أولاً،
 * ثم تتفرّع التحديثات إلى الواجهة عبر `Flow`. لا تقرأ الواجهة من الشبكة أبداً.
 *
 * الضمانة الثانية: كل حدث يحمل `eventId` فريداً، فيُسجَّل في `sync_events`
 * قبل التطبيق، وتكرار وصول الحدث (إعادة إرسال، انقطاع شبكة، إعادة اتصال)
 * لا يُنتج أثراً مزدوجاً.
 */
class SyncManager(
    private val syncEventDao: SyncEventDao,
    private val productDao: ProductDao,
    private val buyerDao: BuyerDao,
    private val orderDao: OrderDao,
    private val invoiceDao: InvoiceDao,
    private val receiptDao: ReceiptDao
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private val _applied = MutableStateFlow(0)
    val appliedCount: StateFlow<Int> = _applied.asStateFlow()

    /**
     * يستقبل حدثاً خاماً (نص JSON) من طبقة WebSocket.
     * يُعيد [Outcome.Success] حتى لو كان الحدث مكرراً — التكرار ليس خطأً.
     */
    suspend fun ingest(rawJson: String): Outcome<Unit> {
        val envelope = try {
            json.decodeFromString<SyncEnvelope>(rawJson)
        } catch (e: Exception) {
            return Outcome.Failure(
                AppError.Validation("payload", "حدث مزامنة غير صالح: ${e.message}")
            )
        }
        return ingest(envelope)
    }

    suspend fun ingest(envelope: SyncEnvelope): Outcome<Unit> {
        if (envelope.eventId.isBlank()) {
            return Outcome.Failure(AppError.Validation("eventId", "الحدث بلا معرّف"))
        }
        // التسجيل أولاً بمفتاح أساسي = eventId؛ الإدخال المكرر يُهمَل بصمت.
        syncEventDao.insert(
            SyncEventEntity(
                eventId = envelope.eventId,
                type = envelope.type,
                payload = envelope.payload,
                receivedAt = TimeSource.now().toEpochMilliseconds(),
                applied = false
            )
        )
        _state.value = SyncState.Receiving
        return guardedSync {
            apply(envelope)
            syncEventDao.markApplied(envelope.eventId)
            _applied.value = _applied.value + 1
            _state.value = SyncState.Idle
            Outcome.Success(Unit)
        }
    }

    /** يعيد تطبيق الأحداث غير المطبَّقة (بعد انقطاع أو إغلاق مفاجئ). */
    suspend fun replayPending(): Outcome<Int> = guardedSync {
        val pending = syncEventDao.pending()
        var count = 0
        pending.forEach { entity ->
            val envelope = SyncEnvelope(entity.eventId, entity.type, entity.payload)
            val result = apply(envelope)
            if (result is Outcome.Success) {
                syncEventDao.markApplied(entity.eventId)
                count += 1
            }
        }
        _applied.value = _applied.value + count
        Outcome.Success(count)
    }

    private suspend fun apply(envelope: SyncEnvelope): Outcome<Unit> {
        return when (envelope.type) {
            TYPE_PRODUCT_UPSERT -> {
                val dto = json.decodeFromString<ProductDto>(envelope.payload)
                productDao.upsert(
                    com.agency.app.core.data.local.entity.ProductEntity(
                        id = dto.id,
                        name = dto.name,
                        sku = dto.sku,
                        unit = dto.unit,
                        priceCents = dto.priceCents,
                        costCents = dto.costCents,
                        stockQuantity = dto.stockQuantity,
                        minOrderQuantity = dto.minOrderQuantity,
                        expiryDate = dto.expiryDate,
                        categoryId = dto.categoryId,
                        imageUrl = dto.imageUrl,
                        isActive = dto.isActive,
                        showPrice = dto.showPrice,
                        showUnit = dto.showUnit,
                        showExpiry = dto.showExpiry,
                        showMinOrder = dto.showMinOrder,
                        showStock = dto.showStock,
                        updatedAt = dto.updatedAt
                    )
                )
                Outcome.Success(Unit)
            }

            TYPE_BUYER_UPSERT -> {
                val dto = json.decodeFromString<BuyerDto>(envelope.payload)
                buyerDao.upsert(
                    com.agency.app.core.data.local.entity.BuyerEntity(
                        id = dto.id,
                        name = dto.name,
                        phone = dto.phone,
                        address = dto.address,
                        shopName = dto.shopName,
                        creditLimitCents = dto.creditLimitCents,
                        isActive = dto.isActive,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt
                    )
                )
                Outcome.Success(Unit)
            }

            TYPE_ORDER_STATUS -> {
                val dto = json.decodeFromString<OrderStatusDto>(envelope.payload)
                val current = orderDao.findById(dto.orderId)
                    ?: return Outcome.Failure(AppError.NotFound("طلبية", dto.orderId))
                val order = com.agency.app.core.data.local.mapper.buildOrder(
                    current.order, current.items, current.timeline
                )
                // نطبّق الانتقال فقط إذا كان مسموحاً من حالتنا المحلية؛
                // الأحداث القديمة أو المتأخرة تُتجاهل بدل إفساد الحالة.
                if (dto.status !in order.status.allowedNext.map { it.name }) {
                    return Outcome.Success(Unit)
                }
                orderDao.updateStatus(dto.orderId, dto.status, dto.at)
                Outcome.Success(Unit)
            }

            TYPE_INVOICE_STATUS -> {
                val dto = json.decodeFromString<InvoiceStatusDto>(envelope.payload)
                val current = invoiceDao.findById(dto.invoiceId)
                    ?: return Outcome.Failure(AppError.NotFound("فاتورة", dto.invoiceId))
                invoiceDao.updateInvoice(
                    current.invoice.copy(status = dto.status, updatedAt = dto.at)
                )
                Outcome.Success(Unit)
            }

            TYPE_RECEIPT_CREATED -> {
                val dto = json.decodeFromString<ReceiptDto>(envelope.payload)
                receiptDao.upsert(
                    com.agency.app.core.data.local.entity.ReceiptEntity(
                        id = dto.id,
                        receiptNumber = dto.receiptNumber,
                        buyerId = dto.buyerId,
                        buyerName = dto.buyerName,
                        amountCents = dto.amountCents,
                        method = dto.method,
                        invoiceId = dto.invoiceId,
                        reference = dto.reference,
                        note = dto.note,
                        collectorUserId = dto.collectorUserId,
                        collectorName = dto.collectorName,
                        collectedAt = dto.collectedAt,
                        isSigned = dto.isSigned,
                        signaturePath = dto.signaturePath,
                        isSynced = true,
                        createdAt = dto.createdAt
                    )
                )
                Outcome.Success(Unit)
            }

            else -> Outcome.Failure(
                AppError.Validation("type", "نوع حدث غير معروف: ${envelope.type}")
            )
        }
    }

    private inline fun <T> guardedSync(block: () -> Outcome<T>): Outcome<T> {
        return try {
            block()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = SyncState.Failed(e.message ?: "خطأ مزامنة")
            Outcome.Failure(AppError.Unknown(e))
        }
    }

    private companion object {
        const val TYPE_PRODUCT_UPSERT = "PRODUCT_UPSERT"
        const val TYPE_BUYER_UPSERT = "BUYER_UPSERT"
        const val TYPE_ORDER_STATUS = "ORDER_STATUS"
        const val TYPE_INVOICE_STATUS = "INVOICE_STATUS"
        const val TYPE_RECEIPT_CREATED = "RECEIPT_CREATED"
    }
}

/** حالة قناة المزامنة كما تعرضها شريط الحالة في الواجهة. */
sealed interface SyncState {
    data object Idle : SyncState
    data object Receiving : SyncState
    data class Failed(val message: String) : SyncState
}

/** مغلّف الحدث القادم من WebSocket. */
@Serializable
data class SyncEnvelope(
    val eventId: String,
    val type: String,
    val payload: String
)

@Serializable
private data class ProductDto(
    val id: String,
    val name: String,
    val sku: String,
    val unit: String,
    val priceCents: Long,
    val costCents: Long = 0L,
    val stockQuantity: Int = 0,
    val minOrderQuantity: Int = 1,
    val expiryDate: Long? = null,
    val categoryId: String? = null,
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val showPrice: Boolean = true,
    val showUnit: Boolean = true,
    val showExpiry: Boolean = true,
    val showMinOrder: Boolean = true,
    val showStock: Boolean = false,
    val updatedAt: Long
)

@Serializable
private data class BuyerDto(
    val id: String,
    val name: String,
    val phone: String,
    val address: String? = null,
    val shopName: String? = null,
    val creditLimitCents: Long = 0L,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
private data class OrderStatusDto(
    val orderId: String,
    val status: String,
    val at: Long
)

@Serializable
private data class InvoiceStatusDto(
    val invoiceId: String,
    val status: String,
    val at: Long
)

@Serializable
private data class ReceiptDto(
    val id: String,
    val receiptNumber: String,
    val buyerId: String,
    val buyerName: String,
    val amountCents: Long,
    val method: String = PaymentMethod.CASH.name,
    val invoiceId: String? = null,
    val reference: String? = null,
    val note: String? = null,
    val collectorUserId: String,
    val collectorName: String,
    val collectedAt: Long,
    val isSigned: Boolean = false,
    val signaturePath: String? = null,
    val createdAt: Long
)