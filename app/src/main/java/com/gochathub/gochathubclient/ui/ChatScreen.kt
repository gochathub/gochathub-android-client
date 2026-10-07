package com.gochathub.gochathubclient.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cometchat.chat.models.Group
import com.cometchat.chat.models.User
import com.cometchat.uikit.compose.presentation.messagecomposer.ui.CometChatMessageComposer
import com.cometchat.uikit.compose.presentation.messageheader.ui.CometChatMessageHeader
import com.cometchat.uikit.compose.presentation.messagelist.style.CometChatMessageListStyle
import com.cometchat.uikit.compose.presentation.messagelist.ui.CometChatMessageList
import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.HubMappers

/**
 * One chat over kit components: header, list, composer. `roomId` is the
 * server room; direct rooms resolve to their peer User, others to a Group.
 */
@Composable
public fun ChatScreen(
    roomId: String,
    onBackPress: () -> Unit
) {
    var user by remember { mutableStateOf<User?>(null) }
    var group by remember { mutableStateOf<Group?>(null) }

    LaunchedEffect(roomId) {
        try {
            val room = Hub.roomById(roomId) ?: Hub.client.room(roomId).also { Hub.rememberRoom(it) }
            if (room.type == "direct") {
                val peer = Hub.client.roomMembers(roomId).firstOrNull { it.id != Hub.meId() }
                user = peer?.let { HubMappers.user(it) }
                group = null
            } else {
                user = null
                group = HubMappers.group(room)
            }
            Hub.socket.subscribe(roomId)
        } catch (_: Exception) {
            user = null
            group = null
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        val u = user
        val g = group
        if (u != null) {
            CometChatMessageHeader(user = u, onBackPress = onBackPress)
            CometChatMessageList(
                user = u,
                style = CometChatMessageListStyle.default(),
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            CometChatMessageComposer(user = u)
        } else if (g != null) {
            CometChatMessageHeader(group = g, onBackPress = onBackPress)
            CometChatMessageList(
                group = g,
                style = CometChatMessageListStyle.default(),
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            CometChatMessageComposer(group = g)
        } else {
            Text("Room unavailable", modifier = Modifier.padding(16.dp))
        }
    }
}