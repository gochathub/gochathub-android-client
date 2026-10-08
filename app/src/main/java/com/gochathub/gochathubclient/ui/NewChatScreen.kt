package com.gochathub.gochathubclient.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gochathub.chat.models.User
import com.cometchat.uikit.compose.presentation.users.ui.CometChatUsers

/**
 * People screen: contacts, or server-wide search from the search box.
 * Picking a person opens their DM (room created on first send).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun NewChatScreen(
    onOpenChat: (User) -> Unit,
    onBackPress: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            colors = hubTopBarColors(),
            title = { Text("New chat", style = MaterialTheme.typography.titleMedium) },
            navigationIcon = {
                IconButton(onClick = onBackPress) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        CometChatUsers(
            modifier = Modifier.weight(1f),
            hideToolbar = true,
            onItemClick = onOpenChat
        )
    }
}
