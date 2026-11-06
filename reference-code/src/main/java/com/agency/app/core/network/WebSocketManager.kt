// core/network/WebSocketManager.kt
package com.agency.app.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor() {
    
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .build()

    private val _messages = MutableSharedFlow<SyncMessage>()
    val messages: SharedFlow<SyncMessage> = _messages

    private val _connectionState = MutableSharedFlow<ConnectionState>()
    val connectionState: SharedFlow<ConnectionState> = _connectionState

    fun connect(serverUrl: String) {
        val request = Request.Builder()
            .url(serverUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.tryEmit(ConnectionState.CONNECTED)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val message = parseMessage(text)
                _messages.tryEmit(message)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                _connectionState.tryEmit(ConnectionState.DISCONNECTED)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.tryEmit(ConnectionState.ERROR)
            }
        })
    }

    fun send(message: SyncMessage) {
        val json = JSONObject().apply {
            put("type", message.type.name)
            put("payload", message.payload)
            put("timestamp", message.timestamp)
            put("userId", message.userId)
        }
        webSocket?.send(json.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
    }

    private fun parseMessage(text: String): SyncMessage {
        val json = JSONObject(text)
        return SyncMessage(
            type = MessageType.valueOf(json.getString("type")),
            payload = json.getJSONObject("payload"),
            timestamp = json.getLong("timestamp"),
            userId = json.getString("userId")
        )
    }
}

data class SyncMessage(
    val type: MessageType,
    val payload: JSONObject,
    val timestamp: Long,
    val userId: String
)

enum class MessageType {
    ORDER_CREATED,
    ORDER_PREPARED,
    ORDER_SENT,
    ORDER_DELIVERED,
    RECEIPT_CREATED,
    PRODUCT_UPDATED,
    USER_ACTION
}

enum class ConnectionState {
    CONNECTED,
    DISCONNECTED,
    ERROR,
    RECONNECTING
}
