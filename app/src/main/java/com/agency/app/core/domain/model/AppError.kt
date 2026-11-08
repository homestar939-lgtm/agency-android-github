package com.agency.app.core.domain.model

/** كل الأخطاء في النظام تُغلَّف بهذا النوع، ولا يُترك استثناء خام يصل للواجهة. */
sealed class AppError(val message: String, val cause: Throwable? = null) {

    data class Network(val reason: String, val throwable: Throwable? = null) :
        AppError(reason, throwable)

    data class Permission(val required: String) :
        AppError("لا تملك الصلاحية: $required")

    data class Validation(val field: String, val reason: String) :
        AppError("قيمة غير صالحة في $field: $reason")

    data class NotFound(val entity: String, val id: String) :
        AppError("$entity غير موجود: $id")

    data class Conflict(val reason: String) : AppError(reason)

    data class Unknown(val throwable: Throwable? = null) :
        AppError("حدث خطأ غير متوقع", throwable)

    companion object {
        fun <T> Throwable.toAppError(): AppError = when (this) {
            is AppException -> error
            else -> Unknown(this)
        }
    }
}

/** استثناء داخلي لحمل [AppError] عبر حدود coroutines دون فقدان النوع. */
class AppException(val error: AppError) : Exception(error.message, error.cause)
