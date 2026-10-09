package com.gochathub.gochathubclient.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.dp
import com.gochathub.chat.models.Conversation
import com.cometchat.uikit.compose.presentation.conversations.ui.CometChatConversations
import com.gochathub.gochathubclient.Auth
import com.gochathub.gochathubclient.share.SharePayload
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun HomeScreen(
    onOpenChat: (Conversation) -> Unit,
    onNewChat: () -> Unit,
    onSettings: () -> Unit,
    onSignedOut: () -> Unit,
    sharedContent: SharePayload? = null,
    onShareCancel: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = hubTopBarColors(),
                title = { Text("goChatHub") },
                actions = {
                    IconButton(onClick = onNewChat) {
                        Icon(Icons.Filled.Add, contentDescription = "New chat")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                    TextButton(
                        onClick = { scope.launch { Auth.logout(); onSignedOut() } },
                        colors = ButtonDefaults.textButtonColors(contentColor = GoChatHubColors.white)
                    ) {
                        Text("Sign out")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            sharedContent?.let { payload ->
                ShareTargetBanner(
                    payload = payload,
                    description = payload.describe() +
                        (if (payload.failed.isNotEmpty()) " · ${payload.failed.size} unreadable" else ""),
                    onCancel = onShareCancel
                )
            }
            CometChatConversations(
                modifier = Modifier.fillMaxSize(),
                title = "Chats",
                style = hubConversationsStyle(),
                hideSeparator = true,
                onItemClick = onOpenChat
            )
        }
    }
}

/** Parked-share notice on the pick list: what arrived, X to cancel. */
@Composable
private fun ShareTargetBanner(
    payload: SharePayload,
    description: String,
    onCancel: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Send shared content to… tap a room", style = MaterialTheme.typography.titleSmall)
                if (payload.ready) {
                    Text(description, style = MaterialTheme.typography.bodySmall)
                } else {
                    LinearProgressIndicator(modifier = Modifier.padding(top = 4.dp))
                }
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel share")
            }
        }
    }
}