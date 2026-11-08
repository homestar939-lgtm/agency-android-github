package com.agency.app.core.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مخزن بيانات حساس مُشفَّر.
 *
 * القاعدة: كل ما يُخزَّن هنا يستطيع وحده فتح جلسة أو تغيير مسار مالي،
 * فلا يجوز أن يكون مقروءاً من نسخة احتياطية مسحوبة أو من جهاز مروّت.
 *
 * يُستخدم `EncryptedSharedPreferences` بمفتاح رئيسي محفوظ في `AndroidKeyStore`
 * (محمي بالعتاد عادةً)، فحتى نسخ ملف التفضيلات كما هو لا يُفيد شيئاً.
 *
 * الانحدار إلى التفضيلات العادية مقصود ومحدود: يحدث فقط على أجهزة يشذّ فيها
 * إنشاء المفتاح (نادر)، لأن الفشل الكامل في الإقلاع أسوأ من جلسة أقل تحصيناً.
 * ومع ذلك يُسجَّل السبب في [isEncrypted] حتى تُظهره شاشة الإعدادات للمدير،
 * فلا يبقى الانحدار سراً في الكود.
 */
@Singleton
class SecureStore @Inject constructor(context: Context) {

    private val prefs: SharedPreferences

    /** هل التخزين مشفَّر فعلاً على هذا الجهاز؟ تُعرض للمدير في الإعدادات. */
    val isEncrypted: Boolean

    init {
        val appContext = context.applicationContext
        val encrypted = runCatching {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as SharedPreferences
        }.getOrNull()

        if (encrypted != null) {
            prefs = encrypted
            isEncrypted = true
        } else {
            prefs = appContext.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
            isEncrypted = false
        }
    }

    fun getString(key: String): String? = prefs.getString(key, null)

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)

    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    private companion object {
        const val ENCRYPTED_PREFS_NAME = "agency.session.encrypted"
        const val FALLBACK_PREFS_NAME = "agency.session.fallback"
    }
}
