package com.cometchat.uikit.core.hub

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Server error mapped from the `{error:{code,message}}` envelope. */
public class HubApiException(
    public val httpStatus: Int,
    public val code: String,
    override val message: String,
    public val retryAfterSeconds: Long? = null
) : Exception(message)

/** Transport failure (DNS, TLS, timeouts) — no envelope available. */
public class HubNetworkException(cause: IOException) : Exception(cause)

// encodeDefaults: LoginRequest.token_request must ride the wire even when true is the default.
internal val JSON = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }
private val JSON_MEDIA = "application/json".toMediaType()
private val EMPTY_BODY = ByteArray(0).toRequestBody(null)

/**
 * GoChatHub REST client (api/openapi.yaml snapshot). Bearer token rides from
 * [HubStore]; non-2xx decodes the error envelope and throws [HubApiException].
 */
public class HubClient(store: HubSession, http: OkHttpClient = OkHttpClient()) {
    // ponytail: console-level request log (tag HubHttp) for device debugging
    private val http: OkHttpClient = http.newBuilder()
        .addInterceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            android.util.Log.d("HubHttp", request.method + " " + request.url.encodedPath + " -> " + response.code)
            response
        }
        .build()
    private val store: HubSession = store

    /** Fired on any 401 (session revoked server-side); app drops to login. */
    public var onUnauthorized: (() -> Unit)? = null

    /** Swap the deployment (login screen field); empty until first login. */
    public var baseUrl: String
        get() = store.baseUrl
        set(value) { store.baseUrl = value }

    private fun api(path: String): String = baseUrl.trimEnd('/') + "/api/v1" + path

    private fun enc(v: String): String = URLEncoder.encode(v, "UTF-8")

    // ---------- plumbing ----------

    private suspend fun await(call: Call): Response = suspendCancellableCoroutine { cont ->
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                cont.resumeWithException(HubNetworkException(e))
            }

            override fun onResponse(call: Call, response: Response) {
                cont.resume(response)
            }
        })
        cont.invokeOnCancellation { call.cancel() }
    }

    private suspend fun readBody(response: Response): String {
        check(response)
        return (response.body?.string() ?: "").also { response.close() }
    }

    private suspend fun check(response: Response) {
        if (response.isSuccessful) return
        val status = response.code
        val retryAfter = response.header("Retry-After")?.toLongOrNull()
        val body = try { response.body?.string().orEmpty() } catch (_: Exception) { "" }
        response.close()
        var code = "unknown"
        var message = "HTTP $status"
        try {
            if (body.isNotEmpty()) {
                JSON.decodeFromString(ErrorResponse.serializer(), body).error.let {
                    code = it.code
                    message = it.message
                }
            }
        } catch (_: Exception) { /* non-JSON error body */ }
        if (status == 401) onUnauthorized?.invoke()
        throw HubApiException(status, code, message, retryAfter)
    }

    private suspend fun <T> get(path: String, serializer: kotlinx.serialization.KSerializer<T>): T =
        JSON.decodeFromString(serializer, readBody(await(http.newCall(
            Request.Builder().url(api(path)).header("Authorization", "Bearer ${store.token}").get().build()))))

    private suspend fun send(method: String, path: String, body: String?): String {
        val rb = Request.Builder().url(api(path)).header("Authorization", "Bearer ${store.token}")
        when (method) {
            "POST" -> rb.post(body?.toRequestBody(JSON_MEDIA) ?: EMPTY_BODY)
            "PUT" -> rb.put(body?.toRequestBody(JSON_MEDIA) ?: EMPTY_BODY)
            "PATCH" -> rb.patch(body?.toRequestBody(JSON_MEDIA) ?: EMPTY_BODY)
            "DELETE" -> rb.delete(
                if (body != null) body.toRequestBody(JSON_MEDIA) else null)
            else -> throw IllegalArgumentException("bad method $method")
        }
        return readBody(await(http.newCall(rb.build())))
    }

    private suspend fun <T> send(method: String, path: String, body: String?, serializer: kotlinx.serialization.KSerializer<T>): T =
        JSON.decodeFromString(serializer, send(method, path, body))

    // ---------- auth ----------

    public suspend fun login(baseUrl: String, username: String, password: String): AuthResponseDto {
        this.baseUrl = baseUrl
        return send("POST", "/auth/login",
            JSON.encodeToString(LoginRequest.serializer(), LoginRequest(username, password)),
            AuthResponseDto.serializer())
    }

    public suspend fun logout() {
        send("POST", "/auth/logout", null)
    }

    // ---------- users ----------

    public suspend fun me(): UserDto = get("/users/me", UserDto.serializer())

    public suspend fun user(id: String): UserDto = get("/users/$id", UserDto.serializer())

    public suspend fun searchUsers(q: String): List<UserDto> = get("/users/search?q=${enc(q)}", ListSerializer(UserDto.serializer()))

    public suspend fun updateMe(body: UpdateUserDto): UserDto =
        send("PATCH", "/users/me", JSON.encodeToString(UpdateUserDto.serializer(), body), UserDto.serializer())

    public suspend fun preferences(): Preferences = get("/users/me/preferences", Preferences.serializer())

    public suspend fun updatePreferences(body: UpdatePreferencesDto): Preferences =
        send("PATCH", "/users/me/preferences",
            JSON.encodeToString(UpdatePreferencesDto.serializer(), body), Preferences.serializer())

    public suspend fun notificationModes(): List<NotificationModeDto> =
        get("/users/me/notifications", ListSerializer(NotificationModeDto.serializer()))

    public suspend fun setNotificationMode(body: SetNotificationModeRequest) {
        send("POST", "/users/me/notifications", JSON.encodeToString(SetNotificationModeRequest.serializer(), body))
    }

    // ---------- contacts ----------

    public suspend fun contacts(): List<ContactDto> = get("/contacts", ListSerializer(ContactDto.serializer()))

    public suspend fun addContact(userId: String): ContactDto =
        send("POST", "/contacts", JSON.encodeToString(AddContactRequest.serializer(), AddContactRequest(userId)),
            ContactDto.serializer())

    public suspend fun removeContact(contactId: String) {
        send("DELETE", "/contacts/$contactId", null)
    }

    // ---------- rooms ----------

    public suspend fun rooms(): List<RoomDto> = get("/rooms", ListSerializer(RoomDto.serializer()))

    public suspend fun room(id: String): RoomDto = get("/rooms/$id", RoomDto.serializer())

    public suspend fun createRoom(body: CreateRoomRequest): RoomDto =
        send("POST", "/rooms", JSON.encodeToString(CreateRoomRequest.serializer(), body), RoomDto.serializer())

    public suspend fun updateRoom(id: String, body: UpdateRoomDto): RoomDto =
        send("PATCH", "/rooms/$id", JSON.encodeToString(UpdateRoomDto.serializer(), body), RoomDto.serializer())

    public suspend fun archiveRoom(id: String) {
        send("DELETE", "/rooms/$id", null)
    }

    public suspend fun setMemberArchive(id: String, archived: Boolean) {
        send("PUT", "/rooms/$id/archived",
            JSON.encodeToString(MemberArchiveRequestDto.serializer(), MemberArchiveRequestDto(archived)))
    }

    public suspend fun roomMembers(id: String): List<UserDto> =
        get("/rooms/$id/members", ListSerializer(UserDto.serializer()))

    public suspend fun addRoomMember(id: String, userId: String) {
        send("POST", "/rooms/$id/members",
            JSON.encodeToString(AddMemberRequest.serializer(), AddMemberRequest(userId)))
    }

    public suspend fun removeRoomMember(id: String, userId: String) {
        send("DELETE", "/rooms/$id/members/$userId", null)
    }

    public suspend fun pinMessage(roomId: String, messageId: String) {
        send("PUT", "/rooms/$roomId/pin",
            JSON.encodeToString(PinRequestDto.serializer(), PinRequestDto(messageId)))
    }

    public suspend fun unpinMessage(roomId: String) {
        send("DELETE", "/rooms/$roomId/pin", null)
    }

    public suspend fun markRead(roomId: String, messageId: String) {
        send("POST", "/rooms/$roomId/read",
            JSON.encodeToString(PinRequestDto.serializer(), PinRequestDto(messageId)))
    }

    // ---------- invites ----------

    public suspend fun invites(): List<InviteDto> = get("/invites", ListSerializer(InviteDto.serializer()))

    @kotlinx.serialization.Serializable
    private data class InviteId(@kotlinx.serialization.SerialName("invite_id") val inviteId: String)

    public suspend fun createInvite(roomId: String, userId: String): String {
        val body = JSON.encodeToString(
            CreateInviteRequest.serializer(), CreateInviteRequest(roomId, userId))
        return send("POST", "/invites", body, InviteId.serializer()).inviteId
    }

    public suspend fun acceptInvite(id: String) {
        send("POST", "/invites/$id/accept", null)
    }

    public suspend fun declineInvite(id: String) {
        send("POST", "/invites/$id/decline", null)
    }

    // ---------- messages ----------

    public suspend fun messages(roomId: String, limit: Int = 50, before: String? = null): MessagePageDto {
        val sb = StringBuilder("/rooms/").append(roomId)
            .append("/messages?limit=").append(limit)
        if (!before.isNullOrEmpty()) sb.append("&before=").append(enc(before))
        return get(sb.toString(), MessagePageDto.serializer())
    }

    public suspend fun message(id: String): MessageDto = get("/messages/$id", MessageDto.serializer())

    public suspend fun createMessage(roomId: String, body: CreateMessageRequest): MessageDto =
        send("POST", "/rooms/$roomId/messages",
            JSON.encodeToString(CreateMessageRequest.serializer(), body), MessageDto.serializer())

    public suspend fun updateMessage(id: String, body: UpdateMessageDto): MessageDto =
        send("PATCH", "/messages/$id",
            JSON.encodeToString(UpdateMessageDto.serializer(), body), MessageDto.serializer())

    public suspend fun deleteMessage(id: String): MessageDto =
        send("DELETE", "/messages/$id", null, MessageDto.serializer())

    public suspend fun react(messageId: String, emoji: String) {
        send("POST", "/messages/$messageId/reactions",
            JSON.encodeToString(ReactRequestDto.serializer(), ReactRequestDto(emoji)))
    }

    public suspend fun unreact(messageId: String, emoji: String) {
        send("DELETE", "/messages/$messageId/reactions/${enc(emoji)}", null)
    }

    public suspend fun reactions(messageId: String): List<ReactionDto> =
        get("/messages/$messageId/reactions", ListSerializer(ReactionDto.serializer()))

    // ---------- attachments ----------

    public suspend fun createUpload(body: CreateUploadRequest): UploadSessionDto =
        send("POST", "/attachments",
            JSON.encodeToString(CreateUploadRequest.serializer(), body), UploadSessionDto.serializer())

    /** Presigned PUT — no bearer header, the URL carries the signature. */
    public suspend fun uploadToPresigned(url: String, payload: ByteArray, contentType: String) {
        val response = await(http.newCall(Request.Builder().url(url)
            .put(payload.toRequestBody(contentType.toMediaType()))
            .build()))
        check(response)
        response.close()
    }

    public suspend fun completeUpload(id: String): AttachmentDto =
        send("POST", "/attachments/$id/complete", null, AttachmentDto.serializer())

    public suspend fun attachment(id: String): AttachmentDto = get("/attachments/$id", AttachmentDto.serializer())

    public suspend fun deleteAttachment(id: String) {
        send("DELETE", "/attachments/$id", null)
    }

    // ---------- devices / push ----------

    public suspend fun vapidPublicKey(): String =
        get("/push/vapid", VapidResponse.serializer()).publicKey

    public suspend fun registerDevice(body: RegisterDeviceRequest): RegisterDeviceResponseDto =
        send("POST", "/devices", JSON.encodeToString(RegisterDeviceRequest.serializer(), body),
            RegisterDeviceResponseDto.serializer())

    public suspend fun validateDevice(deviceId: String, token: String) {
        send("POST", "/devices/$deviceId/validate",
            JSON.encodeToString(ValidateDeviceRequestDto.serializer(), ValidateDeviceRequestDto(token)))
    }

    public suspend fun renewDevice(deviceId: String, body: PushRegistrationDto) {
        send("PATCH", "/devices/$deviceId", JSON.encodeToString(PushRegistrationDto.serializer(), body))
    }

    public suspend fun deleteDevice(deviceId: String) {
        send("DELETE", "/devices/$deviceId", null)
    }

    public suspend fun version(): VersionResponse = get("/version", VersionResponse.serializer())
}