package com.agency.app.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.agency.app.feature_warehouse.orders.OrdersScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.WarehouseOrders.route // نقطة الدخول الحالية للتجربة
    ) {
        // ربط شاشة الطلبات الحقيقية التي تعمل مع قاعدة البيانات
        composable(Screen.WarehouseOrders.route) {
            OrdersScreen(
                onOrderClick = { orderId ->
                    // سيتم إضافة شاشة التفاصيل لاحقاً
                }
            )
        }
        
        // سيتم إضافة بقية الشاشات (Admin, Buyer, Cashier) هنا تدريجياً
    }
}

// تعريف مسارات التنقل
sealed class Screen(val route: String) {
    object WarehouseOrders : Screen("warehouse_orders")
    object AdminDashboard : Screen("admin_dashboard")
    object BuyerCatalog : Screen("buyer_catalog")
    object CashierDashboard : Screen("cashier_dashboard")
    object AccountantDashboard : Screen("accountant_dashboard")
}
