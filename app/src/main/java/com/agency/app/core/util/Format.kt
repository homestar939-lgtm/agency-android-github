package com.agency.app.core.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * تنسيق التواريخ والأعداد لواجهة عربية.
 *
 * كل التحويلات تعتمد على [TimeZone.currentSystemDefault] لأن النظام يعمل
 * في منطقة الوكالة الزمنية، ولا نفترض UTC في أي شاشة.
 */
fun Instant.formattedDate(): String {
    val local = toLocalDateTime(TimeZone.currentSystemDefault())
    val month = local.monthNumber.toString().padStart(2, '0')
    val day = local.dayOfMonth.toString().padStart(2, '0')
    return "${local.year}/$month/$day"
}

fun Instant.formattedDateTime(): String {
    val local = toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = local.hour.toString().padStart(2, '0')
    val minute = local.minute.toString().padStart(2, '0')
    return "${formattedDate()} $hour:$minute"
}

/** «قبل 3 ساعات» — تُستخدم في الخط الزمني للطلبية. */
fun Instant.relative(): String {
    val diff = TimeSource.now().toEpochMilliseconds() - toEpochMilliseconds()
    if (diff < 0) return formattedDateTime()
    val minutes = diff / 60_000L
    return when {
        minutes < 1L -> "الآن"
        minutes < 60L -> "قبل $minutes دقيقة"
        minutes < 1_440L -> "قبل ${minutes / 60L} ساعة"
        minutes < 43_200L -> "قبل ${minutes / 1_440L} يوم"
        else -> formattedDate()
    }
}
