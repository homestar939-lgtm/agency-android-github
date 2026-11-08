package com.agency.app.core.data.repository

import androidx.room.withTransaction
import com.agency.app.core.data.local.AppDatabase
import com.agency.app.core.data.local.dao.OrderDao
import com.agency.app.core.data.local.dao.ProductDao
import com.agency.app.core.data.local.mapper.buildOrder
import com.agency.app.core.data.local.mapper.toDomain
import com.agency.app.core.data.local.mapper.toEntity
import com.agency.app.core.domain.model.AppError
import com.agency.app.core.domain.model.Order
import com.agency.app.core.domain.model.OrderItem
import com.agency.app.core.domain.model.OrderStatus
import com.agency.app.core.domain.model.OrderTimeline
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.repository.NewOrderLine
import com.agency.app.core.domain.repository.OrderRepository
import com.agency.app.core.util.Ids
import com.agency.app.core.util.TimeSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * تنفيذ الطلبيات.
 *
 * قاعدتان لا تُخترقان:
 * 1) إنشاء الطلبية وخصم المخزون يحدثان في معاملة واحدة ([withTransaction])،
 *    فلا توجد طلبية بلا خصم ولا خصم بلا طلبية.
 * 2) الانتقال بين الحالات يطيع [OrderStatus.allowedNext] فقط؛ أي قفز غير
 *    مسموح يُرفض بـ [AppError.Validation] ولا يُكتب في القاعدة.
 */
