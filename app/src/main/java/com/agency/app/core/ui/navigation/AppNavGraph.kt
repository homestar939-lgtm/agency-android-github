package com.agency.app.core.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.agency.app.core.data.auth.SessionState
import com.agency.app.core.data.sync.rememberSyncLifecycle
import com.agency.app.core.domain.model.UserRole
import com.agency.app.core.ui.components.FullScreenLoading
import com.agency.app.core.ui.theme.AgencyAppTheme
import com.agency.app.feature_accountant.AccountantBalancesScreen
import com.agency.app.feature_accountant.AccountantBalancesViewModel
import com.agency.app.feature_accountant.AccountantStatementScreen
import com.agency.app.feature_accountant.AccountantStatementViewModel
import com.agency.app.feature_admin.AdminBuyersScreen
import com.agency.app.feature_admin.AdminBuyersViewModel
import com.agency.app.feature_admin.AdminPermissionsScreen
import com.agency.app.feature_admin.AdminPermissionsViewModel
import com.agency.app.feature_admin.AdminReportsScreen
import com.agency.app.feature_admin.AdminReportsViewModel
import com.agency.app.feature_admin.AdminUsersScreen
import com.agency.app.feature_admin.AdminUsersViewModel
import com.agency.app.feature_auth.ForcePasswordChangeScreen
import com.agency.app.feature_auth.ForcePasswordChangeViewModel
import com.agency.app.feature_auth.LoginScreen
import com.agency.app.feature_auth.LoginViewModel
import com.agency.app.feature_buyer.BuyerCartScreen
import com.agency.app.feature_buyer.BuyerCatalogScreen
import com.agency.app.feature_buyer.BuyerInvoicesScreen
import com.agency.app.feature_buyer.BuyerInvoicesViewModel
import com.agency.app.feature_buyer.BuyerOrderTrackingScreen
import com.agency.app.feature_buyer.BuyerOrdersScreen
import com.agency.app.feature_buyer.BuyerProductScreen
import com.agency.app.feature_buyer.BuyerStatementScreen
import com.agency.app.feature_buyer.CartViewModel
import com.agency.app.feature_buyer.CatalogViewModel
import com.agency.app.feature_buyer.OrderTrackingViewModel
import com.agency.app.feature_buyer.ProductDetailViewModel
import com.agency.app.feature_cashier.CashierCollectScreen
import com.agency.app.feature_cashier.CashierInvoiceDetailScreen
import com.agency.app.feature_cashier.CashierInvoiceDetailViewModel
import com.agency.app.feature_cashier.CashierInvoicesScreen
import com.agency.app.feature_cashier.CashierInvoicesViewModel
import com.agency.app.feature_cashier.CollectViewModel
import com.agency.app.feature_home.DashboardScreen
import com.agency.app.feature_home.DashboardViewModel
import com.agency.app.feature_warehouse.WarehouseOrderDetailScreen
import com.agency.app.feature_warehouse.WarehouseOrderDetailViewModel
import com.agency.app.feature_warehouse.WarehouseOrdersScreen
import com.agency.app.feature_warehouse.WarehouseOrdersViewModel
import com.agency.app.feature_warehouse.WarehouseProductsScreen
import com.agency.app.feature_warehouse.WarehouseProductsViewModel
import com.agency.app.feature_warehouse.WarehouseScanScreen
import com.agency.app.feature_warehouse.WarehouseScanViewModel
import com.agency.app.feature_warehouse.InvoiceViewerScreen
import com.agency.app.feature_warehouse.InvoiceViewerViewModel
import com.agency.app.feature_warehouse.StockAlertsScreen
import com.agency.app.feature_warehouse.StockAlertsViewModel
import com.agency.app.feature_warehouse.StockMovementScreen
import com.agency.app.feature_warehouse.StockMovementViewModel
import com.agency.app.feature_warehouse.WarehouseInvoicesScreen
import com.agency.app.feature_warehouse.WarehouseInvoicesViewModel
import com.agency.app.feature_warehouse.WarehouseStatsScreen
import com.agency.app.feature_warehouse.WarehouseStatsViewModel

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
    val mustChangePassword by loginViewModel.mustChangePassword.collectAsState()

    /*
     * ربط قناة المزامنة بالجلسة يقع هنا لا في الشاشات: القناة تُفتح مرة واحدة
     * لكل جلسة. `remember` يمنع تكرار الربط عند إعادة التركيب.
     */
    val syncLifecycle = rememberSyncLifecycle()
    LaunchedEffect(syncLifecycle) { syncLifecycle.bind() }

    /*
     * الإقلاع يُنفَّذ من الجذر لا من شاشة الدخول.
     *
     * كان مربوطاً بـ`LoginScreen`، لكن تلك الشاشة لا تُركَّب إلا بعد أن تصبح
     * الحالة `SignedOut`، والحالة تبدأ `Loading` ولا تخرج منها إلا بـ`bootstrap`.
     * أي أن كل طرف كان ينتظر الآخر: دائرة تحميل دائمة عند كل تشغيل.
     * الزرع عند الجذر يقطع الانتظار الدائري.
     */
    LaunchedEffect(loginViewModel) { loginViewModel.bootstrap() }

    val role = (session as? SessionState.Active)?.user?.role ?: UserRole.BUYER

    AgencyAppTheme(role = role) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val state = session) {
                SessionState.Loading -> FullScreenLoading()
                SessionState.SignedOut -> LoginHost(loginViewModel)
                is SessionState.Active -> {
                    /*
                     * بوابة تغيير كلمة المرور تسبق المخطط كله، لا تُسجَّل كمسار
                     * داخله. السبب: لو كانت مساراً لأمكن تجاوزها بالرجوع، ولو
                     * كانت شاشة عادية لكان ترتيب `when` وحده هو الحماية.
                     * هنا لا يوجد مخطط أصلاً قبل التغيير، فلا يوجد ما يُتجاوز.
                     */
                    if (mustChangePassword) {
                        val forceViewModel: ForcePasswordChangeViewModel = hiltViewModel()
                        ForcePasswordChangeScreen(viewModel = forceViewModel)
                    } else {
                        AppNavHost(
                            navController = navController,
                            userName = state.user.displayName,
                            role = state.user.role,
                        )
                    }
                }
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
 *
 * المسارات المُعرَّفة في `Route` تُسجَّل هنا كاملةً حتى لو لم يُعرض مدخلها
 * لكل دور: الفتح المباشر من لوحة دور آخر يرفضه مخطط البيانات لا يُسقط التطبيق،
 * لكن غياب التسجيل كان يُسقطه — لذلك اكتمال التسجيل شرط سلامة لا رفاهية.
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

        /* ------------------------------ المخزن ------------------------------ */

        composable<Route.BuyerInvoices> {
            val viewModel: BuyerInvoicesViewModel = hiltViewModel()
            BuyerInvoicesScreen(
                viewModel = viewModel,
                onOpenStatement = { navController.navigate(Route.BuyerStatement) },
            )
        }

        composable<Route.WarehouseOrders> {
            val viewModel: WarehouseOrdersViewModel = hiltViewModel()
            WarehouseOrdersScreen(
                viewModel = viewModel,
                onOpenOrder = { orderId -> navController.navigate(Route.WarehouseOrderDetail(orderId)) },
            )
        }

        composable<Route.WarehouseOrderDetail> {
            val viewModel: WarehouseOrderDetailViewModel = hiltViewModel()
            WarehouseOrderDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.WarehouseProducts> {
            val viewModel: WarehouseProductsViewModel = hiltViewModel()
            WarehouseProductsScreen(viewModel = viewModel)
        }

        composable<Route.WarehouseScan> {
            val viewModel: WarehouseScanViewModel = hiltViewModel()
            WarehouseScanScreen(
                viewModel = viewModel,
                onOpenOrder = { orderId ->
                    navController.navigate(Route.WarehouseOrderDetail(orderId))
                },
                onOpenProducts = { navController.navigate(Route.WarehouseProducts) },
            )
        }

        /* ---------- شاشات المخزن المكمّلة (المرحلة ب) ---------- */
        composable<Route.WarehouseStats> {
            val viewModel: WarehouseStatsViewModel = hiltViewModel()
            WarehouseStatsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.StockMovement> {
            val viewModel: StockMovementViewModel = hiltViewModel()
            StockMovementScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.StockAlerts> {
            val viewModel: StockAlertsViewModel = hiltViewModel()
            StockAlertsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.WarehouseInvoices> {
            val viewModel: WarehouseInvoicesViewModel = hiltViewModel()
            WarehouseInvoicesScreen(
                viewModel = viewModel,
                onOpenInvoice = { invoiceId ->
                    navController.navigate(Route.InvoiceViewer(invoiceId))
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<Route.InvoiceViewer> { entry ->
            /*
             * المعرّف يُقرأ من `toRoute()` لا من وسيط نصي: المسارات هنا
             * `@Serializable`، وقراءة الوسيط يدوياً كانت ستنجح مرة وتنكسر
             * صامتةً عند تغيير التوقيع.
             */
            val route = entry.toRoute<Route.InvoiceViewer>()
            val viewModel: InvoiceViewerViewModel = hiltViewModel()
            InvoiceViewerScreen(
                invoiceId = route.invoiceId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }

        /* --------------------------- أمين الصندوق --------------------------- */

        composable<Route.CashierInvoices> {
            val viewModel: CashierInvoicesViewModel = hiltViewModel()
            CashierInvoicesScreen(
                viewModel = viewModel,
                onOpenInvoice = { invoiceId ->
                    navController.navigate(Route.CashierInvoiceDetail(invoiceId))
                },
            )
        }

        composable<Route.CashierInvoiceDetail> {
            val viewModel: CashierInvoiceDetailViewModel = hiltViewModel()
            CashierInvoiceDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.CashierCollect> {
            val viewModel: CollectViewModel = hiltViewModel()
            CashierCollectScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable<Route.CashierCollectFor> {
            val viewModel: CollectViewModel = hiltViewModel()
            val buyerId = it.arguments?.getString("buyerId")
            CashierCollectScreen(
                viewModel = viewModel,
                initialBuyerId = buyerId,
                onBack = { navController.popBackStack() },
            )
        }

        /* ------------------------------ المحاسب ------------------------------ */

        composable<Route.AccountantBalances> {
            val viewModel: AccountantBalancesViewModel = hiltViewModel()
            AccountantBalancesScreen(
                viewModel = viewModel,
                onOpenStatement = { buyerId ->
                    navController.navigate(Route.AccountantStatement(buyerId))
                },
            )
        }

        composable<Route.AccountantStatement> {
            val viewModel: AccountantStatementViewModel = hiltViewModel()
            AccountantStatementScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }

        /* ------------------------------- المدير ------------------------------- */

        composable<Route.AdminUsers> {
            val viewModel: AdminUsersViewModel = hiltViewModel()
            AdminUsersScreen(viewModel = viewModel)
        }

        composable<Route.AdminBuyers> {
            val viewModel: AdminBuyersViewModel = hiltViewModel()
            AdminBuyersScreen(viewModel = viewModel)
        }

        composable<Route.AdminPermissions> {
            val viewModel: AdminPermissionsViewModel = hiltViewModel()
            AdminPermissionsScreen(viewModel = viewModel)
        }

        composable<Route.AdminReports> {
            val viewModel: AdminReportsViewModel = hiltViewModel()
            AdminReportsScreen(viewModel = viewModel)
        }
    }
}
