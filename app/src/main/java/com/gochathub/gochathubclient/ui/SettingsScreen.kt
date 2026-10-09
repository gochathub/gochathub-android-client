package com.gochathub.gochathubclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.Preferences
import com.cometchat.uikit.core.hub.SetNotificationModeRequest
import com.cometchat.uikit.core.hub.UpdatePreferencesDto
import kotlinx.coroutines.launch

/** Privacy preferences + notification modes (server-enforced; ADR-013). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun SettingsScreen(onBackPress: () -> Unit) {
    var prefs by remember { mutableStateOf<Preferences?>(null) }
    var mode by remember { mutableStateOf("mentions") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try { prefs = Hub.client.preferences() } catch (_: Exception) { }
        try {
            // "" room_id is the user default row; none stored yet means the server default.
            Hub.client.notificationModes().firstOrNull { it.roomId == "" }?.let { mode = it.mode }
        } catch (_: Exception) { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = hubTopBarColors(),
                title = { Text("Settings", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionLabel("Privacy")
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
                Text("Preferences unavailable", style = MaterialTheme.typography.bodyMedium)
            }

            SectionLabel("Accent color")
            Text(
                "Follows your account to every device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Accent.swatches.chunked(8).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                    row.forEach { hex ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (Accent.hex == hex) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = CircleShape
                                )
                                .padding(3.dp)
                                .background(Accent.color(hex), CircleShape)
                                .clickable {
                                    if (Accent.hex != hex) scope.launch { Accent.set(hex) }
                                }
                        )
                    }
                }
            }
            if (Accent.hex != Accent.DEFAULT) {
                TextButton(onClick = { scope.launch { Accent.set("") } }) {
                    Text("Reset to default")
                }
            }

            SectionLabel("Notifications")
            Text(
                "Which messages send a push notification",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
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
                        label = { Text(option, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        }
    }
}

private suspend fun savePrefs(update: UpdatePreferencesDto, set: (Preferences) -> Unit) {
    try {
        set(Hub.client.updatePreferences(update))
    } catch (_: Exception) { /* server value wins on next load */ }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
