package com.agency.app.feature_auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agency.app.core.ui.components.AppButton
import com.agency.app.core.ui.components.AppCard
import com.agency.app.core.ui.components.AppTextField
import com.agency.app.core.ui.components.FullScreenLoading

/**
 * شاشة الدخول.
 *
 * لا تعرض أي بيانات قبل التحقق من الهوية، ولا تكشف تفصيلاً يميّز
 * «اسم مستخدم خاطئ» عن «كلمة مرور خاطئة».
 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBootstrap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { onBootstrap() }

    if (state.isBootstrapping) {
        FullScreenLoading(modifier = modifier)
        return
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // imePadding قبل verticalScroll: حين يفتح لوحة المفاتيح كان زر
                // الدخول يزحف فوق حقل كلمة المرور ويغطيه. الحشو يجعل المساحة
                // القابلة للتمرير تنكمش مع اللوحة، فيبقى الزر تحت الحقل دائماً.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Badge,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "نظام الوكالة",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "إدارة الطلبيات والفوترة والذمم",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            AppCard {
                AppTextField(
                    value = state.username,
                    onValueChange = onUsernameChange,
                    label = "اسم المستخدم",
                    enabled = !state.isSubmitting,
                    leadingIcon = Icons.Filled.Person,
                )
                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    label = "كلمة المرور",
                    isPassword = true,
                    enabled = !state.isSubmitting,
                    leadingIcon = Icons.Filled.Lock,
                )
                if (state.error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(20.dp))
                AppButton(
                    text = "دخول",
                    onClick = onSubmit,
                    enabled = state.canSubmit,
                    loading = state.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}