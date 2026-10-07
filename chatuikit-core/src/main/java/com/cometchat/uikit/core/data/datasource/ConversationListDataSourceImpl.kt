package com.cometchat.uikit.core.data.datasource

import com.gochathub.chat.core.ConversationsRequest
import com.gochathub.chat.models.BaseMessage
import com.gochathub.chat.models.User
import com.gochathub.chat.models.Conversation
import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.HubMappers

/**
 * GoChatHub-backed conversations datasource. The request carries the limit and
 * search keyword; server offers one page per call (no cursor — list is complete).
 */
internal class ConversationListDataSourceImpl : ConversationListDataSource {

    override suspend fun fetchConversations(request: ConversationsRequest): List<Conversation> {
        val rooms = Hub.client.rooms()
        // Peer name/avatar and the preview are part of the row, but the rooms list
        // carries neither. Fetch only what the caches lack (60 RPM server limit);
        // later refreshes cost one request; live frames keep the rows current.
        for (room in rooms) {
            Hub.rememberRoom(room)
            try {
                if (room.type == "direct" && Hub.members(room.id) == null) {
                    Hub.rememberMembers(room.id, Hub.client.roomMembers(room.id))
                }
                if (Hub.lastMessageCache[room.id] == null) {
                    Hub.client.messages(room.id, limit = 1).items.firstOrNull()
                        ?.let { Hub.lastMessageCache[room.id] = it }
                }
            } catch (_: Exception) { /* row renders without the extras */ }
        }
        val keyword = request.searchKeyword?.lowercase()
        return rooms.map { HubMappers.conversation(it) }
            .filter { conversation ->
                if (keyword.isNullOrEmpty()) true
                else (conversation.conversationWith as? User)?.name?.lowercase()?.contains(keyword) == true
            }
    }

    override suspend fun deleteConversation(conversationWith: String, conversationType: String): String {
        // Server keeps message history; member-level archive is the client parity surface.
        Hub.client.setMemberArchive(conversationWith, true)
        return "archived"
    }

    override suspend fun markAsDelivered(message: BaseMessage) {
        HubImpls.delivered(message)
    }
}
