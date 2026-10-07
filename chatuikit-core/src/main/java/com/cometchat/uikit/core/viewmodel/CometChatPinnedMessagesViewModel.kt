package com.cometchat.uikit.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatMessageEvent
import com.cometchat.uikit.core.state.PinnedSavedListUIState
import com.cometchat.uikit.core.utils.PinSaveUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel backing the per-conversation Pinned Messages panel.
 *
 * Pinned messages are conversation-wide (visible to everyone) and ordered newest-pin-first. The
 * count shown in the panel title is a fetch-all-and-count (there is no server count field; the cap
 * is 100). The panel is read-only — it never marks messages read or changes unread counts.
 *
 * @param enableListeners subscribe to the UIKit bus for live upkeep (false for tests/previews)
 */
public open class CometChatPinnedMessagesViewModel(
    private val enableListeners: Boolean = true
) : ViewModel() {

    private val _messages = MutableStateFlow<List<BaseMessage>>(emptyList())

    /** The conversation's pinned messages, newest pin first. */
    public val messages: StateFlow<List<BaseMessage>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<PinnedSavedListUIState>(PinnedSavedListUIState.Loading)

    /** Screen state: [PinnedSavedListUIState.Loading] until the first load settles. */
    public val uiState: StateFlow<PinnedSavedListUIState> = _uiState.asStateFlow()

    private val _count = MutableStateFlow(0)

    /** Exact pinned-message count (fetch-all-and-count; the backend caps at 100). */
    public val count: StateFlow<Int> = _count.asStateFlow()

    private val _actionResult = MutableSharedFlow<PinnedActionResult>(extraBufferCapacity = 1)

    /** One-shot results of message-option actions, for the View to surface as toasts. */
    public val actionResult: SharedFlow<PinnedActionResult> = _actionResult.asSharedFlow()

    private var uid: String? = null
    private var guid: String? = null

    /**
     * The in-flight load job; refreshed on every [reload] so a conversation switch can
     * never let a previous conversation's result land under the new header.
     */
    private var loadJob: Job? = null

    /** The GoChatHub realtime registration (the old SDK message listener). */
    private var hubEventsJob: Job? = null

    init {
        if (enableListeners) addListeners()
    }

    /**
     * Configures the panel for a conversation. Pass the peer uid (1-1) OR the group guid.
     * Triggers an initial load.
     */
    public fun configure(uid: String?, guid: String?) {
        this.uid = uid
        this.guid = guid
        reload()
    }

    /** Loads the conversation's pinned message (the server pins one per room). */
    public fun reload() {
        _messages.value = emptyList()
        _count.value = 0
        _uiState.value = PinnedSavedListUIState.Loading
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val roomId = resolveRoomId() ?: run {
                    _uiState.value = PinnedSavedListUIState.Empty
                    return@launch
                }
                val room = com.cometchat.uikit.core.hub.Hub.roomById(roomId)
                    ?: com.cometchat.uikit.core.hub.Hub.client.room(roomId)
                        .also { com.cometchat.uikit.core.hub.Hub.rememberRoom(it) }
                val pinnedUuid = room.pinnedMessageId
                if (pinnedUuid.isNullOrEmpty()) {
                    _uiState.value = PinnedSavedListUIState.Empty
                    return@launch
                }
                val (type, receiverUid) = com.cometchat.uikit.core.hub.Hub.receiverOf(room)
                val message = com.cometchat.uikit.core.hub.HubMappers.message(
                    com.cometchat.uikit.core.hub.Hub.client.message(pinnedUuid), type, receiverUid
                )
                _messages.value = listOf(message)
                _count.value = 1
                _uiState.value = PinnedSavedListUIState.Content
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: CometChatException) {
                _uiState.value = PinnedSavedListUIState.Error(e)
            } catch (e: Exception) {
                _uiState.value = PinnedSavedListUIState.Error(
                    e as? CometChatException ?: CometChatException(
                        "hub_unsupported", e.message ?: "Failed to load pinned messages"
                    )
                )
            }
        }
    }

    /** Room of the configured conversation: the group guid, or the direct room for the peer uid. */
    private suspend fun resolveRoomId(): String? {
        val hub = com.cometchat.uikit.core.hub.Hub
        guid?.takeIf { it.isNotEmpty() }?.let { guid ->
            return guid
        }
        val peerUid = uid ?: return null
        // Direct room for the peer: member caches are warm whenever a chat has been
        // opened (the screen this panel opens from); a cold cache costs one rooms
        // refresh plus members for the user's direct rooms.
        // // ponytail: rooms listing carries no member ids, so matching needs the
        // per-room members call — upgrade to a server-side peer→room lookup if the
        // API ever grows one.
        directRoomIdForPeer(peerUid)?.let { return it }
        try {
            hub.client.rooms().forEach { hub.rememberRoom(it) }
            val directRooms = hub.roomsCache.values.filter { it.type == "direct" }
            for (room in directRooms) {
                if (hub.memberPeer(room.id) != null) continue
                runCatching {
                    hub.rememberMembers(room.id, hub.client.roomMembers(room.id))
                }
            }
        } catch (_: Exception) {
            // fall through to the (unchanged) cache
        }
        return directRoomIdForPeer(peerUid)
    }

    /** Cached direct room id whose member peer is [peerUid] (null when unknown). */
    private fun directRoomIdForPeer(peerUid: String): String? {
        val hub = com.cometchat.uikit.core.hub.Hub
        return hub.roomsCache.values
            .firstOrNull { it.type == "direct" && hub.memberPeer(it.id)?.id == peerUid }
            ?.id
    }

    /**
     * Unpins a message. Optimistically removes it from the panel, then reverts on error. On success
     * the event is broadcast on the UIKit bus so the main list bubble updates too.
     */
    public fun unpin(message: BaseMessage) {
        val index = _messages.value.indexOfFirst { it.id == message.id }
        val uuid = com.cometchat.uikit.core.hub.HubMappers.toUuid(message) ?: return
        removeRow(message)

        viewModelScope.launch {
            try {
                val roomId = resolveRoomId() ?: throw CometChatException("hub_unsupported", "No room for this conversation")
                com.cometchat.uikit.core.hub.Hub.client.unpinMessage(roomId)
                CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageUnpinned(message))
            } catch (e: CometChatException) {
                // Revert. The full-screen Error state is reserved for LOAD failures — a failed
                // action on a healthy list just restores the row and surfaces a toast.
                restoreRow(message, index)
                _actionResult.tryEmit(pinFailureResult(e))
            }
        }
    }

    /**
     * Pins a message. On success broadcasts on the UIKit bus so the main list bubble updates.
     * (On the pinned panel every row is already pinned, so this is rarely reachable — wired for
     * completeness / robustness.)
     */
    public fun pin(message: BaseMessage) {
        val uuid = com.cometchat.uikit.core.hub.HubMappers.toUuid(message) ?: return

        viewModelScope.launch {
            try {
                val roomId = resolveRoomId() ?: throw CometChatException("hub_unsupported", "No room for this conversation")
                com.cometchat.uikit.core.hub.Hub.client.pinMessage(roomId, uuid)
                CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessagePinned(message))
                _actionResult.tryEmit(PinnedActionResult.PINNED)
            } catch (e: CometChatException) {
                _actionResult.tryEmit(pinFailureResult(e))
            }
        }
    }

    /**
     * Deletes a message. Optimistically removes it from the panel, reverts on error. Mirrors the
     * [unpin] pattern.
     */
    public fun delete(message: BaseMessage) {
        val index = _messages.value.indexOfFirst { it.id == message.id }
        val uuid = com.cometchat.uikit.core.hub.HubMappers.toUuid(message) ?: return
        removeRow(message)

        viewModelScope.launch {
            try {
                val roomId = resolveRoomId() ?: throw CometChatException("hub_unsupported", "No room for this conversation")
                val tombstone = com.cometchat.uikit.core.hub.Hub.client.deleteMessage(uuid)
                val (type, receiverUid) = com.cometchat.uikit.core.hub.Hub.receiverOf(
                    com.cometchat.uikit.core.hub.Hub.roomById(roomId)
                )
                // Broadcast so an open message list (and conversations preview) swaps the bubble
                // to its deleted tombstone immediately — same event the list's own delete emits.
                CometChatEvents.emitMessageEvent(
                    CometChatMessageEvent.MessageDeleted(
                        com.cometchat.uikit.core.hub.HubMappers.message(tombstone, type, receiverUid)
                    )
                )
                _actionResult.tryEmit(PinnedActionResult.DELETED)
            } catch (e: CometChatException) {
                restoreRow(message, index)
                _actionResult.tryEmit(PinnedActionResult.DELETE_FAILED)
            }
        }
    }

    /**
     * Maps a pin/unpin failure to its toast: an RBAC denial gets the shared "you don't have
     * permission" message (pin/unpin is offered to every member, the server enforces the policy),
     * everything else the generic failure.
     */
    private fun pinFailureResult(e: CometChatException): PinnedActionResult =
        when (PinSaveUtils.classifyFailure(e)) {
            PinSaveUtils.Failure.PermissionDenied -> PinnedActionResult.PERMISSION_DENIED
            is PinSaveUtils.Failure.LimitReached -> PinnedActionResult.PIN_LIMIT_REACHED
            is PinSaveUtils.Failure.Other -> PinnedActionResult.PIN_FAILED
        }

    /**
     * Translates a message via the message-translation extension.
     *
     * // ponytail: the server has no extension API — the action reports failed
     * with the same toast the kit shows on a translation error.
     */
    public fun translate(message: BaseMessage) {
        if (message !is com.cometchat.chat.models.TextMessage) return
        _actionResult.tryEmit(PinnedActionResult.TRANSLATE_FAILED)
    }

    /** Optimistically drops a row, keeping count and screen state in step. */
    private fun removeRow(message: BaseMessage) {
        _messages.update { list -> list.filter { it.id != message.id } }
        _count.value = _messages.value.size
        if (_messages.value.isEmpty()) _uiState.value = PinnedSavedListUIState.Empty
    }

    /**
     * Undoes [removeRow] by re-inserting the single removed row at [index].
     *
     * Deliberately not a whole-snapshot restore: a pin/unpin delivered by the SDK listener between
     * the optimistic removal and this failure callback must survive the revert.
     */
    private fun restoreRow(message: BaseMessage, index: Int) {
        if (index >= 0) {
            _messages.update { list ->
                if (list.any { it.id == message.id }) list
                else list.toMutableList().apply { add(index.coerceAtMost(size), message) }
            }
        }
        _count.value = _messages.value.size
        _uiState.value = if (_messages.value.isNotEmpty()) PinnedSavedListUIState.Content
        else PinnedSavedListUIState.Empty
    }

    /**
     * Realtime upkeep via GoChatHub: the server's single-pin model rides
     * `room.pinned_changed` for this room. (Saved/unpin echoes for other rows
     * arrive from the UIKit bus; there is no server event for user-level saves.)
     */
    private fun addListeners() {
        hubEventsJob = com.cometchat.uikit.core.hub.HubBridge.events(viewModelScope) { env ->
            if (env.type != "room.pinned_changed") return@events
            val roomId = env.roomId ?: return@events
            viewModelScope.launch {
                val pinnedMessageId = (env.data["pinned_message_id"] as? kotlinx.serialization.json.JsonPrimitive)
                    ?.content
                if (pinnedMessageId.isNullOrEmpty() || pinnedMessageId == "null") {
                    onMessageUnpinnedExternally(null)
                    return@launch
                }
                try {
                    com.cometchat.uikit.core.hub.Hub.roomById(roomId) ?: return@launch
                    val (type, receiverUid) = com.cometchat.uikit.core.hub.Hub.receiverOf(
                        com.cometchat.uikit.core.hub.Hub.roomById(roomId)
                    )
                    val pinned = com.cometchat.uikit.core.hub.HubMappers.message(
                        com.cometchat.uikit.core.hub.Hub.client.message(pinnedMessageId),
                        type,
                        receiverUid
                    )
                    onMessagePinnedExternally(pinned)
                } catch (_: Exception) {
                    // The panel's next load re-reads the room pin.
                }
            }
        }
    }

    private fun removeListeners() {
        hubEventsJob?.cancel()
        hubEventsJob = null
    }

    override fun onCleared() {
        removeListeners()
        super.onCleared()
    }

    /**
     * Live upkeep. Called both by this ViewModel's own realtime registration and by the
     * View's lifecycle-aware UIKit-bus subscription; both are safe to fire for the same message.
     */
    public fun onMessagePinnedExternally(message: BaseMessage) {
        if (!belongsToThisConversation(message)) return
        if (_messages.value.none { it.id == message.id }) {
            _messages.update { list ->
                if (list.any { it.id == message.id }) list else listOf(message) + list
            }
            _count.value = _messages.value.size
            _uiState.value = PinnedSavedListUIState.Content
        }
    }

    /**
     * Removes a row when the message is unpinned elsewhere (UIKit bus) — or, for a null
     * [message], when the room's single pin is cleared (`room.pinned_changed` with no
     * message): the server keeps one pinned message per room, so every row goes.
     */
    public fun onMessageUnpinnedExternally(message: BaseMessage?) {
        if (message != null) {
            if (_messages.value.any { it.id == message.id }) {
                removeRow(message)
            }
        } else if (_messages.value.isNotEmpty()) {
            _messages.value = emptyList()
            _count.value = 0
            _uiState.value = PinnedSavedListUIState.Empty
        }
    }

    /** Outcome of a message-option action, surfaced to the View for a toast. */
    public enum class PinnedActionResult {
        PINNED, PIN_FAILED,

        /** The pinned-messages cap was hit; the View reads the cap via [PinSaveUtils.pinnedMessagesLimit]. */
        PIN_LIMIT_REACHED,

        /** Any RBAC/SBAC denial, whichever action was attempted. */
        PERMISSION_DENIED,

        DELETED, DELETE_FAILED,
        TRANSLATE_FAILED
    }

    private fun belongsToThisConversation(message: BaseMessage): Boolean {
        val g = guid
        val u = uid
        return when {
            g != null -> message.receiverType == "group" && message.receiverUid == g
            u != null && message.receiverType == "user" -> {
                // A 1-1 message belongs here only if it runs between the peer and the logged-in
                // user; matching on the peer alone would also accept a message the peer sent to
                // somebody else. Falls back to the looser check only if the session has no user
                // (tests/previews), where dropping every event would be worse.
                val me = com.cometchat.uikit.core.CometChatUIKit.getLoggedInUser()?.uid
                if (me == null) {
                    message.receiverUid == u || message.sender?.uid == u
                } else {
                    (message.receiverUid == u && message.sender?.uid == me) ||
                        (message.sender?.uid == u && message.receiverUid == me)
                }
            }
            else -> false
        }
    }
}
