package com.agency.app.feature_auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.ui.components.AppButton
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.AppTextField
import com.agency.app.core.ui.theme.LocalAgencyAccents

/**
 * شاشة تغيير كلمة المرور الإجباري.
 *
 * تُعرض قبل أي شاشة أخرى حين يكون الحساب ما زال على كلمة المرور الافتراضية.
 * وهي إجبارية حقاً لا تحذيرية: لا زر «لاحقاً»، والسبيل الوحيد للمضيّ هو
 * التغيير أو تسجيل الخروج. السبب مالي لا شكلي: من يدخل باسم `admin`
 * وكلمة المرور المنشورة في الكود يملك توقيع الاعتمادات وتعديل الأسعار،
 * فبقاء كلمة المرور الافتراضية يُبطل كل حماية صلاحيات بُنيت في النظام.
 *
 * الشاشة لا تُغلق نفسها: المخطط الأعلى هو من يزيلها لأن
 * `AuthManager.mustChangePassword` يصير `false` بعد النجاح.
 */
@Composable
fun ForcePasswordChangeScreen(
    viewModel: ForcePasswordChangeViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val accents = LocalAgencyAccents.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = accents.warning,
            )
            Text(
                text = "تغيير كلمة المرور إلزامي",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "حسابك ما زال على كلمة المرور الافتراضية التي يعرفها كل من قرأ " +
                    "الكود المصدري. لا يمكنك توقيع اعتماد أو تعديل سعر قبل تغييرها، " +
                    "لأن هذه الحساب هو من يمنح تلك الصلاحيات لبقية الأدوار.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            AppCard {
                AppTextField(
                    value = state.currentPassword,
                    onValueChange = viewModel::onCurrentChange,
                    label = "كلمة المرور الحالية",
                    isPassword = true,
                    enabled = !state.isSubmitting,
                    leadingIcon = Icons.Filled.Lock,
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = state.newPassword,
                    onValueChange = viewModel::onNewChange,
                    label = "كلمة المرور الجديدة",
                    isPassword = true,
                    enabled = !state.isSubmitting,
                    leadingIcon = Icons.Filled.Lock,
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = state.confirmPassword,
                    onValueChange = viewModel::onConfirmChange,
                    label = "تأكيد كلمة المرور",
                    isPassword = true,
                    enabled = !state.isSubmitting,
                    isError = state.confirmPassword.isNotEmpty() && !state.matchesConfirm,
                    supportingText = if (state.confirmPassword.isNotEmpty() && !state.matchesConfirm) {
                        "الكلمتان غير متطابقتين"
                    } else {
                        null
                    },
                    leadingIcon = Icons.Filled.Lock,
                )

                Spacer(Modifier.height(16.dp))
                PasswordRule(
                    text = "لا تقل عن ${AuthManager.MIN_PASSWORD_LENGTH} أحرف",
                    met = state.meetsLength,
                    modifier = Modifier.fillMaxWidth(),
                )
                PasswordRule(
                    text = "ليست كلمة المرور الافتراضية",
                    met = state.newPassword.isNotEmpty() && state.isNotDefault,
                    modifier = Modifier.fillMaxWidth(),
                )
                PasswordRule(
                    text = "تحتوي رقماً ورمزاً وحرفاً كبيراً على الأقل",
                    met = state.strengthSteps >= 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = state.error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(20.dp))
                AppButton(
                    text = "تغيير كلمة المرور والمتابعة",
                    onClick = viewModel::submit,
                    enabled = state.canSubmit,
                    loading = state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                text = "الخروج يعيدك لشاشة الدخول، وسيُطلب منك التغيير مرة أخرى " +
                    "عند الدخول التالي. لا يُخزَّن اختيار التخطّي.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = viewModel::abandon) {
                Text("تسجيل الخروج")
            }
        }
    }
}

/** سطر قاعدة كلمة مرور مع علامة تحقّق — يُظهر الشرط لا يكتفي بفرضه. */
@Composable
private fun PasswordRule(
    text: String,
    met: Boolean,
    modifier: Modifier = Modifier,
) {
    val accents = LocalAgencyAccents.current
    Row(
        modifier = modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = if (met) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (met) accents.success else MaterialTheme.colorScheme.outline,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (met) accents.success else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}