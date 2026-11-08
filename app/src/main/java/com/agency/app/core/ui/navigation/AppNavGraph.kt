package com.agency.app.core.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.agency.app.feature_home.DashboardScreen
import com.agency.app.feature_home.DashboardViewModel

/**
 * جذر التطبيق.
 *
 * الشاشة الأولى تُقرَّر من الجلسة لا من ترتيب المسارات: بلا جلسة نعرض الدخول،
 * وبجلسة نعرض لوحة الدور. أي محاولة للوصول إلى مسار محمي بلا جلسة تُعاد
 * إلى الدخول، وهي حماية على مستوى التنقل لا على مستوى الزر فقط.
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
                is SessionState.Active -> DashboardHost(
                    navController = navController,
                    userName = state.user.displayName,
                    role = state.user.role
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

@Composable
private fun DashboardHost(
    navController: NavHostController,
    userName: String,
    role: UserRole,
) {
    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Dashboard,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable<Route.Dashboard> {
                val dashboardViewModel: DashboardViewModel = hiltViewModel()
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    role = role,
                    userName = userName,
                    onSignOut = { dashboardViewModel.signOut() }
                )
            }
        }
    }
}