@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val orderDao: OrderDao,
    private val productDao: ProductDao
) : OrderRepository {

    private fun detailedToDomain(row: com.agency.app.core.data.local.dao.OrderWithDetails): Order =
        buildOrder(row.order, row.items, row.timeline)

    override fun observeOrders(): Flow<List<Order>> =
        orderDao.observeAllDetailed().map { rows -> rows.map { detailedToDomain(it) } }

    override fun observeByStatus(status: OrderStatus): Flow<List<Order>> =
        orderDao.observeByStatus(status.name).map { rows -> rows.map { detailedToDomain(it) } }

    override fun observeByBuyer(buyerId: String): Flow<List<Order>> =
        orderDao.observeByBuyer(buyerId).map { rows -> rows.map { detailedToDomain(it) } }

    override fun observeOrder(id: String): Flow<Order?> =
        orderDao.observeById(id).map { row -> row?.let { detailedToDomain(it) } }

    override suspend fun getOrder(id: String): Outcome<Order> = guarded {
        val row = orderDao.findById(id)
            ?: return@guarded Outcome.Failure(AppError.NotFound("طلبية", id))
        Outcome.Success(detailedToDomain(row))
    }

    /**
     * إنشاء طلبية جديدة بشكل ذرّي:
     * 1) التحقق من الأسطر ومن المخزون المتاح لكل منتج،
     * 2) تجميد سعر واسم ووحدة المنتج وقت الطلب،
     * 3) كتابة الطلبية + أسطرها + أول حدث في الخط الزمني،
     * 4) خصم المخزون.
     */
    override suspend fun createOrder(
        buyerId: String,
        lines: List<NewOrderLine>,
        actorId: String,
        actorName: String,
        note: String?
    ): Outcome<Order> = guarded {
        if (lines.isEmpty()) {
            return@guarded Outcome.Failure(
                AppError.Validation("lines", "لا يمكن إنشاء طلبية بلا أسطر")
            )
        }
        if (buyerId.isBlank()) {
            return@guarded Outcome.Failure(AppError.Validation("buyerId", "المشتري مطلوب"))
        }

        // تجميع الكميات لكل منتج حتى لا يُفحص المخزون مرتين بكميتين منفصلتين.
        val requested = LinkedHashMap<String, Int>()
        lines.forEach { line ->
            if (line.quantity <= 0) {
                return@guarded Outcome.Failure(
                    AppError.Validation("quantity", "كمية السطر يجب أن تكون أكبر من صفر")
                )
            }
            requested[line.productId] = (requested[line.productId] ?: 0) + line.quantity
        }

        val products = HashMap<String, com.agency.app.core.domain.model.Product>(requested.size)
        requested.forEach { (productId, quantity) ->
            val entity = productDao.findById(productId)
                ?: return@guarded Outcome.Failure(AppError.NotFound("منتج", productId))
            val product = entity.toDomain()
            if (!product.isActive) {
                return@guarded Outcome.Failure(
                    AppError.Conflict("المنتج «${product.name}» غير متاح للبيع حالياً")
                )
            }
            if (quantity > product.stockQuantity) {
                return@guarded Outcome.Failure(
                    AppError.Conflict(
                        "المخزون غير كافٍ للمنتج «${product.name}»: المطلوب $quantity والمتاح ${product.stockQuantity}"
                    )
                )
            }
            products[productId] = product
        }

        val now = TimeSource.now()
        val orderId = Ids.new()
        val orderNumber = Ids.orderNumber()

        // الأسطر: كل سطر يأخذ نسخة مجمّدة من بيانات المنتج وقت الطلب.
        val items = lines.mapIndexed { index, line ->
            val product = products.getValue(line.productId)
            OrderItem(
                id = "$orderId-i${index + 1}",
                orderId = orderId,
                productId = product.id,
                productName = product.name,
                unit = product.unit,
                unitPrice = product.price,
                quantity = line.quantity,
                discount = line.discount
            )
        }

        val header = Order(
            id = orderId,
            orderNumber = orderNumber,
            buyerId = buyerId,
            buyerName = buyerDaoName(buyerId),
            status = OrderStatus.NEW,
            items = items,
            timeline = listOf(
                OrderTimeline(
                    status = OrderStatus.NEW,
                    at = now,
                    actorId = actorId,
                    actorName = actorName,
                    note = note
                )
            ),
            note = note,
            createdAt = now,
            updatedAt = now
        )

        db.withTransaction {
            orderDao.upsertDetailed(
                order = header.toEntity(),
                items = items.map { it.toEntity() },
                timeline = header.timeline.first().toEntity(orderId, "${orderId}-t1")
            )
            requested.forEach { (productId, quantity) ->
                productDao.decrementStock(
                    id = productId,
                    quantity = quantity,
                    at = now.toEpochMilliseconds()
                )
            }
        }

        Outcome.Success(header)
    }

    private suspend fun buyerDaoName(buyerId: String): String {
        // اسم المشتري يُخزَّن منسوخاً على الطلبية، لكنه يُقرأ من جدول المشترين
        // عند الإنشاء فقط، حفاظاً على ظهور الاسم التاريخي في التقارير.
        return db.buyerDao().findById(buyerId)?.name ?: "—"
    }

    /**
     * الانتقال بين الحالات. يُرفض أي انتقال خارج الجدول المسموح،
     * وكل انتقال ناجح يضيف سطراً جديداً في الخط الزمني.
     */
    override suspend fun transition(
        orderId: String,
        target: OrderStatus,
        actorId: String,
        actorName: String,
        note: String?
    ): Outcome<Order> = guarded {
        val row = orderDao.findById(orderId)
            ?: return@guarded Outcome.Failure(AppError.NotFound("طلبية", orderId))
        val current = detailedToDomain(row)

        if (current.status.isFinal) {
            return@guarded Outcome.Failure(
                AppError.Conflict("الطلبية ${current.orderNumber} منتهية ولا تقبل تغيير الحالة")
            )
        }
        if (target == current.status) {
            return@guarded Outcome.Failure(
                AppError.Validation("status", "الطلبية في هذه الحالة بالفعل")
            )
        }
        if (target !in current.status.allowedNext) {
            return@guarded Outcome.Failure(
                AppError.Validation(
                    "status",
                    "انتقال غير مسموح من ${current.status} إلى $target"
                )
            )
        }

        val now = TimeSource.now()
        val entry = OrderTimeline(
            status = target,
            at = now,
            actorId = actorId,
            actorName = actorName,
            note = note
        )
        val updated = current.copy(status = target, timeline = current.timeline + entry, updatedAt = now)

        db.withTransaction {
            orderDao.updateStatus(orderId, target.name, now.toEpochMilliseconds())
            orderDao.insertTimeline(entry.toEntity(orderId, Ids.new()))
            // الإلغاء يعيد الكميات إلى المخزون حتى لا يضيع رصيد الوكالة.
            if (target == OrderStatus.CANCELLED) {
                current.items.forEach { item ->
                    val product = productDao.findById(item.productId)
                    if (product != null) {
                        productDao.updateStock(
                            id = product.id,
                            quantity = product.stockQuantity + item.quantity,
                            at = now.toEpochMilliseconds()
                        )
                    }
                }
            }
        }

        Outcome.Success(updated.copy(timeline = updated.timeline.sortedBy { it.at }))
    }

    override suspend fun countByStatus(status: OrderStatus): Outcome<Int> = guarded {
        Outcome.Success(orderDao.countByStatus(status.name))
    }
}