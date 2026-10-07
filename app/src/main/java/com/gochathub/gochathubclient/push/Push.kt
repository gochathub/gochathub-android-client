package com.gochathub.gochathubclient.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.core.app.NotificationCompat
import com.gochathub.gochathubclient.MainActivity
import com.gochathub.gochathubclient.R
import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.PushRegistrationDto
import com.cometchat.uikit.core.hub.RegisterDeviceRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * UnifiedPush wiring (docs/UNIFIEDPUSH.md, verified contract): connector
 * registration with the server VAPID key, device registration/validation,
 * identifier-only chat.message payloads fetched over REST before display.
 */
public object Push {
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * App start: use the saved distributor, else pick one ourselves. With several
     * installed (e.g. DAVx5 ships an embedded FCM distributor) the connector's
     * default lookup returns false and registration silently stops — and the
     * project must never route through FCM, so ntfy is preferred by name and a
     * lone distributor is accepted; otherwise we skip.
     * ponytail: no picker UI; add one if a second real distributor is ever needed.
     */
    public fun register(context: Context) {
        val up = org.unifiedpush.android.connector.UnifiedPush
        val distributor = up.getSavedDistributor(context)
            ?: up.getDistributors(context).let { all ->
                all.firstOrNull { it == PREFERRED_DISTRIBUTOR } ?: all.singleOrNull()
            }
        if (distributor == null) return
        up.saveDistributor(context, distributor)
        scope.launch {
            try {
                val vapid = Hub.client.vapidPublicKey()
                up.register(context, "", vapid)
            } catch (_: Exception) {
                // no session yet — endpoint callbacks re-register on the next start
            }
        }
    }

    /** Endpoint from the connector: register (device 0) or renew (known id). */
    public fun onEndpoint(context: Context, url: String, publicKey: String, authSecret: String) {
        scope.launch {
            try {
                val existing = Hub.store.deviceId
                val registration = PushRegistrationDto(
                    endpoint = url, publicKey = publicKey, authSecret = authSecret)
                if (existing.isEmpty()) {
                    val response = Hub.client.registerDevice(
                        RegisterDeviceRequest(
                            platform = "android",
                            clientName = "GoChatHub",
                            clientVersion = "1.0.0",
                            pushRegistration = registration
                        )
                    )
                    Hub.store.deviceId = response.deviceId
                } else {
                    Hub.client.renewDevice(existing, registration)
                }
            } catch (_: Exception) { /* resync on next app start re-registers */ }
        }
    }

    public fun onRegistrationFailed() {
        // distributor gone/reinstalled on app start
        scope.launch {
            try { reRegister() } catch (_: Exception) { }
        }
    }

    public fun onUnregistered(context: Context) {
        scope.launch {
            try { reRegister() } catch (_: Exception) { }
        }
    }

    private suspend fun reRegister() {
        if (!Hub.store.hasSession()) return
        val vapid = Hub.client.vapidPublicKey()
        org.unifiedpush.android.connector.UnifiedPush.register(Hub.ctx, "", vapid)
    }

    /**
     * One decrypted push: either the validation ping token (raw base64) or a
     * chat.message identifiers payload. Fetch-then-render over REST either way.
     */
    public fun onMessage(context: Context, content: ByteArray) {
        scope.launch {
            val text = content.decodeToString()
            val payload = try {
                json.decodeFromString(JsonObject.serializer(), text)
            } catch (_: Exception) {
                null
            }
            when (payload?.get("type")?.jsonPrimitive?.content) {
                "chat.message" -> {
                    val roomId = payload["room_id"]?.jsonPrimitive?.content ?: return@launch
                    val messageId = payload["message_id"]?.jsonPrimitive?.content ?: return@launch
                    notify(context, roomId, messageId)
                }
                else -> validate(text)
            }
        }
    }

    private suspend fun validate(token: String) {
        val deviceId = Hub.store.deviceId
        if (deviceId.isEmpty()) return
        try {
            Hub.client.validateDevice(deviceId, token)
        } catch (_: Exception) { /* server retries by design via mode changes */ }
    }

    private suspend fun notify(context: Context, roomId: String, messageId: String) {
        try {
            val message = Hub.client.message(messageId)
            val room = Hub.roomById(roomId) ?: Hub.client.room(roomId).also { Hub.rememberRoom(it) }
            val title = room.name ?: message.author?.displayName ?: "GoChatHub"
            if (message.deletedAt != null) return
            show(context, title, message.body, roomId)
        } catch (_: Exception) { /* offline: resync fills it in */ }
    }

    private fun show(context: Context, title: String, body: String, roomId: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Messages", NotificationManager.IMPORTANCE_HIGH)
        )
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("room_id", roomId)
        }
        val pending = PendingIntent.getActivity(
            context, roomId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body.ifEmpty { "New message" })
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(roomId.hashCode(), notification)
    }

    private const val CHANNEL = "messages"
    private const val PREFERRED_DISTRIBUTOR = "io.heckel.ntfy"
}