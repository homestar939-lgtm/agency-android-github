package com.agency.app.core.data.auth

import android.content.Context
import com.agency.app.core.domain.model.AppError
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.User
import com.agency.app.core.domain.model.UserRole
import com.agency.app.core.domain.repository.UserRepository
import com.agency.app.core.util.TimeSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * مدير الجلسة.
 *
 * يُخزَّن معرّف المستخدم الحالي فقط في SharedPreferences؛ ولا تُخزَّن كلمة المرور
 * ولا حتى بصمتها هنا. الجلسة تُعاد بناؤها من Room عند كل تشغيل، فإن حُذف
 * المستخدم أو أُوقف خرج من الجلسة تلقائياً.
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext context: Context,
    private val userRepository: UserRepository
) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _session = MutableStateFlow<SessionState>(SessionState.Loading)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    /** المستخدم الحالي إن كانت هناك جلسة صالحة. */
    val currentUser: User? get() = (_session.value as? SessionState.Active)?.user

    val currentRole: UserRole? get() = currentUser?.role

    /**
     * تهيئة الجلسة عند بدء التطبيق:
     * 1) يزرع حساب المدير إن كانت القاعدة فارغة (أول تشغيل)،
     * 2) يعيد بناء الجلسة من المعرّف المحفوظ إن كان لا يزال صالحاً.
     */
    suspend fun bootstrap(defaultAdminPassword: String): Outcome<User?> {
        userRepository.ensureSeedAdmin(defaultAdminPassword)
        val savedId = prefs.getString(KEY_USER_ID, null)
        if (savedId.isNullOrBlank()) {
            _session.value = SessionState.SignedOut
            return Outcome.Success(null)
        }
        return when (val result = userRepository.getUser(savedId)) {
            is Outcome.Failure -> {
                clearSession()
                Outcome.Success(null)
            }
            is Outcome.Success -> {
                val user = result.value
                if (!user.isActive) {
                    clearSession()
                    Outcome.Success(null)
                } else {
                    _session.value = SessionState.Active(user)
                    Outcome.Success(user)
                }
            }
        }
    }

    /** تسجيل الدخول: يتحقق من كلمة المرور ثم يثبّت الجلسة محلياً. */
    suspend fun signIn(username: String, password: String): Outcome<User> {
        return when (val result = userRepository.authenticate(username.trim(), password)) {
            is Outcome.Failure -> result
            is Outcome.Success -> {
                val user = result.value
                prefs.edit().putString(KEY_USER_ID, user.id).apply()
                _session.value = SessionState.Active(user)
                userRepository.touchLastLogin(user.id, TimeSource.now())
                Outcome.Success(user)
            }
        }
    }

    /** خروج صريح: يمسح الجلسة فقط، ولا يمسّ بيانات المستخدم. */
    fun signOut() {
        clearSession()
    }

    /**
     * حماية الإجراءات المالية: أي دور بلا صلاحية كتابة مالية يُمنع
     * ويُطلب منه تقديم طلب صلاحية بدلاً من التنفيذ.
     */
    fun requireFinancialWriter(): Outcome<User> {
        val user = currentUser
            ?: return Outcome.Failure(AppError.Permission("لا توجد جلسة نشطة"))
        if (!user.role.canWriteFinancials) {
            return Outcome.Failure(
                AppError.Permission(
                    "دورك (${user.role}) لا يملك صلاحية الكتابة المالية؛ قدّم طلب صلاحية للمدير"
                )
            )
        }
        return Outcome.Success(user)
    }

    private fun clearSession() {
        prefs.edit().remove(KEY_USER_ID).apply()
        _session.value = SessionState.SignedOut
    }

    private companion object {
        const val PREFS_NAME = "agency.session"
        const val KEY_USER_ID = "currentUserId"
    }
}

/** حالات الجلسة كما تراها الواجهة. */
sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class Active(val user: User) : SessionState
}