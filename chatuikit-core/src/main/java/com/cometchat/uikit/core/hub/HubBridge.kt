package com.cometchat.uikit.core.hub

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The single place kit ViewModels bind to GoChatHub realtime: every old
 * `CometChat.addXxxListener` registration funnels through [events] (and
 * connection-state callbacks through [connection]), keeping the same VM
 * handler methods the SDK callbacks called.
 *
 * The cancelled [Job] mirrors the old `CometChat.removeXxxListener(tag)`.
 */
public object HubBridge {

    /**
     * Fans every [WsEnvelope] out to [handler] in [scope]. Presence is also
     * stamped into [Hub]'s cache for every envelope — the payloads that carry
     * `user_id`+`state` (presence.changed) update it, the rest pass through.
     */
    public fun events(scope: CoroutineScope, handler: (WsEnvelope) -> Unit): Job =
        scope.launch {
            Hub.socket.events.collect { env ->
                Hub.rememberPresenceFromEvent(env)
                handler(env)
            }
        }

    /** The old `ConnectionListener.onConnected()` — fires on every socket (re)connect. */
    public fun connection(scope: CoroutineScope, onConnected: () -> Unit): Job = scope.launch {
        Hub.socket.state.collect { if (it == HubSocketState.Connected) onConnected() }
    }
}