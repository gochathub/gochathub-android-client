package com.cometchat.uikit.core.data.datasource

import com.gochathub.chat.models.BaseMessage
import com.gochathub.chat.exceptions.CometChatException
import com.gochathub.chat.models.User
import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.HubIds
import com.cometchat.uikit.core.hub.HubMappers
import com.cometchat.uikit.core.hub.MessageDto

/** Shared GoChatHub plumbing for the datasource impls. */
internal object HubImpls {

    /** Server uuid for a kit message (metadata first, id cache fallback). */
    fun uuidOf(message: BaseMessage): String =
        HubMappers.toUuid(message) ?: throw CometChatException(
            "hub_unknown_id",
            "Message ${message.id} has no server id in this session"
        )

    /** Room of a message via metadata; falls back to the receiver-side room cache. */
    fun roomIdOf(message: BaseMessage): String =
        message.metadata?.optString(HubMappers.META_ROOM_ID).orEmpty()

    /** Resolves (or creates) the server room for a receiver per kit semantics. */
    suspend fun roomForReceiver(receiverUid: String, receiverType: String): String =
        if (receiverType != "user") receiverUid else Hub.roomForPeer(receiverUid)

    /** Delivered receipt via the socket ack (server records it per ADR-009). */
    suspend fun delivered(message: BaseMessage) {
        Hub.socket.ack(listOf(uuidOf(message)))
    }

    /** Re-fetches a message and maps it with the room's receiver context. */
    suspend fun refetchMessage(uuid: String): BaseMessage {
        val dto = Hub.client.message(uuid)
        val room = Hub.roomById(dto.roomId)
        val (type, receiverUid) = Hub.receiverOf(room)
        return HubMappers.message(dto, type, receiverUid)
    }

    fun unsupported(what: String): Nothing =
        throw CometChatException("hub_unsupported", "$what is not supported by the server")

    /** Search keyword from the kit's request model. */
    fun User.nameOrNull(): String? = name
}
