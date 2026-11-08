package com.agency.app.core.data.local.mapper

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
import com.agency.app.core.domain.model.Buyer
import com.agency.app.core.domain.model.Invoice
import com.agency.app.core.domain.model.InvoiceItem
import com.agency.app.core.domain.model.InvoiceStatus
import com.agency.app.core.domain.model.Money
import com.agency.app.core.domain.model.Order
import com.agency.app.core.domain.model.OrderItem
import com.agency.app.core.domain.model.OrderStatus
import com.agency.app.core.domain.model.OrderTimeline
import com.agency.app.core.domain.model.PaymentMethod
import com.agency.app.core.domain.model.PermissionRequest
import com.agency.app.core.domain.model.PermissionRequestStatus
import com.agency.app.core.domain.model.Product
import com.agency.app.core.domain.model.ProductVisibility
import com.agency.app.core.domain.model.Receipt
import com.agency.app.core.domain.model.User
import com.agency.app.core.domain.model.UserRole
import kotlinx.datetime.Instant

/* ------------------------------------------------------------------ */
/*  الوقت: التحويل الوحيد المسموح بين Long (Room) و Instant (Domain)   */
/* ------------------------------------------------------------------ */

fun Long.toInstant(): Instant = Instant.fromEpochMilliseconds(this)

fun Instant.toEpochMs(): Long = toEpochMilliseconds()

/* ------------------------------------------------------------------ */
/*  تحويلات آمنة للـ enums — أي قيمة غير معروفة لا تُسقط التطبيق        */
/* ------------------------------------------------------------------ */

private inline fun <reified T : Enum<T>> enumOrDefault(name: String, fallback: T): T =
    runCatching { enumValueOf<T>(name) }.getOrDefault(fallback)

/* ------------------------------------------------------------------ */
/*  User                                                              */
/* ------------------------------------------------------------------ */

fun UserEntity.toDomain(): User = User(
    id = id,
    username = username,
    displayName = displayName,
    role = enumOrDefault(role, UserRole.BUYER),
    phone = phone,
    isActive = isActive,
    buyerId = buyerId,
    createdAt = createdAt.toInstant(),
    lastLoginAt = lastLoginAt?.toInstant(),
)

fun User.toEntity(passwordHash: String): UserEntity = UserEntity(
    id = id,
    username = username,
    passwordHash = passwordHash,
    displayName = displayName,
    role = role.name,
    phone = phone,
    isActive = isActive,
    buyerId = buyerId,
    createdAt = createdAt.toEpochMs(),
    lastLoginAt = lastLoginAt?.toEpochMs(),
)

/* ------------------------------------------------------------------ */
/*  Buyer                                                             */
/* ------------------------------------------------------------------ */

fun BuyerEntity.toDomain(): Buyer = Buyer(
    id = id,
    name = name,
    phone = phone,
    address = address,
    shopName = shopName,
    creditLimit = Money(creditLimitCents),
    isActive = isActive,
    createdAt = createdAt.toInstant(),
    updatedAt = updatedAt.toInstant(),
)

fun Buyer.toEntity(): BuyerEntity = BuyerEntity(
    id = id,
    name = name,
    phone = phone,
    address = address,
    shopName = shopName,
    creditLimitCents = creditLimit.cents,
    isActive = isActive,
    createdAt = createdAt.toEpochMs(),
    updatedAt = updatedAt.toEpochMs(),
)

/* ------------------------------------------------------------------ */
/*  Product                                                           */
/* ------------------------------------------------------------------ */

fun ProductEntity.toDomain(): Product = Product(
    id = id,
    name = name,
    sku = sku,
    unit = unit,
    price = Money(priceCents),
    cost = Money(costCents),
    stockQuantity = stockQuantity,
    minOrderQuantity = minOrderQuantity,
    expiryDate = expiryDate?.toInstant(),
    categoryId = categoryId,
    imageUrl = imageUrl,
    isActive = isActive,
    visibility = ProductVisibility(
        showPrice = showPrice,
        showUnit = showUnit,
        showExpiry = showExpiry,
        showMinOrder = showMinOrder,
        showStock = showStock,
    ),
    updatedAt = updatedAt.toInstant(),
)

