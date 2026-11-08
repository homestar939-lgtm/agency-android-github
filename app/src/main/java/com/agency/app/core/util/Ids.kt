package com.agency.app.core.util

import java.security.MessageDigest
import java.util.UUID
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * مولّد المعرّفات. كل سطر بيانات في النظام يحمل معرّفاً فريداً يُولَّد محلياً
 * حتى تعمل العمليات بلا اتصال، ويُرفع مع السجل عند العودة للشبكة.
 */
object Ids {
    fun new(): String = UUID.randomUUID().toString()

    /** رقم مستند مقروء للبشر: بادئة + طابع زمني + لاحقة عشوائية قصيرة. */
    fun document(prefix: String): String {
        val stamp = Clock.System.now().toEpochMilliseconds()
        val suffix = (1000..9999).random()
        return "$prefix-$stamp-$suffix"
    }

    fun orderNumber(): String = document("ORD")
    fun invoiceNumber(): String = document("INV")
    fun receiptNumber(): String = document("RCP")
}

/** الوقت: مصدر واحد للوقت الحالي في كل الطبقات. */
object TimeSource {
    fun now(): Instant = Clock.System.now()
}

/**
 * تجزئة كلمة المرور. تُخزَّن البصمة فقط، ولا تُسجَّل كلمة المرور الصريحة
 * في أي مكان من التطبيق.
 *
 * ملاحظة أمنية: SHA-256 مع تعليق ثابت مقبول لهذا النطاق الداخلي؛
 * الانتقال إلى PBKDF2/Argon2 لا يغيّر الواجهة هنا.
 */
object PasswordHasher {
    private const val PEPPER = "agency.b2b.v1"

    fun hash(plain: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$PEPPER|$plain".toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    fun matches(plain: String, storedHash: String): Boolean =
        hash(plain).equals(storedHash, ignoreCase = true)
}