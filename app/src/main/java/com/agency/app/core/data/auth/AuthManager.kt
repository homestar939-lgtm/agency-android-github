package com.agency.app.core.data.auth

import com.agency.app.core.domain.model.AppError
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.User
import com.agency.app.core.domain.model.UserRole
import com.agency.app.core.domain.repository.UserRepository
import com.agency.app.core.util.TimeSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * مدير الجلسة.
 *
 * ما يُحفظ بين التشغيلات هو معرّف المستخدم فقط، في [SecureStore] المشفَّر
 * بمفتاح من `AndroidKeyStore`. لا تُحفظ كلمة المرور ولا بصمتها هنا إطلاقاً؛
 * الجلسة تُعاد بناؤها من Room عند كل تشغيل، فإن حُذف المستخدم أو أُوقف
 * خرج من الجلسة تلقائياً.
 *
 * ولا تُحفظ علامة «غيّر كلمة المرور» محلياً: [mustChangePassword] يُحسب من
 * مطابقة كلمة المرور الافتراضية على البصمة المخزَّنة. لو كانت العلامة محلية
 * لكفى مسح بيانات التطبيق لإسقاط المطلب مع إبقاء كلمة المرور الافتراضية.
 */
@Singleton
class AuthManager @Inject constructor(
    private val store: SecureStore,
    private val userRepository: UserRepository
) {

    private val _session = MutableStateFlow<SessionState>(SessionState.Loading)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    /** المستخدم الحالي إن كانت هناك جلسة صالحة. */
    val currentUser: User? get() = (_session.value as? SessionState.Active)?.user

    val currentRole: UserRole? get() = currentUser?.role

    /**
     * هل يجب إلزام المستخدم الحالي بتغيير كلمة مروره قبل استخدام النظام؟
     *
     * يُفحص في [bootstrap] و[signIn] على قاعدة البيانات، ولا يُستنتج من
     * اسم المستخدم ولا من أي علامة محلية.
     */
    private val _mustChangePassword = MutableStateFlow(false)
    val mustChangePassword: StateFlow<Boolean> = _mustChangePassword.asStateFlow()

    /** هل التخزين المحلي مشفَّر على هذا الجهاز؟ تعرضه شاشة الإعدادات. */
    val isStorageEncrypted: Boolean get() = store.isEncrypted

    /**
     * تهيئة الجلسة عند بدء التطبيق:
     * 1) يزرع حساب المدير إن كانت القاعدة فارغة (أول تشغيل)،
     * 2) يعيد بناء الجلسة من المعرّف المحفوظ إن كان لا يزال صالحاً.
     */
    suspend fun bootstrap(defaultAdminPassword: String): Outcome<User?> {
        userRepository.ensureSeedAdmin(defaultAdminPassword)
        val savedId = store.getString(KEY_USER_ID)
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
                    adopt(user, defaultAdminPassword)
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
                store.putString(KEY_USER_ID, user.id)
                _session.value = SessionState.Active(user)
                // المقارنة بكلمة المرور المُدخلة نفسها: هي بالضرورة ما دخل به الآن.
                _mustChangePassword.value = user.role == UserRole.ADMIN &&
                    password == DEFAULT_ADMIN_PASSWORD &&
                    user.username == SEED_ADMIN_USERNAME
                userRepository.touchLastLogin(user.id, TimeSource.now())
                Outcome.Success(user)
            }
        }
    }

    /**
     * يغيّر كلمة مرور المستخدم الحالي بعد التحقق من القديمة.
     *
     * التحقق من القديمة ليس شكلياً: بدونه يستطيع من أخذ جهازاً مفتوحاً
     * تغيير كلمة مرور المدير وحيازة الحساب نهائياً.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Outcome<Unit> {
        val user = currentUser
            ?: return Outcome.Failure(AppError.Permission("لا توجد جلسة نشطة"))

        val verified = when (val check = userRepository.verifyPassword(user.id, currentPassword)) {
            is Outcome.Failure -> return check
            is Outcome.Success -> check.value
        }
        if (!verified) {
            return Outcome.Failure(
                AppError.Validation("currentPassword", "كلمة المرور الحالية غير صحيحة")
            )
        }
        if (newPassword == DEFAULT_ADMIN_PASSWORD) {
            return Outcome.Failure(
                AppError.Validation("newPassword", "لا يمكن استخدام كلمة المرور الافتراضية")
            )
        }
        if (newPassword.length < MIN_PASSWORD_LENGTH) {
            return Outcome.Failure(
                AppError.Validation(
                    "newPassword",
                    "كلمة المرور يجب أن تكون $MIN_PASSWORD_LENGTH أحرف على الأقل"
                )
            )
        }
        return when (val result = userRepository.changePassword(user.id, newPassword)) {
            is Outcome.Failure -> result
            is Outcome.Success -> {
                _mustChangePassword.value = false
                Outcome.Success(Unit)
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

    /**
     * يتبنّى جلسة قائمة ويعيد حساب مطلب تغيير كلمة المرور من القاعدة.
     *
     * يُستدعى في [bootstrap] حيث لا نملك كلمة المرور التي دخل بها المستخدم
     * سابقاً، فنسأل القاعدة مباشرةً: هل ما زالت البصمة تطابق الافتراضية؟
     */
    private suspend fun adopt(user: User, defaultAdminPassword: String) {
        _session.value = SessionState.Active(user)
        _mustChangePassword.value = if (
            user.role == UserRole.ADMIN && user.username == SEED_ADMIN_USERNAME
        ) {
            when (val check = userRepository.verifyPassword(user.id, defaultAdminPassword)) {
                is Outcome.Success -> check.value
                is Outcome.Failure -> false
            }
        } else {
            false
        }
    }

    private fun clearSession() {
        store.remove(KEY_USER_ID)
        _mustChangePassword.value = false
        _session.value = SessionState.SignedOut
    }

    companion object {
        /**
         * كلمة مرور المدير الافتراضية.
         *
         * وجودها مرئياً مقصود: هي نقطة انطلاق معروفة، ولذلك تُلزم شاشة
         * تغيير إجبارية بتغييرها قبل أي عمل. إخفاؤها لا يزيد الأمان لأن
         * أي مُثبِّت يستطيع قراءة الكود، والإلزام هو ما يحمي فعلاً.
         */
        const val DEFAULT_ADMIN_PASSWORD = "Admin@2024"
        const val SEED_ADMIN_USERNAME = "admin"
        const val MIN_PASSWORD_LENGTH = 8

        private const val KEY_USER_ID = "currentUserId"
    }
}

/** حالات الجلسة كما تراها الواجهة. */
sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class Active(val user: User) : SessionState
}