fun Product.toEntity(updatedAt: Instant): ProductEntity = ProductEntity(
    id = id,
    name = name,
    sku = sku,
    unit = unit,
    priceCents = price.cents,
    costCents = cost.cents,
    stockQuantity = stockQuantity,
    minOrderQuantity = minOrderQuantity,
    expiryDate = expiryDate?.toEpochMs(),
    categoryId = categoryId,
    imageUrl = imageUrl,
    isActive = isActive,
    showPrice = visibility.showPrice,
    showUnit = visibility.showUnit,
    showExpiry = visibility.showExpiry,
    showMinOrder = visibility.showMinOrder,
    showStock = visibility.showStock,
    updatedAt = updatedAt.toEpochMs(),
)

/* ------------------------------------------------------------------ */
/*  Order                                                             */
/* ------------------------------------------------------------------ */

fun OrderItemEntity.toDomain(): OrderItem = OrderItem(
    id = id,
    orderId = orderId,
    productId = productId,
    productName = productName,
    unit = unit,
    unitPrice = Money(unitPriceCents),
    quantity = quantity,
    discount = Money(discountCents),
)

fun OrderItem.toEntity(): OrderItemEntity = OrderItemEntity(
    id = id,
    orderId = orderId,
    productId = productId,
    productName = productName,
    unit = unit,
    unitPriceCents = unitPrice.cents,
    quantity = quantity,
    discountCents = discount.cents,
)

fun OrderTimelineEntity.toDomain(): OrderTimeline = OrderTimeline(
    status = enumOrDefault(status, OrderStatus.NEW),
    at = at.toInstant(),
    actorId = actorId,
    actorName = actorName,
    note = note,
)

fun OrderTimeline.toEntity(orderId: String, id: String): OrderTimelineEntity = OrderTimelineEntity(
    id = id,
    orderId = orderId,
    status = status.name,
    at = at.toEpochMs(),
    actorId = actorId,
    actorName = actorName,
    note = note,
)

fun Order.toEntity(): OrderEntity = OrderEntity(
    id = id,
    orderNumber = orderNumber,
    buyerId = buyerId,
    buyerName = buyerName,
    status = status.name,
    note = note,
    createdAt = createdAt.toEpochMs(),
    updatedAt = updatedAt.toEpochMs(),
)

fun buildOrder(
    header: OrderEntity,
    items: List<OrderItemEntity>,
    timeline: List<OrderTimelineEntity>,
): Order = Order(
    id = header.id,
    orderNumber = header.orderNumber,
    buyerId = header.buyerId,
    buyerName = header.buyerName,
    status = enumOrDefault(header.status, OrderStatus.NEW),
    items = items.map { it.toDomain() },
    timeline = timeline.map { it.toDomain() }.sortedBy { it.at },
    note = header.note,
    createdAt = header.createdAt.toInstant(),
    updatedAt = header.updatedAt.toInstant(),
)

/* ------------------------------------------------------------------ */
/*  Invoice                                                           */
/* ------------------------------------------------------------------ */

fun InvoiceItemEntity.toDomain(): InvoiceItem = InvoiceItem(
    id = id,
    invoiceId = invoiceId,
    productId = productId,
    productName = productName,
    unit = unit,
    unitPrice = Money(unitPriceCents),
    quantity = quantity,
    discount = Money(discountCents),
)

fun InvoiceItem.toEntity(): InvoiceItemEntity = InvoiceItemEntity(
    id = id,
    invoiceId = invoiceId,
    productId = productId,
    productName = productName,
    unit = unit,
    unitPriceCents = unitPrice.cents,
    quantity = quantity,
    discountCents = discount.cents,
)

