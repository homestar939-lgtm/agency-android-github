// feature_buyer/onboarding/OnboardingScreen.kt
package com.agency.app.feature_buyer.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val gradientColors: List<Color>
)

@Composable
fun OnboardingScreen(
    onPageChange: (Int) -> Unit,
    onComplete: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Filled.ShoppingCart,
            title = "تسوق بذكاء",
            description = "اكتشف منتجات الوكالة بسهولة، ابحث بالاسم أو الصنف أو المعرف، واحصل على أفضل الأسعار",
            gradientColors = listOf(
                Color(0xFF1E4D6B),
                Color(0xFF2E6B8B),
                Color(0xFF3E8BAB)
            )
        ),
        OnboardingPage(
            icon = Icons.Filled.Speed,
            title = "سرعة فائقة",
            description = "اطلب بضغطة زر، وتابع طلبيتك لحظة بلحظة من التحضير إلى التسليم بإشعارات فورية",
            gradientColors = listOf(
                Color(0xFFE67E22),
                Color(0xFFF39C12),
                Color(0xFFF1C40F)
            )
        ),
        OnboardingPage(
            icon = Icons.Filled.Security,
            title = "أمان وثقة",
            description = "سندات رسمية موثقة، أرشيف كامل لعملياتك، وتحكم كامل في حسابك مع دعم فني متواصل",
            gradientColors = listOf(
                Color(0xFF27AE60),
                Color(0xFF2ECC71),
                Color(0xFF58D68D)
            )
        )
    )

    var currentPage by remember { mutableStateOf(0) }

    val current = pages[currentPage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = current.gradientColors,
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // مؤشر التقدم
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isActive = index == currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isActive) 32.dp else 8.dp, 8.dp)
                            .background(
                                color = if (isActive) Color.White else Color.White.copy(alpha = 0.4f),
                                shape = MaterialTheme.shapes.small
                            )
                    )
                }
            }

            // المحتوى
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        fadeIn() + slideInHorizontally { it / 2 } togetherWith 
                        fadeOut() + slideOutHorizontally { -it / 2 }
                    }
                ) { page ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // الأيقونة
                        Surface(
                            modifier = Modifier.size(140.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = pages[page].icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = Color.White
                                )
                            }
                        }

                        // العنوان
                        Text(
                            text = pages[page].title,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        // الوصف
                        Text(
                            text = pages[page].description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // الأزرار
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (currentPage < pages.size - 1) {
                            currentPage++
                            onPageChange(currentPage)
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = current.gradientColors.first()
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = if (currentPage < pages.size - 1) "متابعة" else "ابدأ الآن",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (currentPage < pages.size - 1) {
                    TextButton(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "تخطي",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
