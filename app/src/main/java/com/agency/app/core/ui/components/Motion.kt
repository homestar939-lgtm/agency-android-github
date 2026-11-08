package com.agency.app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agency.app.core.domain.model.Money
import com.agency.app.core.ui.theme.LocalAgencyAccents
import kotlinx.coroutines.delay

/**
 * حركة الواجهة — طبقة واحدة تُستدعى من كل الشاشات.
 *
 * المبدأ: الحركة تشرح ما حدث، ولا تُزيّن ما لم يحدث. كل ما هنا مربوط
 * بانتقال حالة حقيقية (ظهور محتوى، تغيّر رقم)، ولا يوجد مؤقّت يخفي
 * انتظاراً حقيقياً خلف رسوم متحركة.
 */

/**
 * رأس الشاشة المميَّز: تدرّج بلون الدور، عنوان، وسطر توضيحي.
 *
 * التدرّج من `LocalAgencyAccents` لا من لون مثبَّت، حتى يبقى كل دور
 * بلونه في كل شاشة تُضاف لاحقاً بلا تعديل.
 */
@Composable
fun HeroHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accent: Color? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val accents = LocalAgencyAccents.current
    val base = accent ?: accents.primary
    // ظهور ناعم عند أول تركيب: الشاشة تُبنى ولا "تقفز" على العين.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val reveal by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "heroReveal",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = ((1f - reveal) * 14f).dp)
            .alpha(reveal),
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(base, accents.secondary),
                    ),
                )
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.size(14.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f),
                        )
                    }
                }
                trailing?.invoke()
            }
        }
    }
}

/**
 * رقم يتصاعد من صفر إلى قيمته عند الظهور.
 *
 * يُستخدم للمبالغ لأن العين تلتقط "المبلغ كبر" قبل أن تقرأ الخانات.
 * عند انعدام قيمة سالبة يتصاعد من صفر إلى السالب مباشرة بلا قص.
 */
@Composable
fun AnimatedCurrency(
    amount: Money,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle? = null,
    color: Color = Color.Unspecified,
) {
    val target = remember(amount.cents) { Animatable(0f) }
    LaunchedEffect(amount.cents) {
        target.animateTo(
            targetValue = amount.cents.toFloat(),
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        )
    }
    Text(
        text = Money(target.value.toLong()).formatted(),
        modifier = modifier,
        style = style ?: MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}

/**
 * شريط تقدّم يتحرك من قيمته السابقة إلى الجديدة.
 *
 * [progress] بين 0 و1. القيمة تُقيَّد داخل المدى، لأن شريطاً يتجاوز
 * عرضه يظهر كخطأ تصميم لا كخطأ بيانات.
 */
@Composable
fun AnimatedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color? = null,
    height: Int = 10,
) {
    val accents = LocalAgencyAccents.current
    val clamped = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "progress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(height.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(height.dp)
                .clip(RoundedCornerShape(height.dp))
                .background(color ?: accents.primary),
        )
    }
}

/**
 * غلاف يُظهر محتواه بانزلاق خفيف عند أول ظهور.
 *
 * [delayMillis] يسمح بترتيب بطاقات الشاشة الواحدة، فيُقرأ التتابع
 * كترتيب أولوية لا كعرض.
 */
@Composable
fun RevealOnAppear(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable ColumnScope.() -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        shown = true
    }
    val value by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "reveal",
    )
    Column(
        modifier = modifier
            .offset(y = ((1f - value) * 10f).dp)
            .alpha(value),
        content = content,
    )
}

/**
 * تقليص لطيف عند الضغط — بديل بصري عن التغذية الراجعة المفقودة في
 * الأزرار المسطّحة. لا يغيّر أي منطق، ولا يُستخدم على عناصر معطّلة.
 */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "pressScale",
    )
    return this.scale(scale)
}

/**
 * انتقال بسيط لمحتوى يتبدّل بين تحميل ونتيجة.
 *
 * الغرض الحقيقي: منع "قفزة" القائمة عند وصول البيانات. المحتوى ينزلق
 * من موضعه لا من أعلى الشاشة.
 */
@Composable
fun AnimatedContentSwap(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(320)) + expandVertically(tween(320), expandFrom = Alignment.Top),
        exit = fadeOut(tween(160)) + shrinkVertically(tween(160), shrinkTowards = Alignment.Top),
    ) {
        content()
    }
}

/**
 * شيمر على شكل بطاقة قائمة — يُستخدم مكان `LoadingShimmer` حين تكون
 * وحدات التحميل بطاقات لا أسطر نص.
 *
 * النبض هنا لا يوحي بأن شيئاً سيصل قريباً؛ هو مجرد إشارة أن الشاشة
 * حيّة. الشاشات التي تعرف مدة الانتظار تعرض نصاً، لا نبضاً.
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    lines: Int = 3,
) {
    val transition = rememberInfiniteTransition(label = "shimmerCard")
    val alpha by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerCardAlpha",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(lines) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (index == 0) 0.55f else if (index % 2 == 0) 1f else 0.78f)
                        .height(if (index == 0) 18.dp else 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.14f),
                        ),
                )
            }
        }
    }
}

/**
 * شاشة تحميل كاملة تُبني من الشيمر بدل دوّار واحد في الفراغ.
 *
 * سبب استبدال الدوّار: الدوّار لا يقول شيئاً عن شكل ما سيصل، فيبدو
 * التطبيق كأنه يبحث عن نفسه. البطاقات تُري المشهد قبل وصوله.
 */
@Composable
fun ShimmerListScreen(
    modifier: Modifier = Modifier,
    cards: Int = 4,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        repeat(cards) {
            ShimmerCard(lines = 3)
        }
    }
}
