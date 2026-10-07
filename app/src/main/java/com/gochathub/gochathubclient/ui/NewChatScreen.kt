package com.gochathub.gochathubclient.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gochathub.chat.models.User
import com.cometchat.uikit.compose.presentation.users.ui.CometChatUsers

/**
 * People screen: contacts, or server-wide search from the search box.
 * Picking a person opens their DM (room created on first send).
 */
@Composable
public fun NewChatScreen(
    onOpenChat: (User) -> Unit,
    onBackPress: () -> Unit
) {
    CometChatUsers(
        modifier = Modifier.fillMaxSize(),
        title = "New chat",
        hideBackIcon = false,
        onBackPress = onBackPress,
        onItemClick = onOpenChat
    )
}