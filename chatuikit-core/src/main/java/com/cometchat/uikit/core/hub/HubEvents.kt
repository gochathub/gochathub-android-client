package com.cometchat.uikit.core.hub

import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.MessageReceipt
import com.cometchat.chat.models.TypingIndicator
import com.cometchat.chat.models.User
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * WS envelope → kit-shaped models. Envelope shapes per the server
 * (server internal/service): message payloads ride `data.message`; receipts,
 * reactions and typing carry ids and user ids.
 */
public object HubEvents {

    /** Decodes a server message payload into a kit BaseMessage (null otherwise). */
    public fun messageOf(envelope: WsEnvelope): BaseMessage? {
        val dto = messageDtoOf(envelope) ?: return null
        HubIds.toLong(dto.id)
        val (type, receiverUid) = Hub.receiverOf(Hub.roomById(dto.roomId))
        return HubMappers.message(dto, type, receiverUid)
    }

    public fun messageDtoOf(envelope: WsEnvelope): MessageDto? {
        val raw = (envelope.data["message"] as? JsonElement)?.toString() ?: return null
        return try {
            JSON.decodeFromString(MessageDto.serializer(), raw)
        } catch (_: Exception) {
            null
        }
    }

    public fun messageIdOf(envelope: WsEnvelope): String? =
        (envelope.data["message_id"] as? JsonPrimitive)?.content

    public fun userIdOf(envelope: WsEnvelope): String? =
        (envelope.data["user_id"] as? JsonPrimitive)?.content

    public fun emojiOf(envelope: WsEnvelope): String? =
        (envelope.data["emoji"] as? JsonPrimitive)?.content

    public fun presenceStateOf(envelope: WsEnvelope): String? =
        (envelope.data["state"] as? JsonPrimitive)?.content

    /**
     * Aggregate receipts update for a message the caller authored (ADR-009).
     * The envelope names the message; the truth comes from REST refetch
     * (delivered/read stamps per room-read state), so no receipt without a
     * session — the caller retries on resync.
     */
    public suspend fun receiptsChangedOf(envelope: WsEnvelope): MessageReceipt? {
        val uuid = messageIdOf(envelope) ?: return null
        val dto = try { Hub.client.message(uuid) } catch (_: Exception) { return null }
        val me = Hub.me ?: return null
        val longMessageId = HubIds.toLong(uuid)
        return MessageReceipt().apply {
            this.messageId = longMessageId
            sender = me.let { HubMappers.user(it) }
            receiverId = dto.roomId
            receiverType = "user"
            receiptType = when {
                dto.receipts?.readAt != null -> MessageReceipt.RECEIPT_TYPE_READ_BY_ALL
                dto.receipts?.deliveredAt != null -> MessageReceipt.RECEIPT_TYPE_DELIVERED_TO_ALL
                else -> MessageReceipt.RECEIPT_TYPE_DELIVERED
            }
            dto.receipts?.deliveredAt?.let { deliveredAt = HubMappers.isoToEpoch(it) }
            dto.receipts?.readAt?.let { readAt = HubMappers.isoToEpoch(it) }
        }
    }

    public fun typingIndicatorOf(envelope: WsEnvelope): TypingIndicator? {
        val roomId = envelope.roomId ?: return null
        val userId = userIdOf(envelope) ?: return null
        val room = Hub.roomById(roomId)
        val (receiverType, receiverId) = Hub.receiverOf(room)
        return TypingIndicator(receiverId, receiverType).apply {
            sender = User(userId, userId)
            typingStatus =
                if (envelope.type == "typing.started") "STARTED" else "STOPPED"
        }
    }
}