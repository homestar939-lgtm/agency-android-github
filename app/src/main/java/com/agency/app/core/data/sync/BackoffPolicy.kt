package com.agency.app.core.data.sync

import kotlin.random.Random

/**
 * سياسة إعادة الاتصال.
 *
 * فُصلت عن عميل الشبكة عمداً لتكون **قابلة للاختبار بلا شبكة**: التأخير
 * المتزايد هو ما يمنع عشرات الأجهزة من مهاجمة الخادم في اللحظة نفسها بعد
 * انقطاع، ولو بقي داخل حلقة `WebSocketListener` لما أمكن إثباته إلا يدوياً.
 *
 * [jitterRatio] ليس تجميلاً: بدونه تتصادف كل الأجهزة على المضاعف نفسه فتُنتج
 * عاصفة اتصالات عند كل عودة للشبكة. يُوزَّع عشوائياً حول القيمة الأساسية.
 */
class BackoffPolicy(
    val initialDelayMs: Long = 1_000L,
    val maxDelayMs: Long = 60_000L,
    val multiplier: Double = 2.0,
    val jitterRatio: Double = 0.2,
    val maxAttempts: Int = Int.MAX_VALUE,
    private val random: Random = Random.Default
) {

    init {
        require(initialDelayMs > 0) { "التأخير الأولي يجب أن يكون موجباً" }
        require(maxDelayMs >= initialDelayMs) { "الحد الأقصى لا يقل عن التأخير الأولي" }
        require(multiplier >= 1.0) { "المضاعف لا يقل عن 1" }
        require(jitterRatio in 0.0..0.9) { "نسبة التذبذب خارج المدى المعقول" }
    }

    /**
     * التأخير قبل المحاولة رقم [attempt] (تبدأ من 1 للأولى).
     *
     * القيمة الأساسية `initial * multiplier^(attempt-1)` مقيّدة بـ[maxDelayMs]،
     * ثم تُذبذب. والتذبذب مُقيَّد أيضاً بـ[maxDelayMs] حتى يبقى الحد الأعلى
     * تعهداً حقيقياً لا تقديراً.
     */
    fun delayFor(attempt: Int): Long {
        if (attempt <= 1) return initialDelayMs
        var base = initialDelayMs.toDouble()
        repeat(attempt - 1) {
            base *= multiplier
            if (base >= maxDelayMs.toDouble()) return@repeat
        }
        val capped = base.coerceAtMost(maxDelayMs.toDouble())
        if (jitterRatio == 0.0) return capped.toLong()
        val spread = capped * jitterRatio
        val low = (capped - spread).coerceAtLeast(1.0)
        val high = (capped + spread).coerceAtMost(maxDelayMs.toDouble())
        return if (high <= low) low.toLong() else random.nextLong(low.toLong(), high.toLong() + 1)
    }

    /** هل تستحق المحاولة رقم [attempt] أن تُجرَّب؟ */
    fun shouldRetry(attempt: Int): Boolean = attempt <= maxAttempts

    companion object {
        /**
         * سياسة الإنتاج: 1s → 2s → 4s … بسقف دقيقة.
         *
         * السقف عند دقيقة لأن انقطاع شبكة الوكالة قد يستمر ساعات، ومحاولة
         * كل ثانية طوال الليل تُستهلك البطارية بلا فائدة.
         */
        fun production(): BackoffPolicy = BackoffPolicy(
            initialDelayMs = 1_000L,
            maxDelayMs = 60_000L,
            multiplier = 2.0,
            jitterRatio = 0.2,
            maxAttempts = Int.MAX_VALUE
        )
    }
}