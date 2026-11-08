package com.agency.app.feature_auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.data.auth.SessionState
import com.agency.app.core.domain.model.AppError
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** حالة واجهة الدخول كما تُعرض للمستخدم. */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val isBootstrapping: Boolean = true
) {
    val canSubmit: Boolean
        get() = username.isNotBlank() && password.isNotBlank() && !isSubmitting && !isBootstrapping
}

/**
 * عقل شاشة الدخول.
 *
 * لا يعرف هذا الـ ViewModel شيئاً عن Room ولا عن الشبكة؛ يكلّم [AuthManager]
 * فقط، ويستقبل رسالة خطأ عربية جاهزة من [AppError.Validation] أو غيرها.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    private val _ui = MutableStateFlow(LoginUiState())
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    val session: StateFlow<SessionState> = authManager.session

    /**
     * هل يجب إجبار المستخدم الحالي على تغيير كلمة مروره؟
     *
     * يُمرَّر كما هو من [AuthManager] بلا حساب محلي، لأن الحقيقة في القاعدة:
     * علامة محلية تُمسح مع بيانات التطبيق فتُسقط المطلب مع بقاء الكلمة الافتراضية.
     */
    val mustChangePassword: StateFlow<Boolean> = authManager.mustChangePassword

    private var bootstrapped = false

    /**
     * تهيئة أول تشغيل: يزرع حساب المدير إن كانت القاعدة فارغة.
     * كلمة المرور الأولية تُطلب من المستخدم لا تُخترع هنا.
     */
    fun bootstrap(defaultAdminPassword: String = DEFAULT_ADMIN_PASSWORD) {
        if (bootstrapped) return
        bootstrapped = true
        viewModelScope.launch {
            authManager.bootstrap(defaultAdminPassword)
            _ui.value = _ui.value.copy(isBootstrapping = false)
        }
    }

    fun onUsernameChange(value: String) {
        _ui.value = _ui.value.copy(username = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _ui.value = _ui.value.copy(password = value, error = null)
    }

    fun submit() {
        val state = _ui.value
        if (!state.canSubmit) return
        _ui.value = state.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            when (val result = authManager.signIn(state.username, state.password)) {
                is Outcome.Success -> {
                    _ui.value = _ui.value.copy(isSubmitting = false, password = "")
                }
                is Outcome.Failure -> {
                    _ui.value = _ui.value.copy(
                        isSubmitting = false,
                        error = result.error.message()
                    )
                }
            }
        }
    }

    private fun AppError.message(): String = when (this) {
        is AppError.Validation -> message
        is AppError.Permission -> message
        is AppError.NotFound -> "الحساب غير موجود"
        is AppError.Conflict -> message
        is AppError.Network -> "تعذّر الاتصال بالخادم"
        is AppError.Unknown -> "حدث خطأ غير متوقع، حاول مرة أخرى"
    }

    companion object {
        /**
         * كلمة مرور المدير الأولية — تُقرأ من [AuthManager] لا تُكرَّر هنا،
         * حتى لا يوجد رقمان لسر واحد يتباعدان عند التعديل.
         */
        const val DEFAULT_ADMIN_PASSWORD = AuthManager.DEFAULT_ADMIN_PASSWORD
    }
}