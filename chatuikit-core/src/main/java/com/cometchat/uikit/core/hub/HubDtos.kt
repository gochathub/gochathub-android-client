package com.cometchat.uikit.core.hub

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Server-shape DTOs for GoChatHub (api/openapi.yaml snapshot). Hand-mapped,
 * names mirroring the server structs 1:1; mappers convert to SDK models.
 */
@Serializable
public data class ErrorBody(
    val code: String,
    val message: String,
    /** Only on `two_factor_required`: redeem at /auth/login/2fa. */
    val challenge: String? = null
)

@Serializable
public data class ErrorResponse(val error: ErrorBody)

@Serializable
public data class UserDto(
    val id: String,
    val username: String,
    @SerialName("display_name") val displayName: String,
    val role: String,
    val email: String? = null,
    val timezone: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null
)

@Serializable
public data class Preferences(
    @SerialName("last_seen_visible") val lastSeenVisible: Boolean = false,
    @SerialName("read_receipts") val readReceipts: Boolean = false,
    @SerialName("allow_group_invites") val allowGroupInvites: Boolean = false,
    @SerialName("allow_private_messages") val allowPrivateMessages: Boolean = false,
    /** Accent swatch hex; "" (never sent by the server) means default. */
    @SerialName("primary_color") val primaryColor: String = ""
)

@Serializable
public data class ContactDto(
    val id: String,
    val user: UserDto,
    @SerialName("created_at") val createdAt: String
)

@Serializable
public data class RoomDto(
    val id: String,
    val type: String,
    val name: String? = null,
    val description: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("pinned_message_id") val pinnedMessageId: String? = null,
    val archived: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("my_role") val myRole: String? = null,
    @SerialName("unread_count") val unreadCount: Long = 0
)

@Serializable
public data class InviteDto(
    val id: String,
    val room: RoomDto,
    val inviter: UserDto,
    val invitee: UserDto,
    val status: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("accepted_at") val acceptedAt: String? = null
)

@Serializable
public data class AttachmentDto(
    val id: String,
    val filename: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    val url: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

@Serializable
public data class ReceiptsDto(
    @SerialName("delivered_at") val deliveredAt: String? = null,
    @SerialName("read_at") val readAt: String? = null
)

@Serializable
public data class ReactionDto(
    val emoji: String,
    val count: Long
)

@Serializable
public data class MessageDto(
    val id: String,
    @SerialName("room_id") val roomId: String,
    @SerialName("author_id") val authorId: String,
    val author: UserDto? = null,
    val body: String,
    val format: String = "markdown",
    @SerialName("reply_to_message_id") val replyToMessageId: String? = null,
    val attachments: List<AttachmentDto> = emptyList(),
    val reactions: List<ReactionDto> = emptyList(),
    val receipts: ReceiptsDto? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
public data class MessagePageDto(
    val items: List<MessageDto>,
    @SerialName("next_cursor") val nextCursor: String? = null
)

@Serializable
public data class AuthResponseDto(
    val token: String? = null,
    val user: UserDto
)

@Serializable
public data class NotificationModeDto(
    @SerialName("room_id") val roomId: String,
    val mode: String
)

@Serializable
public data class UploadSessionDto(
    val attachment: AttachmentDto,
    @SerialName("upload_url") val uploadUrl: String
)

// ---------- request bodies ----------

@Serializable
public data class LoginRequest(
    val username: String,
    val password: String,
    @SerialName("token_request") val tokenRequest: Boolean = true
)

@Serializable
public data class Login2FARequest(
    val challenge: String,
    val code: String,
    @SerialName("token_request") val tokenRequest: Boolean = true
)

@Serializable
public data class CreateRoomRequest(
    val type: String,
    val name: String? = null,
    val description: String? = null,
    val members: List<String> = emptyList()
)

@Serializable
public data class CreateMessageRequest(
    val body: String,
    val format: String = "markdown",
    @SerialName("reply_to_message_id") val replyToMessageId: String? = null,
    @SerialName("attachment_ids") val attachmentIds: List<String> = emptyList()
)

@Serializable
public data class CreateUploadRequest(
    val filename: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    val sha256: String? = null
)

@Serializable
public data class RegisterDeviceRequest(
    val platform: String,
    @SerialName("client_name") val clientName: String,
    @SerialName("client_version") val clientVersion: String,
    @SerialName("push_registration") val pushRegistration: PushRegistrationDto
)

@Serializable
public data class PushRegistrationDto(
    val endpoint: String,
    @SerialName("public_key") val publicKey: String,
    @SerialName("auth_secret") val authSecret: String
)

@Serializable
public data class RegisterDeviceResponseDto(
    @SerialName("device_id") val deviceId: String,
    @SerialName("validation_required") val validationRequired: Boolean
)

@Serializable
public data class SetNotificationModeRequest(
    @SerialName("room_id") val roomId: String? = null,
    val mode: String
)

@Serializable
public data class AddContactRequest(val userId: String)

@Serializable
public data class AddMemberRequest(val userId: String)

@Serializable
public data class CreateInviteRequest(
    val roomId: String,
    val userId: String,
    @SerialName("expires_at") val expiresAt: String? = null
)

@Serializable
public data class VapidResponse(@kotlinx.serialization.SerialName("public_key") val publicKey: String)

@Serializable
public data class VersionResponse(val version: String)

// ---------- WS ----------

@Serializable
public data class WsEnvelope(
    val type: String,
    val id: String? = null,
    val timestamp: String? = null,
    @SerialName("room_id") val roomId: String? = null,
    val data: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap()
)
// ---------- other request bodies (patch/pin/archive) ----------

@Serializable
public data class UpdateUserDto(
    @SerialName("display_name") val displayName: String? = null,
    val email: String? = null,
    val timezone: String? = null,
    @SerialName("avatar_attachment_id") val avatarAttachmentId: String? = null
)

@Serializable
public data class UpdatePreferencesDto(
    @SerialName("last_seen_visible") val lastSeenVisible: Boolean? = null,
    @SerialName("read_receipts") val readReceipts: Boolean? = null,
    @SerialName("allow_group_invites") val allowGroupInvites: Boolean? = null,
    @SerialName("allow_private_messages") val allowPrivateMessages: Boolean? = null,
    /** "" resets to the default; a swatch hex sets it; null omits. */
    @SerialName("primary_color") val primaryColor: String? = null
)

@Serializable
public data class UpdateMessageDto(
    val body: String,
    val format: String = "markdown"
)

@Serializable
public data class UpdateRoomDto(
    val name: String? = null,
    val description: String? = null,
    @SerialName("avatar_attachment_id") val avatarAttachmentId: String? = null
)

@Serializable
public data class PinRequestDto(@SerialName("message_id") val messageId: String)

@Serializable
public data class MemberArchiveRequestDto(val archived: Boolean)

@Serializable
public data class ReactRequestDto(val emoji: String)

@Serializable
public data class ValidateDeviceRequestDto(val token: String)
