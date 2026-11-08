package com.agency.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.data.auth.SecureStore
import com.agency.app.core.data.auth.SessionState
import com.agency.app.core.data.local.AppDatabase
import com.agency.app.core.data.repository.UserRepositoryImpl
import com.agency.app.core.domain.model.Outcome
import com.agency.app.core.domain.model.UserRole
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * حارس تدفق الإقلاع — يمنع رجوع عطب «الدوّار الأبدي».
 *
 * العطب الذي وقع فعلاً على جهاز حقيقي: الحالة الابتدائية `Loading`، ولا تخرج
 * منها إلا بـ`bootstrap()`، وكان `bootstrap()` مربوطاً بـ`LoginScreen` التي
 * لا تُركَّب إلا بعد الخروج من `Loading`. دائرة مغلقة: التطبيق لا يفتح أبداً،
 * والـ CI أخضر، والـ APK يُبنى ويُرفع بلا اعتراض.
 *
 * لم يكشفه الـ CI لأن `assembleDebug` يترجم ولا يشغّل، ولأن اختباراتنا كانت
 * تختبر المستودعات لا دورة حياة الجلسة. الكشف الحقيقي كان بتثبيت الـ APK
 * وتشغيله وقراءة الشاشة.
 *
 * هذا الملف يحوّل الدرس إلى قيد آلي: أي محاولة لإعادة ربط الإقلاع بمسار
 * واجهة، أو لإسقاط مسار الهبوط عند الفشل، تُسقط الاختبار قبل الدمج.
 *
 * ملاحظة مقصودة: نستخدم `SecureStore` الحقيقي لا بديلاً. داخل Robolectric
 * لا يتوفّر `AndroidKeyStore`، فيسلك `SecureStore` مسار الانحدار المعلَن
 * إلى التفضيلات العادية. أي أن هذه الاختبارات تُغطّي مسار الفشل في التشفير
 * أيضاً — وهو المسار الذي يهم ألا يُسقط التطبيق عند الإقلاع.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BootFlowGuardTest {

    private lateinit var db: AppDatabase
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userRepository = UserRepositoryImpl(db.userDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun newAuthManager(): AuthManager =
        AuthManager(SecureStore(context), userRepository)

    /**
     * القاعدة الأولى: الحالة الابتدائية `Loading`.
     *
     * لو صارت `SignedOut` ابتداءً لبدا الإقلاع «يعمل» ظاهرياً، لكن الجلسة
     * المحفوظة كانت ستُهمَل عند كل تشغيل — أي خروج صامت من الحساب عند كل
     * فتح للتطبيق. إثبات `Loading` يحمي الأمرين: لا تفريع قبل التهيئة،
     * والجلسة تُقرأ قبل التفريع.
     */
    @Test
    fun `initial state is Loading so bootstrap owns the transition`() {
        assertEquals(SessionState.Loading, newAuthManager().session.value)
    }

    /**
     * جوهر الحارس: الإقلاع ينقل الحالة **بلا أي شاشة مركَّبة**.
     *
     * لا Composable ولا ViewModel هنا. نجاح هذا الاختبار يثبت أن التهيئة
     * تعمل عند طبقة الجلسة نفسها، وأن أي ربط لها بشاشة هو ربط زائد لا شرط
     * بقاء. هذا بالضبط ما كان مكسوراً: كان البقاء مشروطاً بالشاشة.
     */
    @Test
    fun `bootstrap leaves Loading without any screen mounted`() = runTest {
        val authManager = newAuthManager()
        val result = authManager.bootstrap(AuthManager.DEFAULT_ADMIN_PASSWORD)

        assertTrue("bootstrap يجب أن ينجح", result is Outcome.Success)
        assertNotSame(
            "الحالة بعد الإقلاع يجب ألا تبقى Loading",
            SessionState.Loading,
            authManager.session.value
        )
        assertEquals(SessionState.SignedOut, authManager.session.value)
    }

    /**
     * أول تشغيل يزرع المدير، وبوابة التغيير لا تُفرض قبل وجود جلسة.
     *
     * الربط مقصود: كلمة المرور الافتراضية منشورة في الكود، فحساب المدير
     * المزروع بها يجب أن يُطلب تغييرها قبل توقيع اعتماد أو تعديل سعر.
     */
    @Test
    fun `seeded admin exists after first bootstrap`() = runTest {
        newAuthManager().bootstrap(AuthManager.DEFAULT_ADMIN_PASSWORD)

        val found = userRepository.findByUsername(AuthManager.SEED_ADMIN_USERNAME)
        assertTrue(found is Outcome.Success)
        val user = (found as Outcome.Success).value
        assertNotNull("يجب أن يُزرع حساب المدير في أول تشغيل", user)
        assertEquals(UserRole.ADMIN, user!!.role)
        assertTrue("المدير المزروع يجب أن يكون نشِطاً", user.isActive)
    }

    /**
     * دخول المدير بكلمة المرور الافتراضية يفتح بوابة التغيير الإلزامي.
     *
     * هذا هو البند الأمني نفسه تحت الحماية: لو عُطِّلت البوابة سهواً،
     * يبقى التطبيق يعمل ويبدو سليماً — وهذا أخطر من الانهيار.
     */
    @Test
    fun `admin signing in with default password must change it`() = runTest {
        val authManager = newAuthManager()
        authManager.bootstrap(AuthManager.DEFAULT_ADMIN_PASSWORD)
        assertFalse(
            "لا بوابة قبل الدخول: الجلسة ليست نشطة بعد",
            authManager.mustChangePassword.value
        )

        val signIn = authManager.signIn(
            AuthManager.SEED_ADMIN_USERNAME,
            AuthManager.DEFAULT_ADMIN_PASSWORD
        )
        assertTrue("دخول المدير يجب أن ينجح", signIn is Outcome.Success)
        assertTrue(
            "الدخول بالكلمة الافتراضية يجب أن يفرض التغيير",
            authManager.mustChangePassword.value
        )
    }

    /**
     * تغيير كلمة المرور يرفع البوابة، ويعيد الدخول بالكلمة الجديدة.
     *
     * والتحقق من القديمة حقيقي لا شكلي: بدونه يستطيع من أخذ جهازاً مفتوحاً
     * تغيير كلمة مرور المدير وحيازة الحساب نهائياً.
     */
    @Test
    fun `changing the default password lifts the gate`() = runTest {
        val authManager = newAuthManager()
        authManager.bootstrap(AuthManager.DEFAULT_ADMIN_PASSWORD)
        authManager.signIn(AuthManager.SEED_ADMIN_USERNAME, AuthManager.DEFAULT_ADMIN_PASSWORD)

        val wrongCurrent = authManager.changePassword("wrong-current", "Kayan@2026")
        assertTrue(
            "تغيير الكلمة بكلمة حالية خاطئة يجب أن يُرفض",
            wrongCurrent is Outcome.Failure
        )

        val changed = authManager.changePassword(
            AuthManager.DEFAULT_ADMIN_PASSWORD,
            "Kayan@2026"
        )
        assertTrue("تغيير الكلمة بالكلمة الصحيحة يجب أن ينجح", changed is Outcome.Success)
        assertFalse(
            "بعد التغيير لا تُفرض البوابة",
            authManager.mustChangePassword.value
        )
    }

    /**
     * الفشل يهبط إلى حالة صالحة، لا إلى انتظار أبدي.
     *
     * المسار المكسور سابقاً: أي استثناء في الإقلاع ⇒ لا أحد يكتب الحالة
     * ⇒ دوّار بلا رسالة ولا مخرج. هنا نتحقق أن الفشل اللاحق لا يترك
     * الجلسة في `Loading`.
     */
    @Test
    fun `a failed sign-in never leaves the session in Loading`() = runTest {
        val authManager = newAuthManager()
        authManager.bootstrap(AuthManager.DEFAULT_ADMIN_PASSWORD)
        val afterBootstrap = authManager.session.value

        val bad = authManager.signIn(
            AuthManager.SEED_ADMIN_USERNAME,
            "definitely-not-the-password"
        )

        assertTrue("الدخول بكلمة مرور خاطئة يجب أن يفشل", bad is Outcome.Failure)
        assertEquals(
            "الفشل لا يجوز أن يغيّر حالة الجلسة القائمة",
            afterBootstrap,
            authManager.session.value
        )
        assertNotSame(
            "لا مخرج معلّق: الحالة صالحة وليست Loading",
            SessionState.Loading,
            authManager.session.value
        )
    }

    /**
     * الجلسة تُعاد بناؤها من المعرّف المحفوظ بعد إعادة التشغيل.
     *
     * هذا هو الغرض المعلن لتخزين المعرّف: لا كلمة مرور ولا بصمة، فقط معرّف
     * يُستخدم لإعادة قراءة المستخدم من Room. إثباته يمنع «إصلاحاً» يجعل كل
     * تشغيل يبدأ من الصفر بدعوى تبسيط الإقلاع.
     */
    @Test
    fun `saved session is restored on next bootstrap`() = runTest {
        val first = newAuthManager()
        first.bootstrap(AuthManager.DEFAULT_ADMIN_PASSWORD)
        first.signIn(AuthManager.SEED_ADMIN_USERNAME, AuthManager.DEFAULT_ADMIN_PASSWORD)
        assertTrue("الدخول يجب أن يفتح جلسة نشطة", first.session.value is SessionState.Active)

        // نسخة جديدة تقرأ نفس التخزين — محاكاة إعادة تشغيل التطبيق.
        val restarted = newAuthManager()
        restarted.bootstrap(AuthManager.DEFAULT_ADMIN_PASSWORD)

        assertTrue(
            "الجلسة المحفوظة يجب أن تُعاد بناؤها من المعرّف",
            restarted.session.value is SessionState.Active
        )
    }
}