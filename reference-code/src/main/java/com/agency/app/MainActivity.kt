// MainActivity.kt
package com.agency.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.agency.app.core.security.AuthManager
import com.agency.app.core.ui.navigation.AppNavGraph
import com.agency.app.core.ui.theme.AgencyAppTheme
import com.agency.app.core.ui.theme.UserRole
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val currentUser by authManager.currentUser.collectAsState()
            val role = currentUser?.role ?: UserRole.ADMIN

            AgencyAppTheme(role = role) {
                val navController = rememberNavController()
                
                val startDestination = when {
                    currentUser == null -> "login"
                    !currentUser!!.isActive -> "pending_activation"
                    else -> when (role) {
                        UserRole.ADMIN -> "admin/dashboard"
                        UserRole.WAREHOUSE -> "warehouse/orders"
                        UserRole.BUYER -> "buyer/home"
                        UserRole.CASHIER -> "cashier/dashboard"
                        UserRole.ACCOUNTANT -> "accountant/dashboard"
                    }
                }

                AppNavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}
