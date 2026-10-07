package com.cometchat.uikit.core.hub

import com.cometchat.chat.constants.CometChatConstants
import com.cometchat.chat.models.Attachment
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.GroupMember
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.ReactionCount
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import org.json.JSONObject

/**
 * Server DTO → SDK model builders (models are plain data holders; the network
 * layer is [HubClient]/[HubSocket]). Numeric ids come from [HubIds]; the real
 * uuid rides in metadata `hub_id` on every BaseMessage.
 */
public object HubMappers {

    public const val META_ID: String = "hub_id"
    public const val META_ROOM_ID: String = "hub_room_id"
    public const val META_REPLY_TO: String = "hub_reply_to"

    public fun user(dto: UserDto): User = User(dto.id, dto.displayName).apply {
        avatar = dto.avatarUrl
        role = dto.role
        status = Hub.presence(dto.id)
        dto.lastSeenAt?.let { lastActiveAt = isoToEpoch(it) }
        metadata = JSONObject().apply { dto.timezone?.let { put("timezone", it) } }
    }

    /** Clone of [user] with the new presence [state] ("online"/"offline") stamped in. */
    public fun userWithPresence(user: User, state: String): User = user.clone().apply { status = state }

    public fun group(dto: RoomDto): Group = Group(
        dto.id,
        dto.name,
        dto.avatarUrl,
        dto.description
    ).apply {
        owner = ""
        groupType =
            if (dto.type == "private") CometChatConstants.GROUP_TYPE_PRIVATE
            else CometChatConstants.GROUP_TYPE_PUBLIC
        createdAt = isoToEpoch(dto.createdAt)
        updatedAt = isoToEpoch(dto.updatedAt)
        setHasJoined(dto.myRole != null)
        metadata = JSONObject().apply { put(META_ID, dto.id) }
    }

    public fun groupMemberDto(dto: UserDto, room: RoomDto?): GroupMember = GroupMember(dto.id, dto.displayName).apply {
        avatar = dto.avatarUrl
        role = dto.role
        room?.myRole?.let { scope = it }
    }

    /** Direct room → one conversation; peer from the member cache. conversationId = server room id. */
    public fun conversation(dto: RoomDto): Conversation {
        val conversationWith: Any? = when (dto.type) {
            "direct" -> Hub.memberPeer(dto.id)?.let { user(it) } ?: User(dto.id, dto.name.orEmpty())
            else -> group(dto)
        }
        return Conversation(
            dto.id,
            if (dto.type == "direct") CometChatConstants.RECEIVER_TYPE_USER
            else CometChatConstants.RECEIVER_TYPE_GROUP
        ).apply {
            setConversationWith(conversationWith as com.cometchat.chat.models.AppEntity)
            unreadMessageCount = dto.unreadCount.toInt()
            updatedAt = isoToEpoch(dto.updatedAt)
        }
    }

    public fun receiverTypeOf(roomType: String): String =
        if (roomType == "direct") CometChatConstants.RECEIVER_TYPE_USER
        else CometChatConstants.RECEIVER_TYPE_GROUP

    /** Server message → SDK message. Caller supplies receiver context per the room. */
    public fun message(dto: MessageDto, receiverType: String, receiverUid: String): BaseMessage {
        val longId = HubIds.toLong(dto.id)
        val sender = dto.author?.let { user(it) } ?: User(dto.authorId, "").apply {
            status = Hub.presence(dto.authorId)
        }
        val me = Hub.meId()
        val base: BaseMessage = if (dto.attachments.isNotEmpty()) {
            MediaMessage().apply {
                setAttachments(dto.attachments.map { attachment(it) })
                caption = dto.body
                type = dto.attachments.firstOrNull()?.mimeType?.takeIf { it.startsWith("image/") }
                    ?: CometChatConstants.MESSAGE_TYPE_FILE
            }
        } else {
            TextMessage(receiverUid, receiverType, dto.body)
        }
        return base.apply {
            id = longId
            muid = dto.id
            setSender(sender)
            this.receiverUid = receiverUid
            this.receiverType = receiverType
            sentAt = isoToEpoch(dto.createdAt)
            dto.editedAt?.let { editedAt = isoToEpoch(it); editedBy = dto.authorId }
            dto.deletedAt?.let { deletedAt = isoToEpoch(it); deletedBy = dto.authorId }
            category = CometChatConstants.CATEGORY_MESSAGE
            conversationId = receiverUid
            // ADR-009: caller is author → receipts are the sender-visible aggregate;
            // caller is a recipient → receipts are owned. Server sends whichever applies.
            if (dto.authorId == me) {
                dto.receipts?.deliveredAt?.let { deliveredAt = isoToEpoch(it) }
                dto.receipts?.readAt?.let { readAt = isoToEpoch(it) }
            } else {
                dto.receipts?.deliveredAt?.let { deliveredToMeAt = isoToEpoch(it) }
                dto.receipts?.readAt?.let { readByMeAt = isoToEpoch(it) }
            }
            if (dto.reactions.isNotEmpty()) {
                reactions = dto.reactions.map { r ->
                    ReactionCount().apply { reaction = r.emoji; count = r.count.toInt() }
                }
            }
            metadata = (metadata ?: JSONObject()).apply {
                put(META_ID, dto.id)
                put(META_ROOM_ID, dto.roomId)
                dto.replyToMessageId?.let { put(META_REPLY_TO, it) }
            }
        }
    }

    public fun attachment(dto: AttachmentDto): Attachment = Attachment().apply {
        fileName = dto.filename
        fileMimeType = dto.mimeType
        fileSize = dto.sizeBytes.toInt()
        fileUrl = dto.url
        metadata = JSONObject().apply { put(META_ID, dto.id) }
    }

    /** Server → DTO for the reverse direction (SDK user → dto for requests). */
    public fun toUuid(message: BaseMessage): String? {
        val viaMetadata = message.metadata?.optString(META_ID)
        if (!viaMetadata.isNullOrEmpty()) return viaMetadata
        return HubIds.toStringId(message.id)
    }

    public fun isoToEpoch(rfc3339: String?): Long {
        if (rfc3339.isNullOrEmpty()) return 0L
        return try {
            java.time.Instant.parse(rfc3339).toEpochMilli() / 1000
        } catch (_: Exception) {
            0L
        }
    }

    public fun epochToIso(epochSeconds: Long): String? {
        if (epochSeconds <= 0L) return null
        return java.time.Instant.ofEpochSecond(epochSeconds).toString()
    }
}