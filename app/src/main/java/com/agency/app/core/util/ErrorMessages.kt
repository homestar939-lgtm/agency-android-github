package com.agency.app.core.util

import com.agency.app.core.domain.model.AppError

/**
 * رسائل الأخطاء بالعربية، في موضع واحد.
 *
 * الواجهة لا تعرض `AppError` الخام لأن نصّه إنجليزي/تقنيّ جزئياً، ولا تعيد
 * صياغته صياغة كاملة لأن ذلك يطمس أثر التشخيص. الحل هنا: صياغة عربية للسياق
 * مع الإبقاء على التفصيل القادم من الطبقة الأدنى (اسم الحقل، اسم الكيان).
 */
fun AppError.arabicMessage(): String = when (this) {
    is AppError.Network -> "تعذّر الاتصال بالشبكة. تحقّق من الاتصال وأعد المحاولة."
    is AppError.Permission -> message
    is AppError.Validation -> "قيمة غير صالحة في «$field»: $reason"
    is AppError.NotFound -> "العنصر المطلوب غير موجود ($entity: $id)."
    is AppError.Conflict -> "تعارض في البيانات: $message"
    is AppError.Unknown -> "حدث خطأ غير متوقع. أعد المحاولة."
}