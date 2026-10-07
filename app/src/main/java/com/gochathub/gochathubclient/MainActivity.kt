package com.gochathub.gochathubclient

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.gochathub.gochathubclient.ui.ChatScreen
import com.gochathub.gochathubclient.ui.GoChatHubTheme
import com.gochathub.gochathubclient.ui.HomeScreen
import com.gochathub.gochathubclient.ui.InvitesScreen
import com.gochathub.gochathubclient.ui.LoginScreen
import com.gochathub.gochathubclient.ui.NewChatScreen
import com.gochathub.gochathubclient.ui.SettingsScreen
import com.cometchat.uikit.core.hub.Hub

import kotlinx.coroutines.launch

public class MainActivity : ComponentActivity() {

    public enum class Route { LOGIN, HOME, NEW_CHAT, SETTINGS, INVITES }

    private var loggedIn by mutableStateOf(false)
    private var route by mutableStateOf(Route.HOME)
    private var openRoomId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Hub.client.onUnauthorized = { runOnUiThread { loggedIn = false } }
        readRoomIntent()
        restoreSession()
        setContent {
            GoChatHubTheme {
                when {
                    !loggedIn -> LoginScreen(onLoggedIn = { loggedIn = true; route = Route.HOME })
                    openRoomId != null -> ChatScreen(
                        roomId = openRoomId!!,
                        onBackPress = { openRoomId = null }
                    )
                    route == Route.HOME -> HomeScreen(
                        onOpenChat = { conversation -> openRoomId = conversation.conversationId },
                        onNewChat = { route = Route.NEW_CHAT },
                        onSettings = { route = Route.SETTINGS }
                    )
                    route == Route.NEW_CHAT -> NewChatScreen(
                        onOpenChat = { user ->
                            lifecycleScope.launch {
                                try {
                                    openRoomId = Hub.roomForPeer(user.uid)
                                } catch (_: Exception) { } // toast on failure later
                            }
                        },
                        onBackPress = { route = Route.HOME }
                    )
                    route == Route.SETTINGS -> SettingsScreen(onBackPress = { route = Route.HOME })
                    route == Route.INVITES -> InvitesScreen(onBackPress = { route = Route.HOME })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readRoomIntent(intent)
    }

    private fun readRoomIntent(intent: Intent? = this.intent) {
        intent?.getStringExtra("room_id")?.let {
            if (it.isNotEmpty()) openRoomId = it
        }
    }

    private fun restoreSession() {
        lifecycleScope.launch {
            loggedIn = Auth.restore()
        }
    }
}