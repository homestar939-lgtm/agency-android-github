package com.agency.app.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.agency.app.core.domain.model.UserRole

/**
 * Accent tokens that are NOT part of the Material3 ColorScheme but are needed by
 * role-specific UI (badges, warehouse speed-dial, accountant ledger highlights).
 */
data class AgencyAccents(
    val role: UserRole,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val success: Color,
    val warning: Color,
    val info: Color,
)

val LocalAgencyAccents = staticCompositionLocalOf {
    AgencyAccents(
        role = UserRole.BUYER,
        primary = BuyerPrimary,
        secondary = BuyerSecondary,
        tertiary = BuyerTertiary,
        success = SuccessGreen,
        warning = WarningAmber,
        info = InfoBlue,
    )
}

private fun accentFor(role: UserRole): AgencyAccents = when (role) {
    UserRole.ADMIN -> AgencyAccents(role, AdminPrimary, AdminSecondary, AdminTertiary, SuccessGreen, WarningAmber, InfoBlue)
    UserRole.WAREHOUSE -> AgencyAccents(role, WarehousePrimary, WarehouseSecondary, WarehouseTertiary, SuccessGreen, WarningAmber, InfoBlue)
    UserRole.CASHIER -> AgencyAccents(role, CashierPrimary, CashierSecondary, CashierTertiary, SuccessGreen, WarningAmber, InfoBlue)
    UserRole.ACCOUNTANT -> AgencyAccents(role, AccountantPrimary, AccountantSecondary, AccountantTertiary, SuccessGreen, WarningAmber, InfoBlue)
    UserRole.BUYER -> AgencyAccents(role, BuyerPrimary, BuyerSecondary, BuyerTertiary, SuccessGreen, WarningAmber, InfoBlue)
}

