package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.User
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.HubMappers

/**
 * GoChatHub-backed conversations datasource. The request carries the limit and
 * search keyword; server offers one page per call (no cursor — list is complete).
 */
internal class ConversationListDataSourceImpl : ConversationListDataSource {

    override suspend fun fetchConversations(request: ConversationsRequest): List<Conversation> {
        val rooms = Hub.client.rooms()
        // One members call per direct room before mapping — the peer name/avatar
        // is part of the row (webui parity; rooms list carries no members).
        for (room in rooms) {
            try { Hub.rememberMembers(room.id, Hub.client.roomMembers(room.id)) } catch (_: Exception) { }
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
