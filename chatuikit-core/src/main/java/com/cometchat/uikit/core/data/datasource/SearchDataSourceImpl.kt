package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.ConversationsRequest
import com.cometchat.chat.core.MessagesRequest
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.HubMappers

/**
 * Server-side search: room names via /rooms, message bodies via per-room
 * pages (the contract has no cross-room message search).
 */
internal class SearchDataSourceImpl : SearchDataSource {
    override suspend fun fetchConversations(request: ConversationsRequest): List<Conversation> {
        val keyword = request.searchKeyword
        if (keyword.isNullOrEmpty()) return emptyList()
        return Hub.client.rooms()
            .filter { it.type != "direct" && it.name?.contains(keyword, true) == true }
            .map { HubMappers.conversation(it) }
    }

    override suspend fun fetchMessages(request: MessagesRequest): List<BaseMessage> {
        val guid = request.guid.orEmpty()
        val roomId = if (guid.isNotEmpty()) guid else Hub.roomForPeer(request.uid.orEmpty())
        val keyword = request.searchKeyword
        val limit = if (request.limit in 1..100) request.limit else 50
        val page = Hub.client.messages(roomId, limit.coerceIn(1, 100), null)
        val (type, receiverUid) = Hub.receiverOf(Hub.roomById(roomId))
        return page.items
            .filter { keyword.isNullOrEmpty() || it.body.contains(keyword, ignoreCase = true) }
            .map { HubMappers.message(it, type, receiverUid) }
    }
}
