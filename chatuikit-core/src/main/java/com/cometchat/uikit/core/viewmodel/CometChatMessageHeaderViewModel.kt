package com.cometchat.uikit.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gochathub.chat.constants.CometChatConstants
import com.gochathub.chat.exceptions.CometChatException
import com.gochathub.chat.models.Group
import com.gochathub.chat.models.TypingIndicator
import com.gochathub.chat.models.User
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.constants.UIKitConstants
import com.cometchat.uikit.core.domain.usecase.GetGroupUseCase
import com.cometchat.uikit.core.domain.usecase.GetUserUseCase
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatGroupEvent
import com.cometchat.uikit.core.events.CometChatUserEvent
import com.cometchat.uikit.core.state.MessageHeaderUIState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the CometChatMessageHeader component.
 * Manages UI state for displaying user or group conversation headers.
 * 
 * This ViewModel is shared by both chatuikit-jetpack (Compose) and chatuikit-kotlin (Views)
 * implementations, ensuring consistent behavior across both UI frameworks.
 * 
 * Features:
 * - User/Group data management with real-time updates
 * - Typing indicator handling with debounce
 * - Online/offline status tracking for users
 * - Member count tracking for groups
 * - SDK and UIKit local event listeners for real-time updates
 * 
 * @param getUserUseCase Use case for fetching user data
 * @param getGroupUseCase Use case for fetching group data
 * @param enableListeners Whether to enable CometChat listeners (set to false for testing)
 */
