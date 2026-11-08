package com.agency.app.core.ui.components

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * بوابة التحقق بالبصمة.
 *
 * تُستخدم قبل أي عملية مالية تتجاوز الحد المسموح (سندات كبيرة).
 * حين لا يتوفّر أي نوع بصمة على الجهاز نُعيد `false` ولا نُعلّق الطلب بصمت:
 * الشاشة تُظهر سبب الرفض للمستخدم.
 */
class BiometricGate(private val activity: FragmentActivity) {

    fun canAuthenticate(): Boolean {
        val manager = BiometricManager.from(activity)
        val allowed = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        return manager.canAuthenticate(allowed) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /** يُعلّق حتى ينجح التحقق أو يُلغى. يعيد `true` عند النجاح فقط. */
    suspend fun authenticate(title: String, subtitle: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            val prompt = BiometricPrompt(
                activity,
                androidx.core.content.ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (continuation.isActive) continuation.resume(false)
                    }

                    override fun onAuthenticationFailed() {
                        // الفشل الجزئي لا يُنهي العملية؛ النظام يعيد المحاولة تلقائياً.
                    }
                }
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText("إلغاء")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
                .build()
            prompt.authenticate(info)
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
        }
}

/** يُنشئ [BiometricGate] من سياق الشاشة الحالي، أو `null` إن لم تكن فعّالة. */
@Composable
fun rememberBiometricGate(): BiometricGate? {
    val context = LocalContext.current
    return remember(context) {
        val activity = context as? FragmentActivity
        activity?.let { BiometricGate(it) }
    }
}