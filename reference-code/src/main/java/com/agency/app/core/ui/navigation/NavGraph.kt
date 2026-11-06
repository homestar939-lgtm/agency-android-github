// core/ui/navigation/NavGraph.kt
package com.agency.app.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.agency.app.feature_admin.dashboard.DashboardScreen
import com.agency.app.feature_warehouse.orders.OrdersScreen
import com.agency.app.feature_warehouse.orders.OrderDetailScreen
import com.agency.app.feature_warehouse.orders.PreparationScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ما قبل الدخول
        composable(Screen.Splash.route) {
            // SplashScreen()
        }
        composable(Screen.Login.route) {
            // LoginScreen()
        }

        // المدير
        composable(Screen.AdminDashboard.route) {
            DashboardScreen()
        }
        composable(Screen.AdminAuditLog.route) {
            // AuditLogScreen()
        }

        // المخزن
        composable(Screen.WarehouseOrders.route) {
            OrdersScreen(
                onOrderClick = { orderId ->
                    navController.navigate(Screen.WarehouseOrderDetail.createRoute(orderId))
                }
            )
        }
        composable(
            Screen.WarehouseOrderDetail.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
            OrderDetailScreen(
                orderId = orderId,
                onStartPreparation = {
                    navController.navigate(Screen.WarehousePreparation.createRoute(orderId))
                }
            )
        }
        composable(
            Screen.WarehousePreparation.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
            PreparationScreen(
                orderId = orderId,
                onPreparationComplete = {
                    navController.navigate(Screen.WarehouseInvoiceCreator.createRoute(orderId)) {
                        popUpTo(Screen.WarehouseOrders.route) { inclusive = false }
                    }
                }
            )
        }
    }
}
