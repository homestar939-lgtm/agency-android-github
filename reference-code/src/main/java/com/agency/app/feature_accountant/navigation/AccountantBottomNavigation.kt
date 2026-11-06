// feature_accountant/navigation/AccountantBottomNavigation.kt
package com.agency.app.feature_accountant.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector

data class AccountantNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String,
    val badge: Int? = null
)

@Composable
fun AccountantBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit = {}
) {
    val items = listOf(
        AccountantNavItem(
            "المراقبة",
            Icons.Filled.Monitoring,
            Icons.Outlined.Monitoring,
            "accountant/dashboard",
            badge = 5
        ),
        AccountantNavItem(
            "الفواتير",
            Icons.Filled.Receipt,
            Icons.Outlined.Receipt,
            "accountant/invoices"
        ),
        AccountantNavItem(
            "السندات",
            Icons.Filled.ReceiptLong,
            Icons.Outlined.ReceiptLong,
            "accountant/receipts"
        ),
        AccountantNavItem(
            "التقارير",
            Icons.Filled.QueryStats,
            Icons.Outlined.QueryStats,
            "accountant/reports"
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
