package com.cometchat.uikit.core.data.datasource

import com.cometchat.chat.models.GroupMember
import com.cometchat.uikit.core.data.datasource.HubImpls.unsupported
import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.HubMappers

/** GoChatHub room members datasource. */
internal class GroupMembersDataSourceImpl(
    @Suppress("UNUSED_PARAMETER") private val requestBuilder: com.cometchat.chat.core.GroupMembersRequest.GroupMembersRequestBuilder? = null
) : GroupMembersDataSource {
    private var hasMore = true

    override suspend fun fetchGroupMembers(guid: String, limit: Int, searchKeyword: String?): List<GroupMember> {
        Hub.client.roomMembers(guid).let {
            Hub.rememberMembers(guid, it)
            val room = Hub.roomById(guid)
            hasMore = false // server returns the full member list
            return it
                .filter { u -> searchKeyword.isNullOrEmpty() || u.displayName.contains(searchKeyword, true) || u.username.contains(searchKeyword, true) }
                .map { u -> HubMappers.groupMemberDto(u, room) }
        }
    }

    override suspend fun kickGroupMember(guid: String, uid: String): String {
        Hub.client.removeRoomMember(guid, uid)
        return "removed"
    }

    override suspend fun banGroupMember(guid: String, uid: String): String {
        unsupported("banning members; the server only has member removal")
    }

    override suspend fun changeMemberScope(guid: String, uid: String, scope: String): String {
        unsupported("member scope changes")
    }

    override fun hasMoreMembers(): Boolean = hasMore

    override fun resetRequest() {
        hasMore = true
    }
}
