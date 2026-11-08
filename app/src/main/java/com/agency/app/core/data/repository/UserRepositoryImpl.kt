package com.agency.app.core.data.repository

import com.agency.app.core.data.local.dao.UserDao
import com.agency.app.core.data.local.mapper.toDomain
import com.agency.app.core.data.local.mapper.toEntity
import com.agency.app.core.data.local.mapper.toEpochMs
import com.agency.app.core.domain.model.AppError
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.User
import com.agency.app.core.domain.model.UserRole
import com.agency.app.core.domain.repository.UserRepository
import com.agency.app.core.util.Ids
import com.agency.app.core.util.PasswordHasher
import com.agency.app.core.util.TimeSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant

/**
 * تنفيذ إدارة المستخدمين فوق Room.
 *
 * كل الأخطاء تُغلَّف في [AppError] قبل الخروج من هذه الطبقة،
 * فلا يصل استثناء خام إلى ViewModel ولا إلى الواجهة.
 */
@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {

    override fun observeUsers(): Flow<List<User>> =
        userDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getUser(id: String): Outcome<User> = guarded {
        val entity = userDao.findById(id)
            ?: return@guarded Outcome.Failure(AppError.NotFound("مستخدم", id))
        Outcome.Success(entity.toDomain())
    }

    override suspend fun findByUsername(username: String): Outcome<User?> = guarded {
        Outcome.Success(userDao.findByUsername(username.trim())?.toDomain())
    }

    override suspend fun createUser(
        username: String,
        displayName: String,
        password: String,
        role: UserRole,
        phone: String?,
        buyerId: String?
    ): Outcome<User> = guarded {
        val cleanUsername = username.trim()
        if (cleanUsername.length < 3) {
            return@guarded Outcome.Failure(
                AppError.Validation("username", "اسم المستخدم يجب أن يكون 3 أحرف على الأقل")
            )
        }
        if (password.length < 6) {
            return@guarded Outcome.Failure(
                AppError.Validation("password", "كلمة المرور يجب أن تكون 6 أحرف على الأقل")
            )
        }
        if (displayName.isBlank()) {
            return@guarded Outcome.Failure(
                AppError.Validation("displayName", "الاسم المعروض مطلوب")
            )
        }
        // المشتري يجب أن يُربط بسجل مشترٍ، وإلا فلن يرى أي كتالوج.
        if (role == UserRole.BUYER && buyerId.isNullOrBlank()) {
            return@guarded Outcome.Failure(
                AppError.Validation("buyerId", "حساب المشتري يجب أن يُربط بمشترٍ موجود")
            )
        }
        if (role != UserRole.BUYER && !buyerId.isNullOrBlank()) {
            return@guarded Outcome.Failure(
                AppError.Validation("buyerId", "ربط المشتري مسموح لأدوار المشتري فقط")
            )
        }
        val existing = userDao.findByUsername(cleanUsername)
        if (existing != null) {
            return@guarded Outcome.Failure(
                AppError.Conflict("اسم المستخدم مستخدم بالفعل: $cleanUsername")
            )
        }
        val now = TimeSource.now()
        val user = User(
            id = Ids.new(),
            username = cleanUsername,
            displayName = displayName.trim(),
            role = role,
            phone = phone?.trim()?.takeIf { it.isNotEmpty() },
            isActive = true,
            buyerId = buyerId?.takeIf { it.isNotBlank() },
            createdAt = now,
            lastLoginAt = null
        )
        userDao.upsert(user.toEntity(PasswordHasher.hash(password)))
        Outcome.Success(user)
    }

    override suspend fun setActive(userId: String, active: Boolean): Outcome<Unit> = guarded {
        val entity = userDao.findById(userId)
            ?: return@guarded Outcome.Failure(AppError.NotFound("مستخدم", userId))
        userDao.upsert(entity.copy(isActive = active))
        Outcome.Success(Unit)
    }

    override suspend fun changePassword(userId: String, newPassword: String): Outcome<Unit> =
        guarded {
            if (newPassword.length < 6) {
                return@guarded Outcome.Failure(
                    AppError.Validation("password", "كلمة المرور يجب أن تكون 6 أحرف على الأقل")
                )
            }
            val entity = userDao.findById(userId)
                ?: return@guarded Outcome.Failure(AppError.NotFound("مستخدم", userId))
            userDao.upsert(entity.copy(passwordHash = PasswordHasher.hash(newPassword)))
            Outcome.Success(Unit)
        }

    override suspend fun verifyPassword(userId: String, password: String): Outcome<Boolean> =
        guarded {
            val entity = userDao.findById(userId)
                ?: return@guarded Outcome.Failure(AppError.NotFound("مستخدم", userId))
            Outcome.Success(PasswordHasher.matches(password, entity.passwordHash))
        }

    override suspend fun authenticate(username: String, password: String): Outcome<User> =
        guarded {
            val entity = userDao.findByUsername(username.trim())
                ?: return@guarded Outcome.Failure(
                    AppError.Validation("username", "بيانات الدخول غير صحيحة")
                )
            if (!entity.isActive) {
                return@guarded Outcome.Failure(AppError.Permission("الحساب معطّل"))
            }
            // رسالة واحدة موحّدة لاسم المستخدم وكلمة المرور: لا نكشف أي الحقلين أخطأ.
            if (!PasswordHasher.matches(password, entity.passwordHash)) {
                return@guarded Outcome.Failure(
                    AppError.Validation("password", "بيانات الدخول غير صحيحة")
                )
            }
            val now = TimeSource.now()
            userDao.touchLastLogin(entity.id, now.toEpochMs())
            Outcome.Success(entity.toDomain().copy(lastLoginAt = now))
        }

    override suspend fun ensureSeedAdmin(password: String): Outcome<Unit> = guarded {
        if (userDao.count() > 0) return@guarded Outcome.Success(Unit)
        val now = Instant.fromEpochMilliseconds(0L)
        val admin = User(
            id = Ids.new(),
            username = "admin",
            displayName = "مدير الوكالة",
            role = UserRole.ADMIN,
            phone = null,
            isActive = true,
            buyerId = null,
            createdAt = if (now.toEpochMilliseconds() == 0L) TimeSource.now() else now,
            lastLoginAt = null
        )
        userDao.upsert(admin.toEntity(PasswordHasher.hash(password)))
        Outcome.Success(Unit)
    }

    override suspend fun touchLastLogin(userId: String, at: Instant): Outcome<Unit> = guarded {
        if (userDao.findById(userId) == null) {
            return@guarded Outcome.Failure(AppError.NotFound("مستخدم", userId))
        }
        userDao.touchLastLogin(userId, at.toEpochMs())
        Outcome.Success(Unit)
    }
}

/**
 * غلاف موحّد لتنفيذ عمليات الـ suspend: يحوّل أي استثناء إلى [Outcome.Failure]
 * بنوع [AppError] مناسب، مع الحفاظ على [AppException] كما هو.
 */
internal inline fun <T> guarded(block: () -> Outcome<T>): Outcome<T> = try {
    block()
} catch (e: com.agency.app.core.domain.model.AppException) {
    Outcome.Failure(e.error)
} catch (e: kotlinx.coroutines.CancellationException) {
    throw e
} catch (e: Throwable) {
    Outcome.Failure(AppError.Unknown(e))
}