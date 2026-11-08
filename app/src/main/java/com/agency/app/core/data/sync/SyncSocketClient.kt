package com.agency.app.core.data.sync

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlin.coroutines.resume

/**
 * قناة المزامنة الحيّة.
 *
 * القاعدة الحاكمة: الواجهة لا تلمس الشبكة أبداً. كل ما يصل هنا يُمرَّر إلى
 * [SyncManager] الذي يكتب في Room، ثم تتفرّع التحديثات إلى الشاشات عبر `Flow`.
 * فإن انقطعت القناة لم تنقطع القراءة — الشاشات تعمل من القاعدة، والقناة
 * تُلحق التأخير لا تُسقط النظام.
 *
 * وإعادة الاتصال ليست حلقة ساذجة: [BackoffPolicy] يضاعف التأخير مع تذبذب،
 * لأن عودة الشبكة لوكالة كاملة تدفع عشرات الأجهزة للاتصال في اللحظة نفسها.
 *
 * حالة الاتصال مُعرَّضة في [state] كي يعرف المستخدم سبب تأخّر التحديث بدل
 * أن يحدّق في شاشة جامدة بلا تفسير.
 */
@Singleton
class SyncSocketClient @Inject constructor(
    private val syncManager: SyncManager,
    private val backoff: BackoffPolicy
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client: OkHttpClient = OkHttpClient.Builder()
        /*
         * نبضة دورية تكشف الاتصال الميت: شبكة تُغلق بلا FIN تُبقي المقبس
         * "مفتوحاً" ظاهرياً بينما لا يمرّ شيء، فالصمت نفسه يُفسَّر انقطاعاً.
         */
        .pingInterval(PING_INTERVAL_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    /** عدد المحاولات المتتالية حتى الآن — تُعرض في شاشة التشخيص. */
    private val _attempts = MutableStateFlow(0)
    val attempts: StateFlow<Int> = _attempts.asStateFlow()

    private var socket: WebSocket? = null
    private var supervisor: Job? = null

    /**
     * يبدأ الاتصال ويُبقي القناة حيّة.
     *
     * الاستدعاء المتكرر آمن: إن كانت الحلقة تعمل لا يُفتح اتصال ثانٍ،
     * وتغيير العنوان يتطلّب [disconnect] أولاً حتى لا تبقى قناة معلّقة
     * على خادم قديم تُدسّ أحداثه في نفس القاعدة.
     */
    fun connect(url: String) {
        if (supervisor?.isActive == true) return
        supervisor = scope.launch { runLoop(url) }
    }

    /**
     * يقطع الاتصال ويتوقف عن إعادة المحاولة.
     *
     * يُستدعى عند تسجيل الخروج: إبقاء القناة مفتوحة بعد خروج المستخدم يعني
     * استمرار وصول أحداث دور لم يعد له صاحب على الجهاز.
     */
    fun disconnect() {
        supervisor?.cancel()
        supervisor = null
        socket?.close(NORMAL_CLOSURE, "طلب العميل")
        socket = null
        _attempts.value = 0
        _state.value = ConnectionState.Idle
    }

    /**
     * حلقة الاتصال: تفتح، تنتظر النهاية، تُعلن التأخير القادم، تنام، وتعيد.
     *
     * [delay] قبل كل محاولة هو ما يمنع الإلحاح. لاحظ أن المحاولة الأولى
     * لا تنتظر (تأخيرها صفر منطقياً) لأن المستخدم فتح التطبيق للتوّ ولا
     * معنى لتعطيله ثانية بلا سبب.
     */
    private suspend fun runLoop(url: String) {
        var attempt = 0
        while (true) {
            attempt += 1
            if (!backoff.shouldRetry(attempt)) {
                _state.value = ConnectionState.GaveUp(attempt - 1)
                return
            }
            _state.value = ConnectionState.Connecting(attempt)
            openOnce(url)
            _attempts.value = attempt
            val wait = backoff.delayFor(attempt)
            _state.value = ConnectionState.Reconnecting(wait, attempt)
            delay(wait)
        }
    }

    /**
     * يفتح مقبساً واحداً ويُعلّق حتى يُغلق أو يفشل.
     *
     * الإغلاق بخطأ والانقطاع الطبيعي يُعاملان سواء: كلاهما يعني «جرّب ثانية»،
     * لأن التمييز بينهما لا يغيّر القرار شيئاً.
     */
    private suspend fun openOnce(url: String) = suspendCancellableCoroutine { cont ->
        val request = Request.Builder().url(url).build()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _state.value = ConnectionState.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch { handleFrame(text) }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(NORMAL_CLOSURE, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (cont.isActive) cont.resume(Unit)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (cont.isActive) cont.resume(Unit)
            }
        }
        val created = client.newWebSocket(request, listener)
        cont.invokeOnCancellation { created.cancel() }
        socket = created
    }

    /**
     * يحلّل إطاراً وارداً ويُمرّره للـ[SyncManager].
     *
     * يقبل مغلّفاً كاملاً (`eventId`/`type`/`payload`) وهو الشكل القياسي.
     * وما لا يُفهم يُتجاهل بصمت بدل أن يُسقط الحلقة: رسالة تالفة واحدة
     * من خادم مختلّ لا يجوز أن توقف المزامنة كلها.
     */
    private suspend fun handleFrame(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed == "PING") return

        val envelope = runCatching {
            json.decodeFromString<SyncEnvelope>(trimmed)
        }.getOrNull()

        if (envelope != null && envelope.eventId.isNotBlank()) {
            syncManager.ingest(envelope)
            return
        }

        // احتياطي: بعض الخوادم تُرسل {eventId, type, payload} بترتيب مفاتيح
        // مختلف أو مع حقول إضافية تُفشل decodeToString الصارم.
        runCatching {
            val obj = json.decodeFromString<JsonObject>(trimmed)
            val id = obj.stringOrNull("eventId")
            val type = obj.stringOrNull("type")
            val payload = obj.stringOrNull("payload")
            if (id != null && type != null && payload != null) {
                SyncEnvelope(id, type, payload)
            } else {
                null
            }
        }.getOrNull()?.let { syncManager.ingest(it) }
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    /** حالة القناة كما تظهر في شريط المزامنة. */
    sealed interface ConnectionState {
        data object Idle : ConnectionState
        data class Connecting(val attempt: Int) : ConnectionState
        data object Connected : ConnectionState

        /** [nextDelayMs] هي المهلة المعلنة قبل المحاولة القادمة. */
        data class Reconnecting(val nextDelayMs: Long, val attempt: Int) : ConnectionState

        /** توقّفنا عن المحاولة — يحدث فقط بسياسة ذات حد محدد. */
        data class GaveUp(val attempts: Int) : ConnectionState
    }

    private companion object {
        const val NORMAL_CLOSURE = 1_000
        const val PING_INTERVAL_SECONDS = 20L
    }
}