package com.agency.app.core.ui.navigation

import kotlinx.serialization.Serializable

/**
 * مسارات التنقّل الآمنة نوعياً (Navigation Compose Type-Safe API).
 *
 * كل مسار كائن قابل للتسلسل، فلا تمرّ النصوص المكتوبة يدوياً بين الشاشات،
 * ولا يمكن تمرير معرّف بالخطأ إلى الشاشة الخاطئة.
 */
sealed interface Route {

    /** شاشة الدخول — نقطة البداية دائماً عند غياب الجلسة. */
    @Serializable
    data object Login : Route

    /** لوحة الدور الحالية؛ كل دور يُعرض له محتواه فقط. */
    @Serializable
    data object Dashboard : Route

    /* ---------------- المشتري ---------------- */
    @Serializable
    data object BuyerCatalog : Route

    @Serializable
    data class BuyerProduct(val productId: String) : Route

    @Serializable
    data object BuyerCart : Route

    @Serializable
    data object BuyerOrders : Route

    @Serializable
    data class BuyerOrderDetail(val orderId: String) : Route

    @Serializable
    data object BuyerStatement : Route

    @Serializable
    data object BuyerInvoices : Route

    /* ---------------- المخزن ---------------- */
    @Serializable
    data object WarehouseOrders : Route

    @Serializable
    data class WarehouseOrderDetail(val orderId: String) : Route

    @Serializable
    data object WarehouseProducts : Route

    @Serializable
    data object WarehouseScan : Route

    /* ---------------- أمين الصندوق ---------------- */
    @Serializable
    data object CashierInvoices : Route

    @Serializable
    data class CashierInvoiceDetail(val invoiceId: String) : Route

    @Serializable
    data object CashierCollect : Route

    @Serializable
    data class CashierCollectFor(val buyerId: String) : Route

    /* ---------------- المحاسب ---------------- */
    @Serializable
    data object AccountantBalances : Route

    @Serializable
    data class AccountantStatement(val buyerId: String) : Route

    /* ---------------- المدير ---------------- */
    @Serializable
    data object AdminUsers : Route

    @Serializable
    data object AdminBuyers : Route

    @Serializable
    data object AdminPermissions : Route

    @Serializable
    data object AdminReports : Route
}