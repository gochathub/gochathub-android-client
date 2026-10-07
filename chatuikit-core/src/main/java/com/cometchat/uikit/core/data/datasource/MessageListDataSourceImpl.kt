package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.HubIds
import com.cometchat.uikit.core.data.datasource.HubImpls

import com.cometchat.uikit.core.hub.HubMappers

/**
 * GoChatHub-backed message list datasource. The server paginates backwards
 * from `before=<cursor>`; "next" re-reads the newest page (live updates ride
 * the WS connection, REST is the resync authority).
 */
internal class MessageListDataSourceImpl : MessageListDataSource {

    override suspend fun fetchPreviousMessages(request: MessagesRequest): List<BaseMessage> {
        val roomId = request.guid.ifEmpty { request.uid }
        val limit = if (request.limit in 1..100) request.limit else 50
        val anchor = if (request.messageId > 0) HubIds.toStringId(request.messageId) else null
        val page = Hub.client.messages(roomId, limit, anchor)
        syncIds(page)
        val (type, receiverUid) = Hub.receiverOf(Hub.roomById(roomId))
        return page.items.map { HubMappers.message(it, type, receiverUid) }
    }

    override suspend fun fetchNextMessages(request: MessagesRequest): List<BaseMessage> {
        // No server-side `after` cursor: refetch the newest page and append.
        val roomId = request.guid.ifEmpty { request.uid }
        val limit = if (request.limit in 1..100) request.limit else 50
        val page = Hub.client.messages(roomId, limit, null)
        syncIds(page)
        val (type, receiverUid) = Hub.receiverOf(Hub.roomById(roomId))
        return page.items.map { HubMappers.message(it, type, receiverUid) }
    }

    override suspend fun getConversation(id: String, type: String): Conversation {
        val rooms = Hub.client.rooms()
        Hub.hydrateDirectMembers(rooms)
        val conversation = when (type) {
            "user" -> rooms.firstOrNull { it.type == "direct" && Hub.memberPeer(it.id)?.id == id }
            else -> rooms.firstOrNull { it.id == id && it.type != "direct" }
        }
        return if (conversation != null) {
            HubMappers.conversation(conversation)
        } else {
            // No room yet (first DM) — synthesize an empty conversation.
            com.cometchat.uikit.core.hub.HubMappers.conversation(
                com.cometchat.uikit.core.hub.RoomDto(
                    id = if (type == "user") "" else id,
                    type = if (type == "user") "direct" else "private",
                    name = null
                )
            )
        }
    }

    private fun syncIds(page: com.cometchat.uikit.core.hub.MessagePageDto) {
        // keep the session uuid↔Long map warm for by-id lookups
        for (item in page.items) HubIds.toLong(item.id)
    }

    override suspend fun getMessage(messageId: Long): BaseMessage {
        val uuid = HubIds.toStringId(messageId)
            ?: return HubImpls.unsupported("get message ${'$'}messageId (unknown id)")
        return HubImpls.refetchMessage(uuid)
    }

    override suspend fun deleteMessage(messageId: Long): BaseMessage? {
        val uuid = HubIds.toStringId(messageId)
            ?: return HubImpls.unsupported("delete message ${'$'}messageId (unknown id)")
        val tombstone = Hub.client.deleteMessage(uuid)
        return HubImpls.refetchMessage(tombstone.id)
    }

    override suspend fun flagMessage(messageId: Long, reason: String, remark: String) {
        HubImpls.unsupported("message reporting")
    }

    override suspend fun addReaction(messageId: Long, emoji: String): BaseMessage {
        val uuid = HubImpls.uuidOf(messageFor(messageId))
        Hub.client.react(uuid, emoji)
        return HubImpls.refetchMessage(uuid)
    }

    override suspend fun removeReaction(messageId: Long, emoji: String): BaseMessage {
        val uuid = HubImpls.uuidOf(messageFor(messageId))
        Hub.client.unreact(uuid, emoji)
        return HubImpls.refetchMessage(uuid)
    }

    override suspend fun markAsDelivered(message: BaseMessage) {
        HubImpls.delivered(message)
    }

    override suspend fun markAsRead(message: BaseMessage) {
        val roomId = HubImpls.roomIdOf(message)
        if (roomId.isEmpty()) return HubImpls.unsupported("mark read (no room id)")
        Hub.client.markRead(roomId, HubImpls.uuidOf(message))
        Hub.socket.read(roomId, HubImpls.uuidOf(message))
    }

    override suspend fun markAsUnread(message: BaseMessage): Conversation {
        HubImpls.unsupported("mark-as-unread")
    }

    private fun messageFor(messageId: Long): BaseMessage =
        com.cometchat.chat.models.TextMessage("", "", "").apply {
            id = messageId
            metadata = org.json.JSONObject().put(HubMappers.META_ID, HubIds.toStringId(messageId).orEmpty())
        }
}
