package com.agency.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.agency.app.core.data.local.entity.BuyerEntity
import com.agency.app.core.data.local.entity.InvoiceEntity
import com.agency.app.core.data.local.entity.InvoiceItemEntity
import com.agency.app.core.data.local.entity.OrderEntity
import com.agency.app.core.data.local.entity.OrderItemEntity
import com.agency.app.core.data.local.entity.OrderTimelineEntity
import com.agency.app.core.data.local.entity.PermissionRequestEntity
import com.agency.app.core.data.local.entity.ProductEntity
import com.agency.app.core.data.local.entity.ReceiptEntity
import com.agency.app.core.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/** العلاقة الكاملة لطلبية واحدة مع أسطرها وخطها الزمني. */
data class OrderWithDetails(
    @androidx.room.Embedded val order: OrderEntity,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "orderId")
    val items: List<OrderItemEntity>,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "orderId")
    val timeline: List<OrderTimelineEntity>
)

/** العلاقة الكاملة لفاتورة واحدة مع أسطرها. */
data class InvoiceWithDetails(
    @androidx.room.Embedded val invoice: InvoiceEntity,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "invoiceId")
    val items: List<InvoiceItemEntity>
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY displayName ASC")
    fun observeAll(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("UPDATE users SET lastLoginAt = :at WHERE id = :id")
    suspend fun touchLastLogin(id: String, at: Long)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int
}

@Dao
interface BuyerDao {
    @Query("SELECT * FROM buyers ORDER BY name ASC")
    fun observeAll(): Flow<List<BuyerEntity>>

    @Query("SELECT * FROM buyers WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<BuyerEntity?>

    @Query("SELECT * FROM buyers WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): BuyerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(buyer: BuyerEntity)

    @Query("SELECT COUNT(*) FROM buyers")
    suspend fun count(): Int
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun observeActive(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(products: List<ProductEntity>)

    @Query("UPDATE products SET stockQuantity = :quantity, updatedAt = :at WHERE id = :id")
    suspend fun updateStock(id: String, quantity: Int, at: Long)

    @Query("UPDATE products SET stockQuantity = stockQuantity - :quantity, updatedAt = :at WHERE id = :id")
    suspend fun decrementStock(id: String, quantity: Int, at: Long)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int
}

@Dao
interface OrderDao {
    @Transaction
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun observeAllDetailed(): Flow<List<OrderWithDetails>>

    @Transaction
    @Query("SELECT * FROM orders WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: String): Flow<List<OrderWithDetails>>

    @Transaction
    @Query("SELECT * FROM orders WHERE buyerId = :buyerId ORDER BY createdAt DESC")
    fun observeByBuyer(buyerId: String): Flow<List<OrderWithDetails>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<OrderWithDetails?>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): OrderWithDetails?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun itemsOf(orderId: String): List<OrderItemEntity>

    @Query("UPDATE orders SET status = :status, updatedAt = :at WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, at: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<OrderItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeline(entry: OrderTimelineEntity)

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM orders WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    @Transaction
    suspend fun upsertDetailed(
        order: OrderEntity,
        items: List<OrderItemEntity>,
        timeline: OrderTimelineEntity
    ) {
        upsertOrder(order)
        if (items.isNotEmpty()) upsertItems(items)
        insertTimeline(timeline)
    }
}

@Dao
interface InvoiceDao {
    @Transaction
    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun observeAllDetailed(): Flow<List<InvoiceWithDetails>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: String): Flow<List<InvoiceWithDetails>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE buyerId = :buyerId ORDER BY createdAt DESC")
    fun observeByBuyer(buyerId: String): Flow<List<InvoiceWithDetails>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<InvoiceWithDetails?>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): InvoiceWithDetails?

    @Transaction
    @Query("SELECT * FROM invoices WHERE orderId = :orderId LIMIT 1")
    suspend fun findByOrderId(orderId: String): InvoiceWithDetails?

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun itemsOf(invoiceId: String): List<InvoiceItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<InvoiceItemEntity>)

    @Update
    suspend fun updateInvoice(invoice: InvoiceEntity)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteItems(invoiceId: String)

    @Query("SELECT COUNT(*) FROM invoices")
    suspend fun count(): Int

    @Transaction
    suspend fun upsertDetailed(invoice: InvoiceEntity, items: List<InvoiceItemEntity>) {
        upsertInvoice(invoice)
        deleteItems(invoice.id)
        if (items.isNotEmpty()) upsertItems(items)
    }
}

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY collectedAt DESC")
    fun observeAll(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE buyerId = :buyerId ORDER BY collectedAt DESC")
    fun observeByBuyer(buyerId: String): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ReceiptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(receipt: ReceiptEntity)

    @Query("UPDATE receipts SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM receipts WHERE buyerId = :buyerId")
    suspend fun totalCollectedForBuyer(buyerId: String): Long

    @Query("SELECT COALESCE(SUM(amountCents), 0) FROM receipts WHERE collectedAt BETWEEN :from AND :to")
    fun observeCollectedBetween(from: Long, to: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM receipts")
    suspend fun count(): Int
}

@Dao
interface PermissionDao {
    @Query("SELECT * FROM permission_requests ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PermissionRequestEntity>>

    @Query("SELECT * FROM permission_requests WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: String): Flow<List<PermissionRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(request: PermissionRequestEntity)

    @Query("SELECT * FROM permission_requests WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PermissionRequestEntity?

    @Query("SELECT COUNT(*) FROM permission_requests WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>
}

@Dao
interface SyncEventDao {
    @Query("SELECT eventId FROM sync_events")
    suspend fun allEventIds(): List<String>

    @Query("SELECT COUNT(*) FROM sync_events WHERE eventId = :eventId")
    suspend fun exists(eventId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: com.agency.app.core.data.local.entity.SyncEventEntity): Long

    @Query("UPDATE sync_events SET applied = 1 WHERE eventId = :eventId")
    suspend fun markApplied(eventId: String)

    @Query("SELECT * FROM sync_events WHERE applied = 0 ORDER BY receivedAt ASC")
    suspend fun pending(): List<com.agency.app.core.data.local.entity.SyncEventEntity>
}