private fun lightSchemeFor(role: UserRole) = when (role) {
    UserRole.ADMIN -> lightColorScheme(
        primary = AdminPrimary, onPrimary = Color.White,
        primaryContainer = Color(0xFFD5E2F0), onPrimaryContainer = AdminPrimary,
        secondary = AdminSecondary, onSecondary = Color.White,
        secondaryContainer = Color(0xFFD2EDE5), onSecondaryContainer = Color(0xFF00201A),
        tertiary = AdminTertiary, onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF6E8C6), onTertiaryContainer = Color(0xFF3A2B00),
        background = SurfaceLight, onBackground = OnSurfaceLight,
        surface = SurfaceLight, onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceContainerLight, onSurfaceVariant = Color(0xFF44474D),
        outline = Color(0xFF74808C), outlineVariant = DividerLight,
        error = ErrorRed, onError = Color.White,
    )

    UserRole.WAREHOUSE -> lightColorScheme(
        primary = WarehousePrimary, onPrimary = Color.White,
        primaryContainer = Color(0xFFD3E4F0), onPrimaryContainer = WarehousePrimary,
        secondary = WarehouseSecondary, onSecondary = Color.White,
        secondaryContainer = Color(0xFFFFE0C2), onSecondaryContainer = Color(0xFF2D1600),
        tertiary = WarehouseTertiary, onTertiary = Color.White,
        tertiaryContainer = Color(0xFFCDEBD9), onTertiaryContainer = Color(0xFF00210E),
        background = SurfaceLight, onBackground = OnSurfaceLight,
        surface = SurfaceLight, onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceContainerLight, onSurfaceVariant = Color(0xFF44474D),
        outline = Color(0xFF74808C), outlineVariant = DividerLight,
        error = ErrorRed, onError = Color.White,
    )

    UserRole.CASHIER -> lightColorScheme(
        primary = CashierPrimary, onPrimary = Color.White,
        primaryContainer = Color(0xFFF0D9F7), onPrimaryContainer = CashierPrimary,
        secondary = CashierSecondary, onSecondary = Color.White,
        secondaryContainer = Color(0xFFFBEEC2), onSecondaryContainer = Color(0xFF2A1E00),
        tertiary = CashierTertiary, onTertiary = Color.White,
        tertiaryContainer = Color(0xFFD0EBDA), onTertiaryContainer = Color(0xFF00210E),
        background = SurfaceLight, onBackground = OnSurfaceLight,
        surface = SurfaceLight, onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceContainerLight, onSurfaceVariant = Color(0xFF44474D),
        outline = Color(0xFF74808C), outlineVariant = DividerLight,
        error = ErrorRed, onError = Color.White,
    )

    UserRole.ACCOUNTANT -> lightColorScheme(
        primary = AccountantPrimary, onPrimary = Color.White,
        primaryContainer = Color(0xFFD8DFE6), onPrimaryContainer = AccountantPrimary,
        secondary = AccountantSecondary, onSecondary = Color.White,
        secondaryContainer = Color(0xFFE2E6EA), onSecondaryContainer = Color(0xFF1B242E),
        tertiary = AccountantTertiary, onTertiary = Color.White,
        tertiaryContainer = Color(0xFFCDEAE4), onTertiaryContainer = Color(0xFF00201B),
        background = SurfaceLight, onBackground = OnSurfaceLight,
        surface = SurfaceLight, onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceContainerLight, onSurfaceVariant = Color(0xFF44474D),
        outline = Color(0xFF74808C), outlineVariant = DividerLight,
        error = ErrorRed, onError = Color.White,
    )

    UserRole.BUYER -> lightColorScheme(
        primary = BuyerPrimary, onPrimary = Color.White,
        primaryContainer = Color(0xFFD3E4F0), onPrimaryContainer = BuyerPrimary,
        secondary = BuyerSecondary, onSecondary = Color.White,
        secondaryContainer = Color(0xFFD2EDE5), onSecondaryContainer = Color(0xFF00201A),
        tertiary = BuyerTertiary, onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF6E8C6), onTertiaryContainer = Color(0xFF3A2B00),
        background = SurfaceLight, onBackground = OnSurfaceLight,
        surface = SurfaceLight, onSurface = OnSurfaceLight,
        surfaceVariant = SurfaceContainerLight, onSurfaceVariant = Color(0xFF44474D),
        outline = Color(0xFF74808C), outlineVariant = DividerLight,
        error = ErrorRed, onError = Color.White,
    )
}

