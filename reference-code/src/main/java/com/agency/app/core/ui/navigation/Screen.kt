// core/ui/navigation/Screen.kt
package com.agency.app.core.ui.navigation

sealed class Screen(val route: String) {
    // ما قبل الدخول
    object Splash : Screen("splash")
    object Onboarding1 : Screen("onboarding1")
    object Onboarding2 : Screen("onboarding2")
    object Onboarding3 : Screen("onboarding3")
    object Login : Screen("login")
    object PendingActivation : Screen("pending_activation")

    // المدير
    object AdminDashboard : Screen("admin/dashboard")
    object AdminAuditLog : Screen("admin/audit_log")
    object AdminNotifications : Screen("admin/notifications")
    object AdminReports : Screen("admin/reports")
    object AdminOrders : Screen("admin/orders")
    object AdminOrderDetail : Screen("admin/orders/{orderId}") {
        fun createRoute(orderId: String) = "admin/orders/$orderId"
    }
    object AdminFinance : Screen("admin/finance")
    object AdminInventory : Screen("admin/inventory")
    object AdminSettings : Screen("admin/settings")

    // المخزن
    object WarehouseOrders : Screen("warehouse/orders")
    object WarehouseOrderDetail : Screen("warehouse/orders/{orderId}") {
        fun createRoute(orderId: String) = "warehouse/orders/$orderId"
    }
    object WarehousePreparation : Screen("warehouse/preparation/{orderId}") {
        fun createRoute(orderId: String) = "warehouse/preparation/$orderId"
    }
    object WarehouseInvoiceCreator : Screen("warehouse/invoice_creator/{orderId}") {
        fun createRoute(orderId: String) = "warehouse/invoice_creator/$orderId"
    }
    object WarehouseDispatch : Screen("warehouse/dispatch/{orderId}") {
        fun createRoute(orderId: String) = "warehouse/dispatch/$orderId"
    }
    object WarehouseProducts : Screen("warehouse/products")
    object WarehouseProductEditor : Screen("warehouse/products/{productId}") {
        fun createRoute(productId: String?) = "warehouse/products/${productId ?: "new"}"
    }
    object WarehouseInvoices : Screen("warehouse/invoices")
    object WarehouseProfile : Screen("warehouse/profile")
}
