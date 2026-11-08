package com.agency.app.core.data.sync

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.agency.app.BuildConfig
import com.agency.app.core.data.auth.AuthManager
import com.agency.app.core.data.auth.SessionState
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * يربط قناة المزامنة بدورة حياة الجلسة.
 *
 * الربط هنا لا في الشاشة: القناة يجب أن تُفتح مرة واحدة لكل جلسة، لا مرة
 * لكل شاشة تُفتح. ولو رُبطت بالواجهة لكان كل تنقّل يفتح ويغلق قناة، وهو
 * أسوأ من عدم المزامنة لأنه يُنتج اتصالات مهدورة وأحداثاً مكرّرة.
 *
 * وعند الخروج تُغلق القناة: إبقاؤها مفتوحة بعد خروج المستخدم يعني أن أحداث
 * دورٍ لم يعد له صاحب على الجهاز تستمر في الكتابة في القاعدة المشتركة.
 */
@Singleton
class SyncLifecycle @Inject constructor(
    private val authManager: AuthManager,
    private val socket: SyncSocketClient
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var bound = false

    /**
     * يبدأ المراقبة. آمن الاستدعاء أكثر من مرة.
     *
     * يُستدعى من `AgencyRoot` عند تركيب الجذر — أفضل من `Application` لأنه
     * يضمن أن الجلسة بُنيت بالفعل قبل محاولة الاتصال.
     */
    fun bind() {
        if (bound) return
        bound = true
        authManager.session
            .onEach { state ->
                when (state) {
                    is SessionState.Active -> {
                        val url = BuildConfig.SYNC_WS_URL
                        if (url.isNotBlank()) {
                            socket.connect(url)
                        }
                    }
                    SessionState.SignedOut, SessionState.Loading -> socket.disconnect()
                }
            }
            .launchIn(scope)
    }
}

/**
 * نقطة وصول Hilt من داخل `@Composable` بلا `ViewModel` وسيط.
 *
 * لماذا لا `hiltViewModel()`: [SyncLifecycle] ليس ViewModel ولا ينبغي أن
 * يكون — عمره هو عمر التطبيق لا عمر شاشة. تحويله إلى ViewModel كان سيجعله
 * يُنشأ ويُدمَّر مع كل تنقّل، وهو ما يُسقط سبب وجوده.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncLifecycleEntryPoint {
    fun syncLifecycle(): SyncLifecycle
}

/** يُنشئ الوصول لـ[SyncLifecycle] مرة واحدة داخل التركيب. */
@Composable
fun rememberSyncLifecycle(): SyncLifecycle {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        EntryPointAccessors.fromApplication(context, SyncLifecycleEntryPoint::class.java)
            .syncLifecycle()
    }
}