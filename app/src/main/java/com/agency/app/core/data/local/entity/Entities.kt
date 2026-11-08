package com.agency.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** مستخدمون محليون — تُخزَّن كلمة المرور مُهشَّرة فقط. */
@Entity(tableName = "users", indices = [Index("username", unique = true)])
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val passwordHash: String,
    val displayName: String,
    val role: String,
    val phone: String?,
    val isActive: Boolean,
    val buyerId: String?,
    val createdAt: Long,
    val lastLoginAt: Long?
)

@Entity(tableName = "buyers")
data class BuyerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val address: String?,
    val shopName: String?,
    val creditLimitCents: Long,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "products", indices = [Index("sku", unique = true)])
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sku: String,
    val unit: String,
    val priceCents: Long,
    val costCents: Long,
    val stockQuantity: Int,
    val minOrderQuantity: Int,
    val expiryDate: Long?,
    val categoryId: String?,
    val imageUrl: String?,
    val isActive: Boolean,
    val showPrice: Boolean,
    val showUnit: Boolean,
    val showExpiry: Boolean,
    val showMinOrder: Boolean,
    val showStock: Boolean,
    val updatedAt: Long
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val buyerId: String,
    val buyerName: String,
    val status: String,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val productId: String,
    val productName: String,
    val unit: String,
    val unitPriceCents: Long,
    val quantity: Int,
    val discountCents: Long
)

@Entity(
    tableName = "order_timeline",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class OrderTimelineEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val status: String,
    val at: Long,
    val actorId: String?,
    val actorName: String,
    val note: String?
)

@Entity(tableName = "invoices", indices = [Index(value = ["orderId"])])
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val invoiceNumber: String,
    val orderId: String,
    val buyerId: String,
    val buyerName: String,
    val status: String,
    val commissionCents: Long,
    val note: String?,
    val createdByUserId: String,
    val approvedByUserId: String?,
    val approvedAt: Long?,
    val rejectionReason: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("invoiceId")]
)
data class InvoiceItemEntity(
    @PrimaryKey val id: String,
    val invoiceId: String,
    val productId: String,
    val productName: String,
    val unit: String,
    val unitPriceCents: Long,
    val quantity: Int,
    val discountCents: Long
)

@Entity(tableName = "receipts", indices = [Index("buyerId")])
data class ReceiptEntity(
    @PrimaryKey val id: String,
    val receiptNumber: String,
    val buyerId: String,
    val buyerName: String,
    val amountCents: Long,
    val method: String,
    val invoiceId: String?,
    val reference: String?,
    val note: String?,
    val collectorUserId: String,
    val collectorName: String,
    val collectedAt: Long,
    val isSigned: Boolean,
    val signaturePath: String?,
    val isSynced: Boolean,
    val createdAt: Long
)

@Entity(tableName = "permission_requests")
data class PermissionRequestEntity(
    @PrimaryKey val id: String,
    val requesterUserId: String,
    val requesterName: String,
    val requesterRole: String,
    val action: String,
    val targetEntity: String,
    val targetId: String?,
    val reason: String,
    val status: String,
    val reviewedByUserId: String?,
    val reviewedByName: String?,
    val reviewNote: String?,
    val signatureData: String?,
    val createdAt: Long,
    val reviewedAt: Long?
)

/** أحداث التزامن الواردة من WebSocket قبل تطبيقها على Room (لضمان عدم التكرار). */
@Entity(tableName = "sync_events")
data class SyncEventEntity(
    @PrimaryKey val eventId: String,
    val type: String,
    val payload: String,
    val receivedAt: Long,
    val applied: Boolean
)