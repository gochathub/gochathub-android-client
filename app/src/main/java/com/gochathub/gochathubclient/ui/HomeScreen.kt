package com.gochathub.gochathubclient.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.dp
import com.cometchat.chat.models.Conversation
import com.cometchat.uikit.compose.presentation.conversations.ui.CometChatConversations
import com.gochathub.gochathubclient.Auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun HomeScreen(
    onOpenChat: (Conversation) -> Unit,
    onNewChat: () -> Unit,
    onSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GoChatHub") },
                actions = {
                    IconButton(onClick = onNewChat) {
                        Icon(Icons.Filled.Add, contentDescription = "New chat")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                    TextButton(onClick = { scope.launch { Auth.logout() } }) {
                        Text("Sign out")
                    }
                }
            )
        }
    ) { padding ->
        CometChatConversations(
            modifier = Modifier.padding(padding),
            title = "Chats",
            onItemClick = onOpenChat
        )
    }
}