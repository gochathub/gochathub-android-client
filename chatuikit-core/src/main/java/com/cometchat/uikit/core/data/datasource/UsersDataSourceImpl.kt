package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.UsersRequest
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.HubMappers

/**
 * GoChatHub users datasource. The server has no user directory: search when a
 * keyword is given, otherwise the caller's contacts.
 */
internal class UsersDataSourceImpl : UsersDataSource {
    override suspend fun fetchUsers(request: UsersRequest): List<User> {
        val keyword = request.searchKeyword
        val dtos = if (!keyword.isNullOrEmpty()) {
            Hub.client.searchUsers(keyword)
        } else {
            Hub.client.contacts().map { it.user }
        }
        return dtos.map { HubMappers.user(it) }
    }
}