fun Invoice.toEntity(): InvoiceEntity = InvoiceEntity(
    id = id,
    invoiceNumber = invoiceNumber,
    orderId = orderId,
    buyerId = buyerId,
    buyerName = buyerName,
    status = status.name,
    commissionCents = commission.cents,
    note = note,
    createdByUserId = createdByUserId,
    approvedByUserId = approvedByUserId,
    approvedAt = approvedAt?.toEpochMs(),
    rejectionReason = rejectionReason,
    createdAt = createdAt.toEpochMs(),
    updatedAt = updatedAt.toEpochMs(),
)

fun buildInvoice(
    header: InvoiceEntity,
    items: List<InvoiceItemEntity>,
): Invoice = Invoice(
    id = header.id,
    invoiceNumber = header.invoiceNumber,
    orderId = header.orderId,
    buyerId = header.buyerId,
    buyerName = header.buyerName,
    status = enumOrDefault(header.status, InvoiceStatus.DRAFT),
    items = items.map { it.toDomain() },
    commission = Money(header.commissionCents),
    note = header.note,
    createdByUserId = header.createdByUserId,
    approvedByUserId = header.approvedByUserId,
    approvedAt = header.approvedAt?.toInstant(),
    rejectionReason = header.rejectionReason,
    createdAt = header.createdAt.toInstant(),
    updatedAt = header.updatedAt.toInstant(),
)

/* ------------------------------------------------------------------ */
/*  Receipt                                                           */
/* ------------------------------------------------------------------ */

fun ReceiptEntity.toDomain(): Receipt = Receipt(
    id = id,
    receiptNumber = receiptNumber,
    buyerId = buyerId,
    buyerName = buyerName,
    amount = Money(amountCents),
    method = enumOrDefault(method, PaymentMethod.CASH),
    invoiceId = invoiceId,
    reference = reference,
    note = note,
    collectorUserId = collectorUserId,
    collectorName = collectorName,
    collectedAt = collectedAt.toInstant(),
    isSigned = isSigned,
    signaturePath = signaturePath,
    isSynced = isSynced,
    createdAt = createdAt.toInstant(),
)

fun Receipt.toEntity(): ReceiptEntity = ReceiptEntity(
    id = id,
    receiptNumber = receiptNumber,
    buyerId = buyerId,
    buyerName = buyerName,
    amountCents = amount.cents,
    method = method.name,
    invoiceId = invoiceId,
    reference = reference,
    note = note,
    collectorUserId = collectorUserId,
    collectorName = collectorName,
    collectedAt = collectedAt.toEpochMs(),
    isSigned = isSigned,
    signaturePath = signaturePath,
    isSynced = isSynced,
    createdAt = createdAt.toEpochMs(),
)

/* ------------------------------------------------------------------ */
/*  PermissionRequest                                                 */
/* ------------------------------------------------------------------ */

fun PermissionRequestEntity.toDomain(): PermissionRequest = PermissionRequest(
    id = id,
    requesterUserId = requesterUserId,
    requesterName = requesterName,
    requesterRole = enumOrDefault(requesterRole, UserRole.ACCOUNTANT),
    action = action,
    targetEntity = targetEntity,
    targetId = targetId,
    reason = reason,
    status = enumOrDefault(status, PermissionRequestStatus.PENDING),
    reviewedByUserId = reviewedByUserId,
    reviewedByName = reviewedByName,
    reviewNote = reviewNote,
    signatureData = signatureData,
    createdAt = createdAt.toInstant(),
    reviewedAt = reviewedAt?.toInstant(),
)

fun PermissionRequest.toEntity(): PermissionRequestEntity = PermissionRequestEntity(
    id = id,
    requesterUserId = requesterUserId,
    requesterName = requesterName,
    requesterRole = requesterRole.name,
    action = action,
    targetEntity = targetEntity,
    targetId = targetId,
    reason = reason,
    status = status.name,
    reviewedByUserId = reviewedByUserId,
    reviewedByName = reviewedByName,
    reviewNote = reviewNote,
    signatureData = signatureData,
    createdAt = createdAt.toEpochMs(),
    reviewedAt = reviewedAt?.toEpochMs(),
)
