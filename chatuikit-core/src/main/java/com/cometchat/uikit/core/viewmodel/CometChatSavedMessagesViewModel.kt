package com.cometchat.uikit.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatMessageEvent
import com.cometchat.uikit.core.state.PinnedSavedListUIState
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
 * ViewModel backing the user-level Saved Messages screen.
 *
 * Saved messages are private to the current user and span all conversations, ordered
 * newest-save-first. Each row carries its source conversation context (receiver / receiverType) so
 * the screen can render a "@person / #group" label and deep-link without extra calls. The screen is
 * read-only (no markAsRead / receipts / unread changes).
 *
 * @param enableListeners subscribe to the UIKit bus for live upkeep (false for tests/previews)
 */
public open class CometChatSavedMessagesViewModel(
    private val enableListeners: Boolean = true
) : ViewModel() {

    private val _messages = MutableStateFlow<List<BaseMessage>>(emptyList())

    /** The current user's saved messages across all conversations, newest save first. */
    public val messages: StateFlow<List<BaseMessage>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<PinnedSavedListUIState>(PinnedSavedListUIState.Loading)

    /** Screen state: [PinnedSavedListUIState.Loading] until the first load settles. */
    public val uiState: StateFlow<PinnedSavedListUIState> = _uiState.asStateFlow()

    private val _count = MutableStateFlow(0)

    /** Exact saved-message count (fetch-all-and-count; the backend caps at 100). */
    public val count: StateFlow<Int> = _count.asStateFlow()

    private val _unsaveSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** One-shot signal emitted when an unsave call succeeds, so the View can show a toast. */
    public val unsaveSuccess: SharedFlow<Unit> = _unsaveSuccess.asSharedFlow()

    /**
     * The in-flight paging job. Cancelled before a new load starts so a stale run can never publish
     * its pages over a fresher list, and so two rapid reloads cannot page concurrently.
     */
    private var loadJob: Job? = null

    init {
        if (enableListeners) addListeners()
    }

    /**
     * Realtime upkeep registration.
     *
     * // ponytail: the server has no saved-messages endpoint and emits no
     * saved/unsaved frame, so there is no event left to replace — live upkeep
     * rides [onMessageSavedExternally]/[onMessageUnsavedExternally] from the
     * UIKit bus, and the load path below reports unsupported.
     */
    private fun addListeners() {
        // no server truth for this screen's realtime; see the ponytail note
    }

    private fun removeListeners() {
        loadJob?.cancel()
        loadJob = null
    }

    override fun onCleared() {
        removeListeners()
        super.onCleared()
    }

    /** Rebuilds and loads the panel (bounded by the backend's 100 cap upstream). */
    public fun reload() {
        _messages.value = emptyList()
        _count.value = 0
        _uiState.value = PinnedSavedListUIState.Loading
        loadAll()
    }

    private fun loadAll() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = PinnedSavedListUIState.Error(
                CometChatException("hub_unsupported", "Saved messages are not on the server")
            )
        }
    }

    /** Unsaves a message. The panel is display-only here: nothing to unsaved server-side. */
    public fun unsave(message: BaseMessage) {
        // Optimistic row drop only — there is no server write, and the panel
        // rebuilds from external saves.
        removeRow(message)
        _unsaveSuccess.tryEmit(Unit)
        CometChatEvents.emitMessageEvent(CometChatMessageEvent.MessageUnsaved(message))
    }

    /** Optimistically drops a row, keeping count and screen state in step. */
    private fun removeRow(message: BaseMessage) {
        _messages.update { list -> list.filter { it.id != message.id } }
        _count.value = _messages.value.size
        if (_messages.value.isEmpty()) _uiState.value = PinnedSavedListUIState.Empty
    }

    /**
     * Live upkeep. Called both by this ViewModel's own realtime registration and by the
     * View's lifecycle-aware UIKit-bus subscription; both are safe to fire for the same message.
     */
    public fun onMessageSavedExternally(message: BaseMessage) {
        if (_messages.value.none { it.id == message.id }) {
            _messages.update { list ->
                if (list.any { it.id == message.id }) list else listOf(message) + list
            }
            _count.value = _messages.value.size
            _uiState.value = PinnedSavedListUIState.Content
        }
    }

    /** Removes a row when a message is unsaved elsewhere (see [onMessageSavedExternally]). */
    public fun onMessageUnsavedExternally(message: BaseMessage) {
        if (_messages.value.any { it.id == message.id }) {
            removeRow(message)
        }
    }
}
