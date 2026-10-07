package com.gochathub.gochathubclient.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cometchat.uikit.core.hub.Hub
import kotlinx.coroutines.launch
import com.cometchat.uikit.core.hub.Preferences
import com.cometchat.uikit.core.hub.SetNotificationModeRequest
import com.cometchat.uikit.core.hub.UpdatePreferencesDto

/** Privacy preferences + notification modes (server-enforced; ADR-013). */
@Composable
public fun SettingsScreen(onBackPress: () -> Unit) {
    var prefs by remember { mutableStateOf<Preferences?>(null) }
    var mode by remember { mutableStateOf("mentions") }
    var modesLoaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            prefs = Hub.client.preferences()
        } catch (_: Exception) { }
        try {
            Hub.client.notificationModes()
                .firstOrNull { it.roomId == "" }
                ?.let { mode = it.mode }
            modesLoaded = true
        } catch (_: Exception) { }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Settings", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)

        val p = prefs
        if (p != null) {
            ToggleRow("Show my last seen", p.lastSeenVisible) { on ->
                scope.launch { savePrefs(UpdatePreferencesDto(lastSeenVisible = on)) { prefs = it } }
            }
            ToggleRow("Read receipts", p.readReceipts) { on ->
                scope.launch { savePrefs(UpdatePreferencesDto(readReceipts = on)) { prefs = it } }
            }
            ToggleRow("Allow group invites", p.allowGroupInvites) { on ->
                scope.launch { savePrefs(UpdatePreferencesDto(allowGroupInvites = on)) { prefs = it } }
            }
            ToggleRow("Allow private messages", p.allowPrivateMessages) { on ->
                scope.launch { savePrefs(UpdatePreferencesDto(allowPrivateMessages = on)) { prefs = it } }
            }
        } else {
            Text("Preferences unavailable")
        }

        if (modesLoaded) {
            Text("Notifications", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all", "mentions", "directs", "never").forEach { option ->
                    FilterChip(
                        selected = mode == option,
                        onClick = {
                            val previous = mode
                            mode = option
                            scope.launch {
                                try { Hub.client.setNotificationMode(SetNotificationModeRequest(mode = option)) }
                                catch (_: Exception) { mode = previous }
                            }
                        },
                        label = { Text(option) }
                    )
                }
            }
        }
    }
}

private suspend fun savePrefs(update: UpdatePreferencesDto, set: (Preferences) -> Unit) {
    try {
        set(Hub.client.updatePreferences(update))
    } catch (_: Exception) { /* optimistic toggle; server rejects via resync */ }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}