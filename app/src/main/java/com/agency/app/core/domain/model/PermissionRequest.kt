package com.agency.app.core.domain.model

import kotlinx.datetime.Instant

/** حالة طلب الصلاحية. */
enum class PermissionRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}

/**
 * طلب صلاحية — المحاسب للقراءة فقط، وأي إجراء كتابة يحتاج موافقة المدير
 * وتوقيعاً رقمياً قبل التنفيذ.
 */
data class PermissionRequest(
    val id: String,
    val requesterUserId: String,
    val requesterName: String,
    val requesterRole: UserRole,
    val action: String,
    val targetEntity: String,
    val targetId: String?,
    val reason: String,
    val status: PermissionRequestStatus = PermissionRequestStatus.PENDING,
    val reviewedByUserId: String? = null,
    val reviewedByName: String? = null,
    val reviewNote: String? = null,
    val signatureData: String? = null,
    val createdAt: Instant,
    val reviewedAt: Instant? = null
) {
    val isPending: Boolean get() = status == PermissionRequestStatus.PENDING
    val isApproved: Boolean get() = status == PermissionRequestStatus.APPROVED
    val isSigned: Boolean get() = !signatureData.isNullOrBlank()
}
