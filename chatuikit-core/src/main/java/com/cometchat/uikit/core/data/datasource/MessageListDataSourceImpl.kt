package com.cometchat.uikit.core.data.datasource

import com.gochathub.chat.core.MessagesRequest
import com.gochathub.chat.models.BaseMessage
import com.gochathub.chat.models.Conversation
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

    /**
     * The closed SDK's MessagesRequest tracked its own cursor; ours is inert, so
     * paging state lives here, keyed by request identity (the repository builds
     * a fresh request on reset → fresh state → newest page again).
     */
    private class PageState(var cursor: String? = null, var done: Boolean = false)

    private val pages = java.util.WeakHashMap<MessagesRequest, PageState>()

    override suspend fun fetchPreviousMessages(request: MessagesRequest): List<BaseMessage> {
        val state = synchronized(pages) { pages.getOrPut(request) { PageState() } }
        // Server cursor ends at next_cursor == null: report an empty page so the
        // kit stops asking (it treats empty as "no more").
        if (state.done) return emptyList()
        val roomId = receiverToRoom(request)
        val limit = if (request.limit in 1..100) request.limit else 50
        val page = Hub.client.messages(roomId, limit, state.cursor)
        syncIds(page)
        state.cursor = page.nextCursor
        state.done = page.nextCursor.isNullOrEmpty()
        val (type, receiverUid) = Hub.receiverOf(Hub.roomById(roomId))
        // Server pages newest-first; the kit expects each page oldest-first.
        return page.items.reversed().map { HubMappers.message(it, type, receiverUid) }
    }

    override suspend fun fetchNextMessages(request: MessagesRequest): List<BaseMessage> {
        // No server-side `after` cursor: take the newest page, keep only what's
        // newer than the anchor message (live arrivals ride the socket anyway).
        val roomId = receiverToRoom(request)
        val limit = if (request.limit in 1..100) request.limit else 50
        val page = Hub.client.messages(roomId, limit, null)
        syncIds(page)
        val (type, receiverUid) = Hub.receiverOf(Hub.roomById(roomId))
        return page.items.reversed()
            .filter { HubIds.toLong(it.id) > request.messageId }
            .map { HubMappers.message(it, type, receiverUid) }
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

    /** Groups/rooms carry the room id in guid; user chats carry the peer id in uid. */
    private suspend fun receiverToRoom(request: MessagesRequest): String {
        val guid = request.guid.orEmpty()
        val uid = request.uid.orEmpty()
        return if (guid.isNotEmpty()) guid else Hub.roomForPeer(uid)
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
        com.gochathub.chat.models.TextMessage("", "", "").apply {
            id = messageId
            metadata = org.json.JSONObject().put(HubMappers.META_ID, HubIds.toStringId(messageId).orEmpty())
        }
}
