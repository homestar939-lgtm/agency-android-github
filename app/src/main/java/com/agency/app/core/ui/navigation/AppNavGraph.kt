package com.agency.app.core.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.agency.app.core.data.auth.SessionState
import com.agency.app.core.domain.model.UserRole
import com.agency.app.core.ui.components.FullScreenLoading
import com.agency.app.core.ui.theme.AgencyAppTheme
import com.agency.app.feature_auth.LoginScreen
import com.agency.app.feature_auth.LoginViewModel
import com.agency.app.feature_buyer.BuyerCartScreen
import com.agency.app.feature_buyer.BuyerCatalogScreen
import com.agency.app.feature_buyer.BuyerOrderTrackingScreen
import com.agency.app.feature_buyer.BuyerOrdersScreen
import com.agency.app.feature_buyer.BuyerProductScreen
import com.agency.app.feature_buyer.BuyerStatementScreen
import com.agency.app.feature_buyer.CartViewModel
import com.agency.app.feature_buyer.CatalogViewModel
import com.agency.app.feature_buyer.OrderTrackingViewModel
import com.agency.app.feature_buyer.ProductDetailViewModel
import com.agency.app.feature_home.DashboardScreen
import com.agency.app.feature_home.DashboardViewModel

/**
 * جذر التطبيق.
 *
 * الشاشة الأولى تُقرَّر من الجلسة لا من ترتيب المسارات: بلا جلسة نعرض الدخول،
 * وبجلسة نعرض مخطط الشاشات المحمية. الحماية على مستوى التنقل لا على مستوى الزر.
 */
@Composable
fun AgencyRoot(
    navController: NavHostController = rememberNavController(),
) {
    val loginViewModel: LoginViewModel = hiltViewModel()
    val session by loginViewModel.session.collectAsState()
    val role = (session as? SessionState.Active)?.user?.role ?: UserRole.BUYER

    AgencyAppTheme(role = role) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val state = session) {
                SessionState.Loading -> FullScreenLoading()
                SessionState.SignedOut -> LoginHost(loginViewModel)
                is SessionState.Active -> AppNavHost(
                    navController = navController,
                    userName = state.user.displayName,
                    role = state.user.role,
                )
            }
        }
    }
}

@Composable
private fun LoginHost(viewModel: LoginViewModel) {
    val state by viewModel.ui.collectAsState()
    LoginScreen(
        state = state,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onBootstrap = { viewModel.bootstrap() }
    )
}

/**
 * مخطط الشاشات المحمية.
 *
 * كل شاشة تُجلب لها ViewModel الخاصة عبر `hiltViewModel()` فتُحقن
 * مستودعاتها تلقائياً، ولا تُنشأ أي تبعية يدوياً في الواجهة.
 */
@Composable
private fun AppNavHost(
    navController: NavHostController,
    userName: String,
    role: UserRole,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Dashboard,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable<Route.Dashboard> {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            DashboardScreen(
                viewModel = dashboardViewModel,
                role = role,
                userName = userName,
                onSignOut = {
                    dashboardViewModel.signOut()
                    navController.popBackStack(Route.Dashboard, inclusive = true)
                },
                onNavigate = { destination -> navController.navigate(destination) },
            )
        }

        /* ------------------------------ المشتري ------------------------------ */

        composable<Route.BuyerCatalog> {
            val viewModel: CatalogViewModel = hiltViewModel()
            BuyerCatalogScreen(
                viewModel = viewModel,
                onOpenProduct = { productId -> navController.navigate(Route.BuyerProduct(productId)) },
                onOpenCart = { navController.navigate(Route.BuyerCart) },
            )
        }

        composable<Route.BuyerProduct> {
            val viewModel: ProductDetailViewModel = hiltViewModel()
            BuyerProductScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenCart = { navController.navigate(Route.BuyerCart) },
            )
        }

        composable<Route.BuyerCart> {
            val viewModel: CartViewModel = hiltViewModel()
            BuyerCartScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onCreated = { orderId ->
                    navController.navigate(Route.BuyerOrderDetail(orderId)) {
                        popUpTo(Route.BuyerCart) { inclusive = true }
                    }
                },
            )
        }

        composable<Route.BuyerOrders> {
            BuyerOrdersScreen(
                viewModel = hiltViewModel(),
                onOpenOrder = { orderId -> navController.navigate(Route.BuyerOrderDetail(orderId)) },
            )
        }

        composable<Route.BuyerOrderDetail> {
            val viewModel: OrderTrackingViewModel = hiltViewModel()
            BuyerOrderTrackingScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.BuyerStatement> {
            BuyerStatementScreen(viewModel = hiltViewModel())
        }
    }
}