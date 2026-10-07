package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.HubMappers

/** GoChatHub-backed user/group lookups for the message header. */
internal class MessageHeaderDataSourceImpl : MessageHeaderDataSource {
    override suspend fun getUser(uid: String): User {
        val cached = Hub.allCachedUsers().firstOrNull { it.id == uid }
        return HubMappers.user(cached ?: Hub.client.user(uid))
    }

    override suspend fun getGroup(guid: String): Group {
        val room = Hub.roomById(guid) ?: Hub.client.room(guid).also { Hub.rememberRoom(it) }
        return HubMappers.group(room)
    }
}