open public class CometChatMessageHeaderViewModel(
    private val getUserUseCase: GetUserUseCase,
    private val getGroupUseCase: GetGroupUseCase,
    private val enableListeners: Boolean = true
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<MessageHeaderUIState>(MessageHeaderUIState.Loading)
    public val uiState: StateFlow<MessageHeaderUIState> = _uiState.asStateFlow()

    // User state
    private val _user = MutableStateFlow<User?>(null)
    public val user: StateFlow<User?> = _user.asStateFlow()

    // Group state
    private val _group = MutableStateFlow<Group?>(null)
    public val group: StateFlow<Group?> = _group.asStateFlow()

    // Typing indicator state
    private val _typingIndicator = MutableStateFlow<TypingIndicator?>(null)
    public val typingIndicator: StateFlow<TypingIndicator?> = _typingIndicator.asStateFlow()

    // Member count for groups
    private val _memberCount = MutableStateFlow(0)
    public val memberCount: StateFlow<Int> = _memberCount.asStateFlow()

    // Error events - emitted for UI to handle via callback
    private val _errorEvent = MutableSharedFlow<CometChatException>()
    public val errorEvent: SharedFlow<CometChatException> = _errorEvent.asSharedFlow()

    // GoChatHub realtime + connection jobs (the old SDK listener registrations)
    private var hubEventsJob: Job? = null
    private var connectionJob: Job? = null

    // Typing debounce job
    private var typingDebounceJob: Job? = null
    
    // Typing indicator debounce delay (1 second, matching Java implementation)
    private val TYPING_INDICATOR_DEBOUNCER = 1000L

    // Current entity ID for listener filtering
    private var currentId: String? = null
    
    // Local event listener jobs
    private var userEventsJob: Job? = null
    private var groupEventsJob: Job? = null

    init {
        if (enableListeners) {
            addListeners()
        }
    }

    /**
     * Sets the user for the message header.
     * Clears any existing group data and updates the UI state.
     * 
     * @param user The User object to display in the header
     */
    public fun setUser(user: User) {
        _user.value = user
        _group.value = null
        currentId = user.uid
        _uiState.value = MessageHeaderUIState.UserContent(user)
    }

    /**
     * Sets the group for the message header.
     * Clears any existing user data and updates the UI state.
     * 
     * @param group The Group object to display in the header
     */
    public fun setGroup(group: Group) {
        _group.value = group
        _user.value = null
        currentId = group.guid
        _memberCount.value = group.membersCount
        _uiState.value = MessageHeaderUIState.GroupContent(group)
    }

    /**
     * Refreshes user data from the server.
     * Used for reconnection scenarios or manual refresh.
     * 
     * @param uid The user ID to refresh
     */
    public fun refreshUser(uid: String) {
        viewModelScope.launch {
            getUserUseCase(uid)
                .onSuccess { user ->
                    setUser(user)
                }
                .onFailure { e ->
                    // Cancellation (e.g. navigating away mid-fetch) is not an error
                    if (e is CancellationException) throw e
                    _errorEvent.emit(
                        e as? CometChatException
                            ?: CometChatException("UNKNOWN_ERROR", e.message ?: "Unknown error occurred")
                    )
                }
        }
    }

    /**
     * Refreshes group data from the server.
     * Used for reconnection scenarios or manual refresh.
     * 
     * @param guid The group ID to refresh
     */
    public fun refreshGroup(guid: String) {
        viewModelScope.launch {
            getGroupUseCase(guid)
                .onSuccess { group ->
                    setGroup(group)
                }
                .onFailure { e ->
                    // Cancellation (e.g. navigating away mid-fetch) is not an error
                    if (e is CancellationException) throw e
                    _errorEvent.emit(
                        e as? CometChatException
                            ?: CometChatException("UNKNOWN_ERROR", e.message ?: "Unknown error occurred")
                    )
                }
        }
    }

    /**
     * Adds GoChatHub realtime listeners and UIKit local event listeners.
     * Called during initialization if enableListeners is true.
     */
    private fun addListeners() {
        // Realtime envelope handlers — the old SDK User/Group/Message/Connection
        // listeners' callbacks, driven by server frames instead of the closed SDK.
        hubEventsJob = com.cometchat.uikit.core.hub.HubBridge.events(viewModelScope, ::handleHubEvent)
        connectionJob = com.cometchat.uikit.core.hub.HubBridge.connection(viewModelScope) {
            refreshMessageHeader()
        }
        addUIKitLocalEventListeners()
    }

    /**
     * Server envelope → the same handlers the old SDK listeners drove.
     * Presence stamps also fire globally inside [com.cometchat.uikit.core.hub.HubBridge.events].
     */
    private fun handleHubEvent(env: com.cometchat.uikit.core.hub.WsEnvelope) {
        val hubEvents = com.cometchat.uikit.core.hub.HubEvents
        val hubMappers = com.cometchat.uikit.core.hub.HubMappers
        when (env.type) {
            // The old UserListener's onUserOnline/onUserOffline.
            "presence.changed" -> {
                val userId = hubEvents.userIdOf(env) ?: return
                val user = _user.value ?: return
                if (user.uid != userId || isBlocked(user)) return
                val updated = hubMappers.userWithPresence(
                    user, hubEvents.presenceStateOf(env) ?: UIKitConstants.UserStatus.OFFLINE
                )
                _user.value = updated
                _uiState.value = MessageHeaderUIState.UserContent(updated)
            }
            // The old GroupListener's member events — the server only carries the
            // actor's id, so the count is re-read per event (one lightweight REST call).
            "room.member_added", "room.member_removed" -> {
                val roomId = env.roomId ?: return
                if (roomId != currentId) return
                viewModelScope.launch {
                    try {
                        val members = com.cometchat.uikit.core.hub.Hub.client.roomMembers(roomId)
                        com.cometchat.uikit.core.hub.Hub.rememberMembers(roomId, members)
                        _memberCount.value = members.size
                        com.cometchat.uikit.core.hub.Hub.roomById(roomId)?.let { room ->
                            val refreshed = hubMappers.group(room).apply { membersCount = members.size }
                            _group.value = refreshed
                            _uiState.value = MessageHeaderUIState.GroupContent(refreshed)
                        }
                    } catch (_: Exception) {
                        // List refresh on the next screen stays the fallback.
                    }
                }
            }
            // The old MessageListener's onTypingStarted/onTypingEnded.
            "typing.started", "typing.stopped" -> {
                if (isBlocked(_user.value)) return
                hubEvents.typingIndicatorOf(env)?.let {
                    handleTypingIndicator(it, env.type == "typing.started")
                }
            }
        }
    }

    /**
     * Adds UIKit local event listeners for UI-triggered events.
     * These events are emitted by other UI components (e.g., when a user is blocked).
     * Uses Flow-based event collection from CometChatEvents.
     */
    private fun addUIKitLocalEventListeners() {
        // User block/unblock events
        userEventsJob = viewModelScope.launch {
            CometChatEvents.userEvents.collect { event ->
                when (event) {
                    is CometChatUserEvent.UserBlocked -> {
                        if (event.user.uid == currentId) {
                            _user.value = event.user
                            _uiState.value = MessageHeaderUIState.UserContent(event.user)
                        }
                    }
                    is CometChatUserEvent.UserUnblocked -> {
                        if (event.user.uid == currentId) {
                            _user.value = event.user
                            _uiState.value = MessageHeaderUIState.UserContent(event.user)
                        }
                    }
                }
            }
        }

        // Group events from other UI components
        groupEventsJob = viewModelScope.launch {
            CometChatEvents.groupEvents.collect { event ->
                when (event) {
                    is CometChatGroupEvent.MembersAdded -> {
                        if (event.group.guid == currentId) {
                            _group.value = event.group
                            _memberCount.value = event.group.membersCount
                            _uiState.value = MessageHeaderUIState.GroupContent(event.group)
                        }
                    }
                    is CometChatGroupEvent.MemberKicked -> {
                        if (event.group.guid == currentId) {
                            _group.value = event.group
                            _memberCount.value = event.group.membersCount
                            _uiState.value = MessageHeaderUIState.GroupContent(event.group)
                        }
                    }
                    is CometChatGroupEvent.MemberBanned -> {
                        if (event.group.guid == currentId) {
                            _group.value = event.group
                            _memberCount.value = event.group.membersCount
                            _uiState.value = MessageHeaderUIState.GroupContent(event.group)
                        }
                    }
                    is CometChatGroupEvent.OwnershipChanged -> {
                        if (event.group.guid == currentId) {
                            _group.value = event.group
                            _uiState.value = MessageHeaderUIState.GroupContent(event.group)
                        }
                    }
                    is CometChatGroupEvent.MemberJoined -> {
                        if (event.group.guid == currentId) {
                            _group.value = event.group
                            _memberCount.value = event.group.membersCount
                            _uiState.value = MessageHeaderUIState.GroupContent(event.group)
                        }
                    }
                    is CometChatGroupEvent.GroupLeft -> {
                        if (event.group.guid == currentId) {
                            _group.value = event.group
                            _memberCount.value = event.group.membersCount
                            _uiState.value = MessageHeaderUIState.GroupContent(event.group)
                        }
                    }
                    is CometChatGroupEvent.MemberUnbanned -> {
                        if (event.group.guid == currentId) {
                            _group.value = event.group
                            _memberCount.value = event.group.membersCount
                            _uiState.value = MessageHeaderUIState.GroupContent(event.group)
                        }
                    }
                    is CometChatGroupEvent.MemberScopeChanged -> {
                        if (event.group.guid == currentId) {
                            _group.value = event.group
                            _uiState.value = MessageHeaderUIState.GroupContent(event.group)
                        }
                    }
                    else -> {
                        // Other group events don't affect message header
                    }
                }
            }
        }
    }

    /**
     * Handles typing indicator events with debounce for typing end.
     * 
     * @param typingIndicator The typing indicator from the SDK
     * @param isTyping True if typing started, false if typing ended
     */
    private fun handleTypingIndicator(typingIndicator: TypingIndicator, isTyping: Boolean) {
        val matchesCurrentConversation = when {
            typingIndicator.receiverType == CometChatConstants.RECEIVER_TYPE_USER ->
                typingIndicator.sender.uid == currentId
            else -> typingIndicator.receiverId == currentId
        }

        if (matchesCurrentConversation) {
            if (isTyping) {
                // Cancel any pending debounce job
                typingDebounceJob?.cancel()
                _typingIndicator.value = typingIndicator
            } else {
                // Debounce typing end to prevent flickering
                typingDebounceJob?.cancel()
                typingDebounceJob = viewModelScope.launch {
                    delay(TYPING_INDICATOR_DEBOUNCER)
                    _typingIndicator.value = null
                }
            }
        }
    }

    /**
     * Refreshes the current message header data.
     * Called on reconnection to ensure data is up-to-date.
     */
    private fun refreshMessageHeader() {
        _user.value?.let { refreshUser(it.uid) }
        _group.value?.let { refreshGroup(it.guid) }
    }

    /**
     * Checks if a user is blocked (either blocked by me or has blocked me).
     * 
     * @param user The user to check, or null
     * @return True if the user is blocked in either direction, false otherwise
     */
    private fun isBlocked(user: User?): Boolean {
        return user?.let { it.isBlockedByMe || it.isHasBlockedMe } ?: false
    }

    /**
     * Removes the GoChatHub realtime jobs and cancels UIKit local event listener jobs.
     * Called when the ViewModel is cleared.
     */
    public fun removeListeners() {
        hubEventsJob?.cancel()
        connectionJob?.cancel()

        // Cancel local event listener jobs
        userEventsJob?.cancel()
        groupEventsJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        typingDebounceJob?.cancel()
        removeListeners()
    }
}
