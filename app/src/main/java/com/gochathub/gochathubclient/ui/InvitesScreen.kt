package com.gochathub.gochathubclient.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.cometchat.uikit.core.hub.InviteDto
import kotlinx.coroutines.launch

/** Open invites: accept joins the room, decline removes it. */
private suspend fun reload(set: (List<InviteDto>) -> Unit) {
    set(try { Hub.client.invites() } catch (_: Exception) { emptyList() })
}

@Composable
public fun InvitesScreen(onBackPress: () -> Unit) {
    var invites by remember { mutableStateOf<List<InviteDto>?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { reload { invites = it } }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Invites", style = MaterialTheme.typography.titleLarge)
        val list = invites
        when {
            list == null -> Text("Loading…")
            list.isEmpty() -> Text("No open invites")
            else -> for (invite in list.filter { it.status == "pending" }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(invite.room.name ?: "Direct chat", style = MaterialTheme.typography.titleSmall)
                        Text(invite.inviter.displayName, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = {
                        scope.launch {
                            try { Hub.client.acceptInvite(invite.id) } catch (_: Exception) { }
                            reload { invites = it }
                        }
                    }) { Text("Join") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            try { Hub.client.declineInvite(invite.id) } catch (_: Exception) { }
                            reload { invites = it }
                        }
                    }) { Text("Decline") }
                }
            }
        }
    }
}