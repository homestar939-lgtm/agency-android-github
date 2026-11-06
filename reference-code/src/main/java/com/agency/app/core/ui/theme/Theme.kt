// core/ui/theme/Theme.kt
package com.agency.app.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AdminLightColorScheme = lightColorScheme(
    primary = AdminPrimary,
    secondary = AdminSecondary,
    tertiary = AdminTertiary,
    error = AdminError,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight
)

private val WarehouseLightColorScheme = lightColorScheme(
    primary = WarehousePrimary,
    secondary = WarehouseSecondary,
    tertiary = WarehouseTertiary,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight
)

@Composable
fun AgencyAppTheme(
    role: UserRole,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (role) {
        UserRole.ADMIN -> if (darkTheme) AdminDarkColorScheme else AdminLightColorScheme
        UserRole.WAREHOUSE -> if (darkTheme) WarehouseDarkColorScheme else WarehouseLightColorScheme
        // ... باقي الأدوار
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AgencyTypography,
        content = content
    )
}

enum class UserRole {
    ADMIN, WAREHOUSE, BUYER, CASHIER, ACCOUNTANT
}
