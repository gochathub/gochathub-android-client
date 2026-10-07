package com.cometchat.uikit.core.hub

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.add
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

public sealed interface HubSocketState {
    public data object Disconnected : HubSocketState
    public data object Connecting : HubSocketState
    public data object Connected : HubSocketState
}

/**
 * GoChatHub WebSocket (docs/WEBSOCKETS.md, internal/ws/client.go).
 *
 * Client frames: subscribe/unsubscribe {room_id}, ack {message_ids},
 * read {room_id,message_id}, typing.started/stopped {room_id}.
 * Server events arrive as [WsEnvelope] on [events]. Server pings every 30s —
 * OkHttp answers control pings automatically. Reconnect with capped backoff;
 * subscribers resync via REST on [onReconnected].
 */
public class HubSocket(
    private val store: HubSession,
    private val http: OkHttpClient = OkHttpClient(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val json: Json = JSON
) {
    private var socket: WebSocket? = null
    private var reconnectJob: Job? = null
    private val subscribed = LinkedHashSet<String>()
    private var backoffSeconds = 1L
    private var closedByUser = false

    public val events: MutableSharedFlow<WsEnvelope> = MutableSharedFlow(extraBufferCapacity = 64)
    private val _state = MutableStateFlow<HubSocketState>(HubSocketState.Disconnected)
    public val state: StateFlow<HubSocketState> = _state

    /** Set by the app layer: refetch rooms/messages over REST after a drop. */
    public var onReconnected: (() -> Unit)? = null

    public fun connect() {
        if (socket != null) return
        closedByUser = false
        val base = store.baseUrl.trimEnd('/')
        if (base.isEmpty() || !store.hasSession()) return
        val url = base.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://") + "/api/v1/ws"
        _state.value = HubSocketState.Connecting
        val request = Request.Builder().url(url)
            .header("Authorization", "Bearer ${store.token}")
            .build()
        socket = http.newWebSocket(request, listener)
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _state.value = HubSocketState.Connected
            backoffSeconds = 1
            synchronized(subscribed) {
                for (room in subscribed) sendSubscribe(room)
            }
            onReconnected?.invoke()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val env = try { json.decodeFromString(WsEnvelope.serializer(), text) } catch (_: Exception) { null } ?: return
            events.tryEmit(env)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            socket = null
            _state.value = HubSocketState.Disconnected
            scheduleReconnect()
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            socket = null
            _state.value = HubSocketState.Disconnected
            if (!closedByUser) scheduleReconnect()
        }
    }

    private fun scheduleReconnect() {
        if (closedByUser || reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(backoffSeconds * 1000)
            backoffSeconds = (backoffSeconds * 2).coerceAtMost(60)
            socket ?: connect()
        }
    }

    private fun sendJson(text: String): Boolean =
        socket?.send(text) ?: false

    public fun subscribe(roomId: String) {
        synchronized(subscribed) { subscribed.add(roomId) }
        if (_state.value == HubSocketState.Connected) sendSubscribe(roomId)
    }

    private fun sendSubscribe(roomId: String) {
        sendJson(obj("subscribe") { put("room_id", JsonPrimitive(roomId)) })
    }

    public fun unsubscribe(roomId: String) {
        synchronized(subscribed) { subscribed.remove(roomId) }
        sendJson(obj("unsubscribe") { put("room_id", JsonPrimitive(roomId)) })
    }

    /** Delivered receipts (ADR-009) — batch ack of message ids. */
    public fun ack(messageIds: List<String>) {
        if (messageIds.isEmpty()) return
        sendJson(obj("ack") {
            put("message_ids", buildJsonArray { messageIds.forEach { add(JsonPrimitive(it)) } })
        })
    }

    /** Advance the read cursor. */
    public fun read(roomId: String, messageId: String) {
        sendJson(obj("read") {
            put("room_id", JsonPrimitive(roomId))
            put("message_id", JsonPrimitive(messageId))
        })
    }

    public fun typingStarted(roomId: String) {
        sendJson(obj("typing.started") { put("room_id", JsonPrimitive(roomId)) })
    }

    public fun typingStopped(roomId: String) {
        sendJson(obj("typing.stopped") { put("room_id", JsonPrimitive(roomId)) })
    }

    private inline fun obj(type: String, body: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): String =
        json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.buildJsonObject {
                put("type", JsonPrimitive(type))
                body()
            })

    public fun close() {
        closedByUser = true
        reconnectJob?.cancel()
        socket?.close(1000, "bye")
        socket = null
        _state.value = HubSocketState.Disconnected
    }
}