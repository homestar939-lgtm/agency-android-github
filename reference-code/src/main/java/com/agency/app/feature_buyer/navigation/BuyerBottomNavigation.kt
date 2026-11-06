// feature_buyer/navigation/BuyerBottomNavigation.kt
package com.agency.app.feature_buyer.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector

data class BuyerNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String,
    val badge: Int? = null
)

@Composable
fun BuyerBottomNavigation(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BuyerNavItem("الرئيسية", Icons.Filled.Home, Icons.Outlined.Home, "buyer/home"),
        BuyerNavItem("الوكالات", Icons.Filled.Store, Icons.Outlined.Store, "buyer/agencies"),
        BuyerNavItem("السلة", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart, "buyer/cart", badge = 3),
        BuyerNavItem("طلباتي", Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping, "buyer/orders"),
        BuyerNavItem("حسابي", Icons.Filled.Person, Icons.Outlined.Person, "buyer/profile")
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
