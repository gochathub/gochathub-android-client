package com.gochathub.gochathubclient

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.cometchat.uikit.core.hub.Hub
import com.gochathub.gochathubclient.ui.ChatScreen
import com.gochathub.gochathubclient.ui.GoChatHubTheme
import com.gochathub.gochathubclient.ui.HomeScreen
import com.gochathub.gochathubclient.ui.InvitesScreen
import com.gochathub.gochathubclient.ui.LoginScreen
import com.gochathub.gochathubclient.ui.NewChatScreen
import com.gochathub.gochathubclient.ui.SettingsScreen
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
                // Android 13+: notifications need a runtime grant; ask once after sign-in.
                val askNotifications = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* denial just means no push banners; the app keeps working */ }
                LaunchedEffect(loggedIn) {
                    if (loggedIn && Build.VERSION.SDK_INT >= 33 &&
                        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                // targetSdk 36 enforces edge-to-edge; draw the app inside the safe area
                // in-app Back: chat -> list, sub-screens -> list (root exits as usual)
                BackHandler(enabled = loggedIn && (openRoomId != null || route != Route.HOME)) {
                    if (openRoomId != null) openRoomId = null else route = Route.HOME
                }
                Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
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