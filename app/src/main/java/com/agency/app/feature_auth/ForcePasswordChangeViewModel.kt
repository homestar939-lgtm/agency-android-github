package com.agency.app.feature_auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.util.arabicMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * حالة شاشة تغيير كلمة المرور.
 *
  * [ForcePasswordChangeUiState.strengthSteps] ليست زخرفة: الشاشة إجبارية ولا مخرج منها، فالمستخدم يستحق
 * أن يعرف لماذا تُرفض كلمته قبل أن يضغط بدل أن يُفاجأ برسالة خطأ.
 */
data class ForcePasswordChangeUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val done: Boolean = false
) {
    val meetsLength: Boolean
        get() = newPassword.length >= AuthManager.MIN_PASSWORD_LENGTH

    val matchesConfirm: Boolean
        get() = confirmPassword.isNotEmpty() && newPassword == confirmPassword

    val isNotDefault: Boolean
        get() = newPassword != AuthManager.DEFAULT_ADMIN_PASSWORD

    val canSubmit: Boolean
        get() = currentPassword.isNotBlank() &&
            meetsLength &&
            isNotDefault &&
            matchesConfirm &&
            !isSubmitting

    /** مؤشر قوة مبسّط: عدد الشروط المتحققة من أربعة. */
    val strengthSteps: Int
        get() = listOf(
            newPassword.length >= AuthManager.MIN_PASSWORD_LENGTH,
            newPassword.any { it.isDigit() },
            newPassword.any { !it.isLetterOrDigit() },
            newPassword.any { it.isUpperCase() }
        ).count { it }
}

/**
 * عقل شاشة التغيير الإجباري.
 *
 * لا يقرّر هو أن التغيير مطلوب؛ [AuthManager] هو من يُبلّغ بذلك لأن الحقيقة
 * في القاعدة. هنا التنفيذ فقط، وبنجاحه يُسقط [AuthManager.mustChangePassword]
 * فتمرّ الواجهة إلى المخطط الطبيعي.
 */
@HiltViewModel
class ForcePasswordChangeViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    private val _ui = MutableStateFlow(ForcePasswordChangeUiState())
    val ui: StateFlow<ForcePasswordChangeUiState> = _ui.asStateFlow()

    val currentUser = authManager.currentUser

    fun onCurrentChange(value: String) {
        _ui.value = _ui.value.copy(currentPassword = value, error = null)
    }

    fun onNewChange(value: String) {
        _ui.value = _ui.value.copy(newPassword = value, error = null)
    }

    fun onConfirmChange(value: String) {
        _ui.value = _ui.value.copy(confirmPassword = value, error = null)
    }

    fun submit() {
        val state = _ui.value
        if (!state.canSubmit) return
        _ui.value = state.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            when (val result =
                authManager.changePassword(state.currentPassword, state.newPassword)) {
                is Outcome.Success -> {
                    _ui.value = _ui.value.copy(
                        isSubmitting = false,
                        done = true,
                        currentPassword = "",
                        newPassword = "",
                        confirmPassword = ""
                    )
                }
                is Outcome.Failure -> {
                    _ui.value = _ui.value.copy(
                        isSubmitting = false,
                        error = result.error.arabicMessage()
                    )
                }
            }
        }
    }

    /**
     * الخروج من الشاشة بالانسحاب من الجلسة.
     *
     * لا نسمح بتخطّي التغيير، لكن حبس المستخدم في شاشة بلا مخرج سلوك سيئ؛
     * فالبديل الصحيح هو تسجيل الخروج — يعود لاحقاً ويكمل التغيير.
     */
    fun abandon() {
        authManager.signOut()
    }
}