private fun darkSchemeFor(role: UserRole) = when (role) {
    UserRole.ADMIN -> darkColorScheme(
        primary = Color(0xFF9FC5E8), onPrimary = Color(0xFF00213A),
        primaryContainer = Color(0xFF0F2A47), onPrimaryContainer = Color(0xFFD5E2F0),
        secondary = Color(0xFF7FD1BC), onSecondary = Color(0xFF00201A),
        secondaryContainer = Color(0xFF00503F), onSecondaryContainer = Color(0xFFD2EDE5),
        tertiary = Color(0xFFE5C98A), onTertiary = Color(0xFF3A2B00),
        tertiaryContainer = Color(0xFF5A4300), onTertiaryContainer = Color(0xFFF6E8C6),
        background = SurfaceDark, onBackground = OnSurfaceDark,
        surface = SurfaceDark, onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceContainerDark, onSurfaceVariant = Color(0xFFC7C6CE),
        outline = Color(0xFF8E9099), outlineVariant = DividerDark,
        error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    )

    UserRole.WAREHOUSE -> darkColorScheme(
        primary = Color(0xFF9CC7E8), onPrimary = Color(0xFF00213A),
        primaryContainer = Color(0xFF1E4D6B), onPrimaryContainer = Color(0xFFD3E4F0),
        secondary = Color(0xFFFFB870), onSecondary = Color(0xFF4A2800),
        secondaryContainer = Color(0xFF6B3D00), onSecondaryContainer = Color(0xFFFFE0C2),
        tertiary = Color(0xFF8FD6B0), onTertiary = Color(0xFF00210E),
        tertiaryContainer = Color(0xFF00522C), onTertiaryContainer = Color(0xFFCDEBD9),
        background = SurfaceDark, onBackground = OnSurfaceDark,
        surface = SurfaceDark, onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceContainerDark, onSurfaceVariant = Color(0xFFC7C6CE),
        outline = Color(0xFF8E9099), outlineVariant = DividerDark,
        error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    )

    UserRole.CASHIER -> darkColorScheme(
        primary = Color(0xFFDBAFE8), onPrimary = Color(0xFF3A0B4A),
        primaryContainer = Color(0xFF7B2D8E), onPrimaryContainer = Color(0xFFF0D9F7),
        secondary = Color(0xFFE8C56A), onSecondary = Color(0xFF3B2E00),
        secondaryContainer = Color(0xFF574400), onSecondaryContainer = Color(0xFFFBEEC2),
        tertiary = Color(0xFF88D6AE), onTertiary = Color(0xFF00210E),
        tertiaryContainer = Color(0xFF00522F), onTertiaryContainer = Color(0xFFD0EBDA),
        background = SurfaceDark, onBackground = OnSurfaceDark,
        surface = SurfaceDark, onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceContainerDark, onSurfaceVariant = Color(0xFFC7C6CE),
        outline = Color(0xFF8E9099), outlineVariant = DividerDark,
        error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    )

    UserRole.ACCOUNTANT -> darkColorScheme(
        primary = Color(0xFFAEBFCF), onPrimary = Color(0xFF0F1B26),
        primaryContainer = Color(0xFF2C3E50), onPrimaryContainer = Color(0xFFD8DFE6),
        secondary = Color(0xFFB9C2CC), onSecondary = Color(0xFF212A33),
        secondaryContainer = Color(0xFF3F4A55), onSecondaryContainer = Color(0xFFE2E6EA),
        tertiary = Color(0xFF7FD8C8), onTertiary = Color(0xFF00201B),
        tertiaryContainer = Color(0xFF005046), onTertiaryContainer = Color(0xFFCDEAE4),
        background = SurfaceDark, onBackground = OnSurfaceDark,
        surface = SurfaceDark, onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceContainerDark, onSurfaceVariant = Color(0xFFC7C6CE),
        outline = Color(0xFF8E9099), outlineVariant = DividerDark,
        error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    )

    UserRole.BUYER -> darkColorScheme(
        primary = Color(0xFF9CC7E8), onPrimary = Color(0xFF00213A),
        primaryContainer = Color(0xFF1E4D6B), onPrimaryContainer = Color(0xFFD3E4F0),
        secondary = Color(0xFF7FD1BC), onSecondary = Color(0xFF00201A),
        secondaryContainer = Color(0xFF00503F), onSecondaryContainer = Color(0xFFD2EDE5),
        tertiary = Color(0xFFE5C98A), onTertiary = Color(0xFF3A2B00),
        tertiaryContainer = Color(0xFF5A4300), onTertiaryContainer = Color(0xFFF6E8C6),
        background = SurfaceDark, onBackground = OnSurfaceDark,
        surface = SurfaceDark, onSurface = OnSurfaceDark,
        surfaceVariant = SurfaceContainerDark, onSurfaceVariant = Color(0xFFC7C6CE),
        outline = Color(0xFF8E9099), outlineVariant = DividerDark,
        error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    )
}

private val AgencyTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)

private val AgencyShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
)

/**
 * Root theme. The color scheme is derived from [role] so that every screen
 * automatically inherits the identity of the logged-in employee.
 */
@Composable
fun AgencyAppTheme(
    role: UserRole,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) darkSchemeFor(role) else lightSchemeFor(role)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    CompositionLocalProvider(LocalAgencyAccents provides accentFor(role)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AgencyTypography,
            shapes = AgencyShapes,
            content = content,
        )
    }
}
