package com.agency.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// تعريف الأدوار
enum class UserRole {
    ADMIN, WAREHOUSE, BUYER, CASHIER, ACCOUNTANT
}

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E4D6B),
    secondary = Color(0xFFE67E22),
    tertiary = Color(0xFF27AE60),
    background = Color(0xFFFEFBFF),
    surface = Color(0xFFFEFBFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1B1B1F),
    onSurface = Color(0xFF1B1B1F)
)

@Composable
fun AgencyAppTheme(
    role: UserRole,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme // يمكن تطويره ليدعم Dark Mode لاحقاً

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// تعريف مؤقت للخطوط لتجنب أخطاء البناء
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
