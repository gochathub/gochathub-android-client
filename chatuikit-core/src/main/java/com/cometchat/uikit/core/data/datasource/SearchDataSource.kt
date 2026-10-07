package com.cometchat.uikit.core.data.datasource

import com.gochathub.chat.core.ConversationsRequest
import com.gochathub.chat.core.MessagesRequest
import com.gochathub.chat.models.BaseMessage
import com.gochathub.chat.models.Conversation

/**
 * Interface defining data source operations for search functionality.
 * Lives in data layer - defines contract for data fetching.
 * Allows for different implementations (remote, local, mock).
 */
public interface SearchDataSource {

    /**
     * Fetches conversations from the data source based on search criteria.
     * @param request The configured ConversationsRequest
     * @return Raw list of Conversation objects
     * @throws Exception if fetching fails
     */
    suspend public fun fetchConversations(request: ConversationsRequest): List<Conversation>

    /**
     * Fetches messages from the data source based on search criteria.
     * @param request The configured MessagesRequest
     * @return Raw list of BaseMessage objects
     * @throws Exception if fetching fails
     */
    suspend public fun fetchMessages(request: MessagesRequest): List<BaseMessage>
}
