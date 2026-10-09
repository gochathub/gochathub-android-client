package com.gochathub.gochathubclient

import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.cometchat.uikit.core.hub.Hub
import com.gochathub.gochathubclient.share.SharePayload
import com.gochathub.gochathubclient.share.ShareTarget
import com.gochathub.gochathubclient.ui.ChatScreen
import com.gochathub.gochathubclient.ui.GoChatHubTheme
import com.gochathub.gochathubclient.ui.HomeScreen
import com.gochathub.gochathubclient.ui.InvitesScreen
import com.gochathub.gochathubclient.ui.LoginScreen
import com.gochathub.gochathubclient.ui.NewChatScreen
import com.gochathub.gochathubclient.ui.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

public class MainActivity : ComponentActivity() {

    public enum class Route { LOGIN, HOME, NEW_CHAT, SETTINGS, INVITES }

    private var loggedIn by mutableStateOf(false)
    private var route by mutableStateOf(Route.HOME)
    private var openRoomId by mutableStateOf<String?>(null)
    private var sharePayload by mutableStateOf<SharePayload?>(null)
    private var shareApplied by mutableStateOf(false)
    private var confirmDiscardShare by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Hub.client.onUnauthorized = { runOnUiThread { loggedIn = false } }
        readRoomIntent()
        readShareIntent()
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
                BackHandler(enabled = loggedIn && !confirmDiscardShare &&
                    (openRoomId != null || route != Route.HOME)
                ) { popFromRoom() }
                if (confirmDiscardShare) {
                    AlertDialog(
                        onDismissRequest = { confirmDiscardShare = false },
                        title = { Text("Discard shared content?") },
                        text = { Text("Keep it parked so the next room you open gets it, or drop it.") },
                        confirmButton = {
                            TextButton(onClick = {
                                confirmDiscardShare = false
                                sharePayload = null
                                shareApplied = false
                                openRoomId = null
                            }) { Text("Discard") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                confirmDiscardShare = false
                                shareApplied = false
                                openRoomId = null
                            }) { Text("Keep") }
                        }
                    )
                }
                Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                    when {
                        !loggedIn -> LoginScreen(onLoggedIn = { loggedIn = true; route = Route.HOME })
                        openRoomId != null -> ChatScreen(
                            roomId = openRoomId!!,
                            sharedContent = sharePayload,
                            onBackPress = { popFromRoom() }
                        )
                        route == Route.HOME -> HomeScreen(
                            sharedContent = sharePayload,
                            onShareCancel = {
                                sharePayload = null
                                shareApplied = false
                            },
                            onOpenChat = { conversation ->
                                if (sharePayload != null) shareApplied = true
                                openRoomId = conversation.conversationId
                            },
                            onNewChat = { route = Route.NEW_CHAT },
                            onSettings = { route = Route.SETTINGS },
                            onSignedOut = { loggedIn = false }
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
        readShareIntent(intent)
    }

    private fun readRoomIntent(intent: Intent? = this.intent) {
        intent?.getStringExtra("room_id")?.let {
            if (it.isNotEmpty()) openRoomId = it
        }
    }

    /**
     * Share target (ACTION_SEND/ACTION_SEND_MULTIPLE): parse, then copy stream
     * URIs to app cache off-thread (the read grant is temporary). A newer share
     * replaces any parked one and brings the pick list up.
     */
    private fun readShareIntent(intent: Intent? = this.intent) {
        val parts = intent?.let { ShareTarget.extract(it) } ?: return
        shareApplied = false
        openRoomId = null
        route = Route.HOME
        // Banner up while copying; empty text means nothing to show yet, so hold
        // at null until the copy completes.
        if (parts.text != null) {
            sharePayload = SharePayload(parts.text, emptyList(), ready = false, failed = emptyList())
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val (inputs, failed) = ShareTarget.stageAll(applicationContext, parts.streams)
            withContext(Dispatchers.Main) {
                val ready = SharePayload(parts.text, inputs, ready = true, failed = failed)
                if (ready.isEmpty) {
                    sharePayload = null
                    Toast.makeText(this@MainActivity, "Could not read shared content", Toast.LENGTH_SHORT).show()
                } else {
                    sharePayload = ready
                }
            }
        }
    }

    /** Chat back: confirm keep/discard while a share is staged in it. */
    private fun popFromRoom() {
        if (openRoomId == null) {
            route = Route.HOME
            return
        }
        if (shareApplied && sharePayload != null) {
            confirmDiscardShare = true
        } else {
            openRoomId = null
        }
    }

    private fun restoreSession() {
        lifecycleScope.launch {
            loggedIn = Auth.restore()
        }
    }
}