package com.gochathub.gochathubclient.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.gochathub.chat.models.Group
import com.gochathub.chat.models.User
import com.gochathub.gochathubclient.share.SharePayload
import com.cometchat.uikit.compose.presentation.messagecomposer.ui.CometChatMessageComposer
import com.cometchat.uikit.compose.presentation.messageheader.ui.CometChatMessageHeader
import com.cometchat.uikit.compose.presentation.messagelist.ui.CometChatMessageList
import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.HubMappers

/**
 * One chat over kit components: header, list, composer. `roomId` is the
 * server room; direct rooms resolve to their peer User, others to a Group.
 * `sharedContent` (share-target prefill) applies to the composer once per
 * room entry; drafts never survive re-entry (fresh store per room), so the
 * full-set prefill cannot clobber a live draft.
 */
@Composable
public fun ChatScreen(
    roomId: String,
    sharedContent: SharePayload? = null,
    onBackPress: () -> Unit
) {
    var user by remember { mutableStateOf<User?>(null) }
    var group by remember { mutableStateOf<Group?>(null) }

    // The Kit creates its list/header/composer ViewModels without a per-conversation key;
    // in the activity's store the second room would reuse the first room's list.
    val owner = remember(roomId) {
        object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
    }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }

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

    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            val u = user
            val g = group
            if (u != null) {
                CometChatMessageHeader(user = u, onBackPress = onBackPress, style = hubHeaderStyle())
                CometChatMessageList(
                    user = u,
                    style = hubMessageListStyle(),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                CometChatMessageComposer(
                    user = u,
                    initialText = sharedContent?.text,
                    initialAttachments = sharedContent?.attachments
                )
            } else if (g != null) {
                CometChatMessageHeader(group = g, onBackPress = onBackPress, style = hubHeaderStyle())
                CometChatMessageList(
                    group = g,
                    style = hubMessageListStyle(),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
                CometChatMessageComposer(
                    group = g,
                    initialText = sharedContent?.text,
                    initialAttachments = sharedContent?.attachments
                )
            } else {
                Text("Room unavailable", modifier = Modifier.padding(16.dp))
            }
        }
    }
}