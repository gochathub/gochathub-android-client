package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.core.GroupsRequest
import com.cometchat.chat.models.Group
import com.cometchat.uikit.core.data.datasource.HubImpls.unsupported
import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.HubMappers

/**
 * GoChatHub rooms datasource: room list covers group rooms (already all
 * "joined"); membership opens only through invites server-side.
 */
internal class GroupsDataSourceImpl : GroupsDataSource {
    override suspend fun fetchGroups(request: GroupsRequest): List<Group> {
        val keyword = request.searchKeyword
        val rooms = Hub.client.rooms().filter { it.type != "direct" }
        return rooms
            .filter { true }
            .filter { keyword.isNullOrEmpty() || it.name?.lowercase()?.contains(keyword.lowercase()) == true }
            .map { HubMappers.group(it) }
    }

    override suspend fun joinGroup(groupId: String, groupType: String, password: String?): Group {
        // Server has no open-join: invitations land in GET /invites (accept there).
        unsupported("joining a room without an invite; use the invites screen")
    }
}
