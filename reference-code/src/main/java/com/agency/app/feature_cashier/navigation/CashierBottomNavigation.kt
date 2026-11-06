// feature_cashier/navigation/CashierBottomNavigation.kt
package com.agency.app.feature_cashier.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector

data class CashierNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String,
    val badge: Int? = null
)

@Composable
fun CashierBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit = {}
) {
    val items = listOf(
        CashierNavItem(
            "التحصيل",
            Icons.Filled.AccountBalanceWallet,
            Icons.Outlined.AccountBalanceWallet,
            "cashier/dashboard"
        ),
        CashierNavItem(
            "السندات",
            Icons.Filled.ReceiptLong,
            Icons.Outlined.ReceiptLong,
            "cashier/receipts"
        ),
        CashierNavItem(
            "إحصائياتي",
            Icons.Filled.QueryStats,
            Icons.Outlined.QueryStats,
            "cashier/stats"
        ),
        CashierNavItem(
            "حسابي",
            Icons.Filled.Person,
            Icons.Outlined.Person,
            "cashier/profile"
        )
    )

    NavigationBar {
        items.forEach { item ->
            val selected = currentRoute == item.route
            
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (item.badge != null && item.badge > 0) {
                                Badge { Text(item.badge.toString()) }
                            }
                        }
                    ) {
                        Icon(
                            if (selected) item.selectedIcon else item.unselectedIcon,
                            item.label
                        )
                    }
                },
                label = { Text(item.label) }
            )
        }
    }
}
