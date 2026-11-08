package com.agency.app.core.data.repository

import com.agency.app.core.data.local.dao.PermissionDao
import com.agency.app.core.data.local.mapper.toDomain
import com.agency.app.core.data.local.mapper.toEntity
import com.agency.app.core.domain.model.AppError
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.PermissionRequest
import com.agency.app.core.domain.model.PermissionRequestStatus
import com.agency.app.core.domain.model.UserRole
import com.agency.app.core.domain.repository.PermissionRepository
import com.agency.app.core.util.Ids
import com.agency.app.core.util.TimeSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * تنفيذ طلبات الصلاحية.
 *
 * القاعدة الحاكمة: لا يُنفَّذ إجراء كتابي من دور للقراءة فقط إلا بطلب معتمد
 * وتوقيع رقمي من المدير. الموافقة بلا توقيع مرفوضة بالكامل.
 */
@Singleton
class PermissionRepositoryImpl @Inject constructor(
    private val permissionDao: PermissionDao
) : PermissionRepository {

    override fun observeAll(): Flow<List<PermissionRequest>> =
        permissionDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeByStatus(
        status: PermissionRequestStatus
    ): Flow<List<PermissionRequest>> =
        permissionDao.observeByStatus(status.name).map { rows -> rows.map { it.toDomain() } }

    override fun observePendingCount(): Flow<Int> = permissionDao.observePendingCount()

    override suspend fun request(
        requesterUserId: String,
        requesterName: String,
        requesterRole: UserRole,
        action: String,
        targetEntity: String,
        targetId: String?,
        reason: String
    ): Outcome<PermissionRequest> = guarded {
        if (reason.isBlank()) {
            return@guarded Outcome.Failure(
                AppError.Validation("reason", "سبب الطلب مطلوب لدراسته من المدير")
            )
        }
        if (action.isBlank() || targetEntity.isBlank()) {
            return@guarded Outcome.Failure(
                AppError.Validation("action", "الإجراء والكيان المستهدف مطلوبان")
            )
        }
        val now = TimeSource.now()
        val request = PermissionRequest(
            id = Ids.new(),
            requesterUserId = requesterUserId,
            requesterName = requesterName,
            requesterRole = requesterRole,
            action = action,
            targetEntity = targetEntity,
            targetId = targetId,
            reason = reason,
            status = PermissionRequestStatus.PENDING,
            createdAt = now
        )
        permissionDao.upsert(request.toEntity())
        Outcome.Success(request)
    }

    override suspend fun approve(
        requestId: String,
        reviewerUserId: String,
        reviewerName: String,
        signatureData: String,
        reviewNote: String?
    ): Outcome<PermissionRequest> = guarded {
        if (signatureData.isBlank()) {
            return@guarded Outcome.Failure(
                AppError.Permission("الموافقة تتطلب توقيعاً رقمياً من المدير")
            )
        }
        val entity = permissionDao.findById(requestId)
            ?: return@guarded Outcome.Failure(AppError.NotFound("طلب صلاحية", requestId))
        val current = entity.toDomain()
        if (!current.isPending) {
            return@guarded Outcome.Failure(
                AppError.Conflict("الطلب ${current.id} حُسم مسبقاً بحالة ${current.status}")
            )
        }
        val now = TimeSource.now()
        val approved = current.copy(
            status = PermissionRequestStatus.APPROVED,
            reviewedByUserId = reviewerUserId,
            reviewedByName = reviewerName,
            reviewNote = reviewNote,
            signatureData = signatureData,
            reviewedAt = now
        )
        permissionDao.upsert(approved.toEntity())
        Outcome.Success(approved)
    }

    override suspend fun reject(
        requestId: String,
        reviewerUserId: String,
        reviewerName: String,
        reviewNote: String
    ): Outcome<PermissionRequest> = guarded {
        if (reviewNote.isBlank()) {
            return@guarded Outcome.Failure(
                AppError.Validation("reviewNote", "سبب الرفض مطلوب ليصل إلى مقدّم الطلب")
            )
        }
        val entity = permissionDao.findById(requestId)
            ?: return@guarded Outcome.Failure(AppError.NotFound("طلب صلاحية", requestId))
        val current = entity.toDomain()
        if (!current.isPending) {
            return@guarded Outcome.Failure(
                AppError.Conflict("الطلب ${current.id} حُسم مسبقاً بحالة ${current.status}")
            )
        }
        val rejected = current.copy(
            status = PermissionRequestStatus.REJECTED,
            reviewedByUserId = reviewerUserId,
            reviewedByName = reviewerName,
            reviewNote = reviewNote,
            reviewedAt = TimeSource.now()
        )
        permissionDao.upsert(rejected.toEntity())
        Outcome.Success(rejected)
    }

    /**
     * هل يوجد اعتماد ساري؟ لا يجوز أن يعتمد مقدّم الطلب طلبه بنفسه،
     * ولا يُعدّ الطلب سارياً بلا توقيع.
     */
    override suspend fun hasApproval(
        requesterUserId: String,
        action: String,
        targetEntity: String,
        targetId: String?
    ): Outcome<Boolean> = guarded {
        // لقطة واحدة من الطلبات المعتمدة ثم فلترة في الذاكرة (الجدول صغير بطبيعته).
        val approved = permissionDao
            .observeByStatus(PermissionRequestStatus.APPROVED.name)
            .first()
        Outcome.Success(
            approved.any { row ->
                row.requesterUserId == requesterUserId &&
                    row.action == action &&
                    row.targetEntity == targetEntity &&
                    row.targetId == targetId &&
                    !row.signatureData.isNullOrBlank()
            }
        )
    }
}