package com.cometchat.uikit.core.viewmodel

import android.content.Context
import androidx.annotation.RawRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gochathub.chat.constants.CometChatConstants
import com.gochathub.chat.core.ConversationsRequest
import com.gochathub.chat.exceptions.CometChatException
import com.gochathub.chat.models.Action
import com.gochathub.chat.models.Conversation
import com.gochathub.chat.models.Group
import com.gochathub.chat.models.BaseMessage
import com.gochathub.chat.models.CustomMessage
import com.gochathub.chat.models.MediaMessage
import com.gochathub.chat.models.MessageReceipt
import com.gochathub.chat.models.TextMessage
import com.gochathub.chat.models.TypingIndicator
import com.gochathub.chat.models.User
import com.cometchat.uikit.core.CometChatUIKit
import com.cometchat.uikit.core.constants.UIKitConstants

import com.cometchat.uikit.core.resources.soundmanager.CometChatSoundManager
import com.cometchat.uikit.core.resources.soundmanager.Sound
import com.cometchat.uikit.core.domain.usecase.DeleteConversationUseCase
import com.cometchat.uikit.core.domain.usecase.GetConversationListUseCase
import com.cometchat.uikit.core.domain.usecase.RefreshConversationListUseCase
import com.cometchat.uikit.core.events.CometChatConversationEvent
import com.cometchat.uikit.core.events.CometChatEvents
import com.cometchat.uikit.core.events.CometChatGroupEvent
import com.cometchat.uikit.core.events.CometChatMessageEvent
import com.cometchat.uikit.core.events.CometChatUserEvent
import com.cometchat.uikit.core.events.MessageStatus
import com.cometchat.uikit.core.state.DeleteState
import com.cometchat.uikit.core.state.UIState
import com.cometchat.uikit.core.utils.CometChatLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

/**
 * ViewModel that holds UI state and calls use cases.
 * Does NOT fetch data directly - delegates to use cases.
 * 
 * Implements [ListOperations] interface for standardized list manipulation.
 * All list operation methods are open for client override.
 * 
 * @param getConversationListUseCase Use case for fetching conversations
 * @param deleteConversationUseCase Use case for deleting conversations
 * @param refreshConversationListUseCase Use case for refreshing the list
 * @param enableListeners Whether to enable CometChat listeners (set to false for testing)
 */
open public class CometChatConversationsViewModel(
    private val getConversationListUseCase: GetConversationListUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val refreshConversationListUseCase: RefreshConversationListUseCase,
    private val enableListeners: Boolean = true
) : ViewModel(), ListOperations<Conversation> {
    
    // UI State
    private val _uiState = MutableStateFlow<UIState>(UIState.Loading)
    public val uiState: StateFlow<UIState> = _uiState.asStateFlow()
    
    // Conversations list
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    public val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()
    
    // Fetching state - prevents concurrent fetches
    private var isFetching = false
    
    // Has more data flag - tracks if there are more conversations to fetch
    private var hasMoreData = true
    
    // List operations delegate - handles internal list manipulation
    private val listDelegate = ListOperationsDelegate(
        stateFlow = _conversations,
        equalityChecker = { a, b -> a.conversationId == b.conversationId }
    )
    
    // Typing indicators
    private val _typingIndicators = MutableStateFlow<Map<String, TypingIndicator>>(emptyMap())
    public val typingIndicators: StateFlow<Map<String, TypingIndicator>> = _typingIndicators.asStateFlow()
    
    // Typing indicator debounce job
    private var typingDebounceJob: Job? = null
    private val typingIndicatorHashMap = mutableMapOf<String, TypingIndicator>()
    
    // Typing indicator debounce delay (1 second, matching Java implementation)
    private val TYPING_INDICATOR_DEBOUNCER = 1000L
    
    // Selection state
    private val _selectedConversations = MutableStateFlow<Set<Conversation>>(emptySet())
    public val selectedConversations: StateFlow<Set<Conversation>> = _selectedConversations.asStateFlow()
    
    // Delete state
    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    public val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()
    
    // Sound playback event - emits true when a sound should be played
    private val _playSoundEvent = MutableSharedFlow<Boolean>()
    public val playSoundEvent: SharedFlow<Boolean> = _playSoundEvent.asSharedFlow()
    
    // Scroll to top event - emits when list should scroll to top (new message received)
    private val _scrollToTopEvent = MutableSharedFlow<Unit>()
    public val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()
    
    // Configuration
    private var conversationsRequest: ConversationsRequest? = null
    private var conversationsRequestBuilder: ConversationsRequest.ConversationsRequestBuilder? = null
    private var disableReceipt = false
    private var disableSoundForMessages = false
    @RawRes private var customSoundForMessage: Int = 0
    private var soundManager: CometChatSoundManager? = null

    // GoChatHub realtime + connection jobs (the old SDK listener registrations)
    private var hubEventsJob: Job? = null
    private var connectionJob: Job? = null
    
    // Local event listener jobs
    private var conversationEventsJob: Job? = null
    private var groupEventsJob: Job? = null
    private var userEventsJob: Job? = null
    private var messageEventsJob: Job? = null
    
    init {
        if (enableListeners) {
            addListeners()
        }
        fetchConversations()
    }
    
    /**
     * Fetches conversations with pagination support.
     * Shows loading state only on initial fetch.
     * Uses client's request builder if set, otherwise creates a default one.
     * Prevents concurrent fetches using isFetching flag.
     */
    public fun fetchConversations() {
        // Prevent concurrent fetches and don't fetch if no more data
        if (isFetching || !hasMoreData) return
        
        viewModelScope.launch {
            isFetching = true
            
            if (_conversations.value.isEmpty()) {
                _uiState.value = UIState.Loading
            }
            
            // Create request on first fetch, reuse for pagination
            // Use client's builder if provided, otherwise create default
            if (conversationsRequest == null) {
                val builder = conversationsRequestBuilder 
                    ?: ConversationsRequest.ConversationsRequestBuilder()
                        .setLimit(30)
                conversationsRequest = builder.build()
            }
            
            conversationsRequest?.let { request ->
                getConversationListUseCase(request)
                    .onSuccess { newConversations ->
                        // Only append if we got new conversations
                        if (newConversations.isNotEmpty()) {
                            val updatedList = (_conversations.value + newConversations)
                                .distinctBy { it.conversationId }
                            _conversations.value = updatedList
                            _uiState.value = UIState.Content(updatedList)
                        } else {
                            // No new conversations means we've reached the end
                            hasMoreData = false
                            if (_conversations.value.isEmpty()) {
                                _uiState.value = UIState.Empty
                            }
                        }
                    }
                    .onFailure { exception ->
                        val cometChatException = if (exception is CometChatException) {
                            exception
                        } else {
                            CometChatException(
                                "ERR_UNKNOWN",
                                exception.message ?: "Unknown error",
                                exception.localizedMessage ?: "Unknown error"
                            )
                        }
                        _uiState.value = UIState.Error(cometChatException)
                    }
                
                isFetching = false
            } ?: run {
                isFetching = false
            }
        }
    }
    
    /**
     * Refreshes the conversation list from the beginning.
     * Clears existing data and fetches fresh.
     * This is a silent refresh - it does not show loading shimmer.
     * Uses client's request builder if set, otherwise creates a default one.
     */
    public fun refreshList() {
        // Reset pagination state for fresh fetch
        hasMoreData = true
        isFetching = false
        
        viewModelScope.launch {
            // Silent refresh - don't show loading state, keep existing data visible
            // Only show loading if list is empty
            if (_conversations.value.isEmpty()) {
                _uiState.value = UIState.Loading
            }
            
            // Use client's builder if provided, otherwise create default
            val builder = conversationsRequestBuilder
                ?: ConversationsRequest.ConversationsRequestBuilder()
                    .setLimit(30)

            val freshRequest = builder.build()

            refreshConversationListUseCase(freshRequest)
                .onSuccess { fetched ->
                    // Deduplicate before publishing — the list is keyed by conversationId in the
                    // UI, and a repeat would crash Compose on a duplicate key . The
                    // paginated fetch in fetchConversations() already does the same.
                    val conversations = fetched.distinctBy { it.conversationId }
                    _conversations.value = conversations
                    _uiState.value = if (conversations.isEmpty()) {
                        UIState.Empty
                    } else {
                        UIState.Content(conversations)
                    }

                    conversationsRequest = freshRequest

                    // Emit scroll to top event after refresh (new conversation at top)
                    _scrollToTopEvent.emit(Unit)
                }
                .onFailure { exception ->
                    // Only show error if list is empty, otherwise keep existing data
                    if (_conversations.value.isEmpty()) {
                        val cometChatException = if (exception is CometChatException) {
                            exception
                        } else {
                            CometChatException(
                                "ERR_UNKNOWN",
                                exception.message ?: "Unknown error",
                                exception.localizedMessage ?: "Unknown error"
                            )
                        }
                        _uiState.value = UIState.Error(cometChatException)
                    }
                }
        }
    }
    
    /**
     * Deletes a conversation.
     * Updates delete state throughout the operation.
     */
    /**
     * Pins a conversation for the current user. On success the returned conversation (carrying
     * pinnedAt/pinnedBy) is moved to the top of the list, matching the backend ordering.
     */
    public fun pinConversation(
        conversation: Conversation,
        onSuccess: (() -> Unit)? = null,
        onError: ((com.gochathub.chat.exceptions.CometChatException?) -> Unit)? = null
    ) {
        // ponytail: the server has no conversation-pin endpoint (only a pinned
        // message per room) — the pin reports unsupported so the UI can surface it.
        onError?.invoke(
            CometChatException("hub_unsupported", "Conversation pinning is not on the server")
        )
    }

    /**
     * Unpins a conversation for the current user. On success the conversation moves to the top of
     * the unpinned section — leaving it in place could strand an unpinned conversation inside the
     * pinned block at the head of the list.
     */
    public fun unpinConversation(conversation: Conversation, onSuccess: (() -> Unit)? = null) {
        // ponytail: no conversation-pin endpoint on the server; mirror [pinConversation].
    }

    public fun deleteConversation(conversation: Conversation) {
        viewModelScope.launch {
            _deleteState.value = DeleteState.InProgress
            
            deleteConversationUseCase(conversation)
                .onSuccess {
                    _conversations.value = _conversations.value.filter { 
                        it.conversationId != conversation.conversationId 
                    }
                    _deleteState.value = DeleteState.Success
                    
                    if (_conversations.value.isEmpty()) {
                        _uiState.value = UIState.Empty
                    }
                }
                .onFailure { exception ->
                    val cometChatException = if (exception is CometChatException) {
                        exception
                    } else {
                        CometChatException(
                            "ERR_UNKNOWN",
                            exception.message ?: "Unknown error",
                            exception.localizedMessage ?: "Unknown error"
                        )
                    }
                    _deleteState.value = DeleteState.Failure(cometChatException)
                }
        }
    }
    
    /**
     * Resets delete state to idle.
     * Call after handling delete success/failure.
     */
    public fun resetDeleteState() {
        _deleteState.value = DeleteState.Idle
    }
    
    /**
     * Checks if a message should increment the unread count.
     * For CustomMessage, checks metadata for `incrementUnreadCount` boolean,
     * falling back to `willUpdateConversation()` method.
     * 
     * @param message The message to check
     * @return true if unread count should be incremented
     */
    open public fun willUpdateIncrementUnreadCount(message: BaseMessage): Boolean {
        if (message is CustomMessage) {
            val metadata = message.metadata
            if (metadata != null && metadata.has("incrementUnreadCount")) {
                return try {
                    metadata.getBoolean("incrementUnreadCount")
                } catch (e: Exception) {
                    false
                }
            }
            return message.willUpdateConversation()
        }
        return false
    }
    
    /**
     * Determines if a custom message should update the conversation.
     * Returns true if the message should increment unread count OR if settings allow custom messages.
     * 
     * @param message The custom message to check
     * @return true if the conversation should be updated
     */
    open public fun shouldUpdateConversationForCustomMessage(message: CustomMessage): Boolean {
        return willUpdateIncrementUnreadCount(message) || 
            CometChatUIKit.getConversationUpdateSettings().shouldUpdateOnCustomMessages()
    }
    
    /**
     * Checks if a message is a threaded reply.
     * A message is threaded if it has a parentMessageId > 0.
     * 
     * @param message The message to check
     * @return true if the message is a threaded reply
     */
    open public fun isThreadedMessage(message: BaseMessage): Boolean {
        return message.parentMessageId > 0
    }
    
    /**
     * Checks if a conversation should be added to the list based on the conversation type filter.
     * Returns true if:
     * - No filter is set (conversationsRequest is null or conversationType is null)
     * - Filter is set to BOTH
     * - Conversation type matches the filter
     * 
     * @param conversation The conversation to check
     * @return true if the conversation should be added to the list
     */
    private fun isAddToConversationList(conversation: Conversation?): Boolean {
        if (conversation == null) return false
        
        val request = conversationsRequest ?: return true
        val filterType = request.conversationType ?: return true
        
        if (filterType.equals(UIKitConstants.ConversationType.BOTH, ignoreCase = true)) {
            return true
        }
        
        return conversation.conversationType.equals(filterType, ignoreCase = true)
    }
    
    /**
     * Selects or deselects a conversation based on selection mode.
     */
    public fun selectConversation(conversation: Conversation, mode: UIKitConstants.SelectionMode) {
        when (mode) {
            UIKitConstants.SelectionMode.SINGLE -> {
                _selectedConversations.value = setOf(conversation)
            }
            UIKitConstants.SelectionMode.MULTIPLE -> {
                val current = _selectedConversations.value.toMutableSet()
                if (current.contains(conversation)) {
                    current.remove(conversation)
                } else {
                    current.add(conversation)
                }
                _selectedConversations.value = current
            }
            UIKitConstants.SelectionMode.NONE -> {
                // Do nothing
            }
        }
    }
    
    /**
     * Clears all selected conversations.
     */
    public fun clearSelection() {
        _selectedConversations.value = emptySet()
    }
    
    /**
     * Returns the list of currently selected conversations.
     */
    public fun getSelectedConversations(): List<Conversation> =
        _selectedConversations.value.toList()
    
    /**
     * Sets the conversations request builder for customizing fetch parameters.
     * This will store the builder and reset the current request to use the new configuration.
     * The builder will be used for both initial fetch and refresh operations.
     *
     * @param builder The custom request builder provided by the client
     */
    public fun setConversationsRequestBuilder(
        builder: ConversationsRequest.ConversationsRequestBuilder
    ) {
        conversationsRequestBuilder = builder
        conversationsRequest = builder.build()
    }
    
    /**
     * Sets whether to disable read receipts.
     */
    public fun setDisableReceipt(disable: Boolean) {
        disableReceipt = disable
    }
    
    /**
     * Sets whether to disable sound for incoming messages.
     */
    public fun setDisableSoundForMessages(disable: Boolean) {
        disableSoundForMessages = disable
    }
    
    /**
     * Sets a custom sound resource for incoming messages.
     * Pass 0 to use the default sound.
     *
     * @param rawRes The raw resource ID of the custom sound.
     */
    public fun setCustomSoundForMessage(@RawRes rawRes: Int) {
        customSoundForMessage = rawRes
    }
    
    /**
     * Initializes the sound manager with the given context.
     * Must be called before sounds can be played.
     *
     * @param context The application context.
     */
    public fun initSoundManager(context: Context) {
        if (soundManager == null) {
            soundManager = CometChatSoundManager(context.applicationContext)
        }
    }
    
    /**
     * Plays the incoming message sound if sound is not disabled.
     */
    private fun playIncomingMessageSound() {
        if (!disableSoundForMessages) {
            viewModelScope.launch {
                _playSoundEvent.emit(true)
            }
            soundManager?.play(Sound.INCOMING_MESSAGE_FROM_OTHER, customSoundForMessage)
        }
    }

    
    /**
     * Adds CometChat listeners for real-time updates.
     */
    private fun addListeners() {
        // GoChatHub realtime — the old Conversation/Message/Connection listeners'
        // callbacks, driven by server frames. Conversation-level pin/unpin has no
        // frame (the hub's pinned frame is the room's pinned message; the server has
        // no conversation-pin endpoint), so only this VM's own pin calls apply state.
        hubEventsJob = com.cometchat.uikit.core.hub.HubBridge.events(viewModelScope, ::handleHubEvent)
        connectionJob = com.cometchat.uikit.core.hub.HubBridge.connection(viewModelScope) {
            refreshList()
        }

        // Add local event listeners
        addLocalEventListeners()
    }

    /** Suspended receipts decode (REST truth) → the old receipt handlers. */
    private fun handleHubEvent(env: com.cometchat.uikit.core.hub.WsEnvelope) {
        val hubEvents = com.cometchat.uikit.core.hub.HubEvents
        when (env.type) {
            // The old MessageListener's on*MessageReceived/onMessageEdited/onMessageDeleted.
            "message.created", "message.updated", "message.deleted" -> handleMessageEnvelope(env)

            // The old onMessagesDelivered/onMessagesRead. A receipt decode is suspend
            // (REST truth); a burst replaces the pending decode instead of queueing.
            "message.receipts_changed", "room.read_state_changed" -> {
                if (disableReceipt) return
                viewModelScope.launch {
                    hubEvents.receiptsChangedOf(env)?.let { receipt ->
                        if (receipt.receiptType == MessageReceipt.RECEIPT_TYPE_READ ||
                            receipt.receiptType == MessageReceipt.RECEIPT_TYPE_READ_BY_ALL
                        ) {
                            updateReadReceipts(receipt)
                        } else {
                            updateDeliveredReceipts(receipt)
                        }
                    }
                }
            }

            // The old onTypingStarted/onTypingEnded.
            "typing.started", "typing.stopped" ->
                hubEvents.typingIndicatorOf(env)?.let { indicator ->
                    if (env.type == "typing.started") addTypingIndicator(indicator)
                    else removeTypingIndicator(indicator)
                }

            // The old UserListener's onUserOnline/onUserOffline.
            "presence.changed" -> {
                val userId = hubEvents.userIdOf(env) ?: return
                val state = hubEvents.presenceStateOf(env) ?: return
                val conversationUser = _conversations.value.firstOrNull { conversation ->
                    conversation.conversationType == UIKitConstants.ConversationType.USERS &&
                        (conversation.conversationWith as? User)?.uid == userId
                }?.conversationWith as? User ?: return
                updateUserStatus(com.cometchat.uikit.core.hub.HubMappers.userWithPresence(conversationUser, state))
            }

            // The old GroupListener's member events — the update rides the room cache
            // (the frame only names the actor); the acting user keeps hasJoined.
            "room.member_added", "room.member_removed" -> {
                val hub = com.cometchat.uikit.core.hub.Hub
                val roomId = env.roomId ?: return
                hub.roomById(roomId) ?: return
                if (env.type == "room.member_added" && hubEvents.userIdOf(env) == hub.meId()) {
                    hubGroupFromList(roomId)?.setHasJoined(true)
                } else if (env.type == "room.member_removed" && hubEvents.userIdOf(env) == hub.meId()) {
                    // My own removal — drop the row (the old listener removed me via the action).
                    _conversations.value = _conversations.value.filter { it.conversationId != roomId }
                    updateUIStateFromList()
                    return
                }
                updateConversationForRoomAction(roomId)
            }
            // `room.pinned_changed` is the room's pinned MESSAGE (the server has no
            // conversation-pin frame), so there is no arm for it here — the panel VMs
            // own that frame.
        }
    }

    /** Server message frame → the checkAndUpdateConversation paths. */
    private fun handleMessageEnvelope(env: com.cometchat.uikit.core.hub.WsEnvelope) {
        val message = com.cometchat.uikit.core.hub.HubEvents.messageOf(env) ?: return
        if (env.type == "message.created") {
            checkAndUpdateConversation(message, true)
        } else {
            conversationFromMessage(message)?.let { updateConversation(it, isActionMessage = false) }
        }
    }

    /**
     * Room-membership frame → the old [updateConversationForGroupAction] path:
     * update (or drop, handled in [handleHubEvent]) the conversation for the touched room.
     */
    private fun updateConversationForRoomAction(roomId: String) {
        val hub = com.cometchat.uikit.core.hub.Hub
        val room = hub.roomById(roomId) ?: return
        val conversation = com.cometchat.uikit.core.hub.HubMappers.conversation(room)
        conversation.lastMessage = listOfConversationLastMessage(room.id) ?: return
        if (com.cometchat.uikit.core.CometChatUIKit.getConversationUpdateSettings().shouldUpdateOnGroupActions()) {
            updateConversation(conversation, isActionMessage = true)
        }
    }

    /** The list's current last message for a conversation id (frames carry none). */
    private fun listOfConversationLastMessage(conversationId: String?): com.gochathub.chat.models.BaseMessage? =
        _conversations.value.firstOrNull { it.conversationId == conversationId }?.lastMessage

    /**
     * Local stand-in for the closed SDK's CometChatHelper.getConversationFromMessage:
     * keys the room off the message's hub metadata and rebuilds the conversation from
     * the room cache, with [message] as its last message.
     */
    private fun conversationFromMessage(message: BaseMessage): Conversation? {
        val roomId = message.metadata?.optString(com.cometchat.uikit.core.hub.HubMappers.META_ROOM_ID)
            ?.takeIf { it.isNotEmpty() }
        val room = roomId?.let { com.cometchat.uikit.core.hub.Hub.roomById(it) } ?: return null
        val conversation = com.cometchat.uikit.core.hub.HubMappers.conversation(room)
        conversation.lastMessage = message
        conversation.updatedAt = message.sentAt
        return conversation
    }
    /**
     * Adds local event listeners for UI-triggered events.
     * These events are emitted by other UI components (e.g., when a message is sent from MessageComposer).
     */
    private fun addLocalEventListeners() {
        // Conversation events (e.g., conversation deleted from another screen)
        conversationEventsJob = viewModelScope.launch {
            CometChatEvents.conversationEvents.collect { event ->
                when (event) {
                    is CometChatConversationEvent.ConversationDeleted -> {
                        removeConversation(event.conversation)
                    }
                    is CometChatConversationEvent.ConversationUpdated -> {
                        updateConversationInList(event.conversation)
                    }
                }
            }
        }
        
        // Group events (e.g., group deleted, user left group from another screen)
        groupEventsJob = viewModelScope.launch {
            CometChatEvents.groupEvents.collect { event ->
                when (event) {
                    is CometChatGroupEvent.GroupDeleted -> {
                        removeGroup(event.group)
                    }
                    is CometChatGroupEvent.GroupLeft -> {
                        removeGroup(event.group)
                    }
                    is CometChatGroupEvent.MemberJoined -> {
                        updateGroupInConversation(event.group)
                    }
                    is CometChatGroupEvent.MembersAdded -> {
                        updateGroupInConversation(event.group)
                        // Fetch the conversation from server to get the real last message
                        // (locally-constructed Actions have id=0 and message=null)
                        refreshConversationFromServer(event.group.guid, CometChatConstants.CONVERSATION_TYPE_GROUP)
                    }
                    is CometChatGroupEvent.MemberKicked -> {
                        updateGroupInConversation(event.group)
                        refreshConversationFromServer(event.group.guid, CometChatConstants.CONVERSATION_TYPE_GROUP)
                    }
                    is CometChatGroupEvent.MemberBanned -> {
                        updateGroupInConversation(event.group)
                        refreshConversationFromServer(event.group.guid, CometChatConstants.CONVERSATION_TYPE_GROUP)
                    }
                    is CometChatGroupEvent.MemberUnbanned -> {
                        updateGroupInConversation(event.group)
                        refreshConversationFromServer(event.group.guid, CometChatConstants.CONVERSATION_TYPE_GROUP)
                    }
                    is CometChatGroupEvent.MemberScopeChanged -> {
                        updateGroupInConversation(event.group)
                        refreshConversationFromServer(event.group.guid, CometChatConstants.CONVERSATION_TYPE_GROUP)
                    }
                    is CometChatGroupEvent.OwnershipChanged -> {
                        updateGroupInConversation(event.group)
                    }
                    is CometChatGroupEvent.GroupCreated -> {
                        // Refresh list to show new group conversation
                        refreshList()
                    }
                }
            }
        }
        
        // User events (e.g., user blocked/unblocked from another screen)
        userEventsJob = viewModelScope.launch {
            CometChatEvents.userEvents.collect { event ->
                when (event) {
                    is CometChatUserEvent.UserBlocked -> {
                        // Remove user conversation if blocked users are not included
                        // Note: This depends on conversationsRequest configuration
                        removeUser(event.user)
                    }
                    is CometChatUserEvent.UserUnblocked -> {
                        // Update user status in conversation
                        updateUserStatus(event.user)
                    }
                }
            }
        }
        
        // Message events (e.g., message sent from MessageComposer)
        messageEventsJob = viewModelScope.launch {
            CometChatLogger.d("CometChatConvListVM", "Started collecting messageEvents")
            CometChatEvents.messageEvents.collect { event ->
                CometChatLogger.d("CometChatConvListVM", "Received messageEvent: ${event::class.simpleName}")
                when (event) {
                    is CometChatMessageEvent.MessageSent -> {
                        if (event.status == MessageStatus.SUCCESS) {
                            checkAndUpdateConversation(event.message, false)
                        }
                    }
                    is CometChatMessageEvent.MessageEdited -> {
                        if (event.status == MessageStatus.SUCCESS) {
                            checkAndUpdateConversation(event.message, false)
                        }
                    }
                    is CometChatMessageEvent.MessageDeleted -> {
                        checkAndUpdateConversation(event.message, false)
                    }
                    is CometChatMessageEvent.MessageRead -> {
                        CometChatLogger.d("CometChatConvListVM", "Received MessageRead event - conversationId=${event.message.conversationId}, messageId=${event.message.id}")
                        clearUnreadCountForMessage(event.message)
                    }
                    is CometChatMessageEvent.TextMessageReceived -> {
                        checkAndUpdateConversation(event.message, true)
                    }
                    is CometChatMessageEvent.MediaMessageReceived -> {
                        checkAndUpdateConversation(event.message, true)
                    }
                    is CometChatMessageEvent.CustomMessageReceived -> {
                        checkAndUpdateConversation(event.message, true)
                    }
                    else -> {
                        // Other message events don't affect conversation list
                    }
                }
            }
        }
    }
    
    /**
     * Removes the GoChatHub realtime listeners.
     */
    private fun removeListeners() {
        hubEventsJob?.cancel()
        connectionJob?.cancel()
    }
    

    
    /**
     * Updates conversation receipt status for delivered receipts.
     * Matches the Java implementation's updateDeliveredReceipts() method logic.
     * 
     * For USER conversations: matches by receipt.sender.uid == conversationWith.uid
     * For GROUP conversations: matches by receipt.receiverId == conversationWith.guid AND DELIVERED_TO_ALL type
     * 
     * @param receipt The message receipt containing delivery information
     */
    private fun updateDeliveredReceipts(receipt: MessageReceipt) {
        viewModelScope.launch {
            _conversations.value = _conversations.value.map { conversation ->
                val lastMessage = conversation.lastMessage
                
                // Skip if no last message, already delivered, or message ID doesn't match
                if (lastMessage == null || 
                    lastMessage.deliveredAt != 0L || 
                    lastMessage.id != receipt.messageId) {
                    return@map conversation
                }
                
                val shouldUpdate = when (receipt.receiverType) {
                    UIKitConstants.ReceiverType.USER -> {
                        // For user conversations: match by sender UID
                        conversation.conversationType == CometChatConstants.RECEIVER_TYPE_USER &&
                            receipt.sender?.uid == (conversation.conversationWith as? User)?.uid
                    }
                    UIKitConstants.ReceiverType.GROUP -> {
                        // For group conversations: match by receiver ID and DELIVERED_TO_ALL type
                        conversation.conversationType == CometChatConstants.RECEIVER_TYPE_GROUP &&
                            receipt.receiptType == MessageReceipt.RECEIPT_TYPE_DELIVERED_TO_ALL &&
                            receipt.receiverId == (conversation.conversationWith as? Group)?.guid
                    }
                    else -> false
                }
                
                if (shouldUpdate) {
                    // Clone to create new reference for Compose recomposition
                    conversation.clone().apply {
                        this.lastMessage = lastMessage.apply {
                            deliveredAt = receipt.deliveredAt
                        }
                    }
                } else {
                    conversation
                }
            }
        }
    }

    /**
     * Updates conversation receipt status for read receipts.
     * Matches the Java implementation's updateReadReceipts() method logic.
     * 
     * For USER conversations: matches by receipt.sender.uid == conversationWith.uid
     * For GROUP conversations: matches by receipt.receiverId == conversationWith.guid AND READ_BY_ALL type
     * 
     * Also clears unread count when the receipt is from the logged-in user.
     * 
     * @param receipt The message receipt containing read information
     */
    private fun updateReadReceipts(receipt: MessageReceipt) {
        viewModelScope.launch {
            val loggedInUser = getLoggedInUserSafe()
            val isReceiptFromLoggedInUser = loggedInUser != null && 
                receipt.sender?.uid?.equals(loggedInUser.uid, ignoreCase = true) == true
            
            _conversations.value = _conversations.value.map { conversation ->
                val lastMessage = conversation.lastMessage
                
                when (receipt.receiverType) {
                    UIKitConstants.ReceiverType.USER -> {
                        // For user conversations: match by sender UID
                        if (conversation.conversationType == CometChatConstants.RECEIVER_TYPE_USER &&
                            receipt.sender?.uid == (conversation.conversationWith as? User)?.uid) {
                            
                            // Check if we should update readAt timestamp
                            if (lastMessage != null && 
                                lastMessage.readAt == 0L && 
                                lastMessage.id == receipt.messageId) {
                                // Clone to create new reference for Compose recomposition
                                conversation.clone().apply {
                                    this.lastMessage = lastMessage.apply {
                                        readAt = receipt.readAt
                                    }
                                }
                            } else if (isReceiptFromLoggedInUser) {
                                // Clear unread count when receipt is from logged-in user
                                conversation.clone().apply {
                                    unreadMessageCount = 0
                                }
                            } else {
                                conversation
                            }
                        } else {
                            conversation
                        }
                    }
                    UIKitConstants.ReceiverType.GROUP -> {
                        // For group conversations: match by receiver ID and READ_BY_ALL type
                        if (conversation.conversationType == CometChatConstants.RECEIVER_TYPE_GROUP &&
                            receipt.receiptType == MessageReceipt.RECEIPT_TYPE_READ_BY_ALL &&
                            receipt.receiverId == (conversation.conversationWith as? Group)?.guid) {
                            
                            // Check if we should update readAt timestamp
                            if (lastMessage != null && 
                                lastMessage.readAt == 0L && 
                                lastMessage.id == receipt.messageId) {
                                // Clone to create new reference for Compose recomposition
                                conversation.clone().apply {
                                    this.lastMessage = lastMessage.apply {
                                        readAt = receipt.readAt
                                    }
                                }
                            } else {
                                conversation
                            }
                        } else if (isReceiptFromLoggedInUser) {
                            // Clear unread count when receipt is from logged-in user (for group conversations)
                            conversation.clone().apply {
                                unreadMessageCount = 0
                            }
                        } else {
                            conversation
                        }
                    }
                    else -> conversation
                }
            }
        }
    }

    /**
     * Updates conversation receipt status (legacy method - kept for compatibility).
     */
    private fun updateConversationReceipt(receipt: MessageReceipt) {
        viewModelScope.launch {
            _conversations.value = _conversations.value.map { conversation ->
                if (conversation.lastMessage?.id == receipt.messageId) {
                    // Clone to create new reference for Compose recomposition
                    conversation.clone()
                } else {
                    conversation
                }
            }
        }
    }
    
    /**
     * Updates unread count for a conversation.
     */
    private fun updateConversationUnreadCount(conversation: Conversation, count: Int) {
        CometChatLogger.d("CometChatConvListVM", "updateConversationUnreadCount() - conversationId=${conversation.conversationId}, newCount=$count")
        _conversations.value = _conversations.value.map {
            if (it.conversationId == conversation.conversationId) {
                // Clone to create new reference for Compose recomposition
                CometChatLogger.d("CometChatConvListVM", "updateConversationUnreadCount() - Updated conversation ${it.conversationId} unreadCount from ${it.unreadMessageCount} to $count")
                it.clone().apply { unreadMessageCount = count }
            } else {
                it
            }
        }
    }
    
    /**
     * Applies an externally supplied [Conversation] onto the matching entry in the list
     * without moving it to the top.
     *
     * Merge semantics: every field the caller actually supplied is taken, and the rest of
     * the existing entry is preserved. This is what lets an integrator refresh
     * [Conversation.conversationWith] — a [com.gochathub.chat.models.Group] whose metadata
     * changed server-side, say — for which the SDK emits no real-time event.
     *
     * The counters — [Conversation.unreadMessageCount], [Conversation.unreadMentionsCount],
     * [Conversation.lastReadMessageId] and [Conversation.latestMessageId] — are always taken
     * from [conversation]. An `Int`/`Long` has no "unset" value to tell apart from a deliberate
     * zero (marking a conversation read), so the supplied object stays authoritative for them.
     * A caller pushing an update for some other reason should source the conversation from the
     * SDK rather than hand-building one, so the counters carry real values:
     *
     * ```kotlin
     * CometChat.getConversation(guid, CometChatConstants.CONVERSATION_TYPE_GROUP,
     *     object : CometChat.CallbackListener<Conversation>() {
     *         override fun onSuccess(conversation: Conversation) {
     *             CometChatEvents.emitConversationEvent(ConversationUpdated(conversation))
     *         }
     *         override fun onError(e: CometChatException) = Unit
     *     })
     * ```
     *
     * [CometChatHelper.getConversationFromMessage] avoids the network call but leaves every
     * counter at zero, so anything built that way must carry the current values over first.
     *
     * Visibility is `internal` rather than `private` so unit tests can drive it directly;
     * it is not part of the public API.
     *
     * @param conversation The conversation carrying the updated properties.
     */
    internal fun updateConversationInList(conversation: Conversation) {
        _conversations.value = _conversations.value.map { existing ->
            if (existing.conversationId != conversation.conversationId) existing
            else mergeConversationUpdate(existing, conversation)
        }
    }
    
    /**
     * Adds a typing indicator and updates the conversation's isReceiverTyping property.
     * Updates immediately without debouncing.
     */
    private fun addTypingIndicator(typingIndicator: TypingIndicator) {
        val key = getTypingIndicatorKey(typingIndicator)
        typingIndicatorHashMap[key] = typingIndicator
        // Update immediately when typing starts
        _typingIndicators.value = typingIndicatorHashMap.toMap()
    }
    
    /**
     * Removes a typing indicator and updates the conversation's isReceiverTyping property.
     * Uses debouncing to prevent flickering when multiple users are typing.
     */
    private fun removeTypingIndicator(typingIndicator: TypingIndicator) {
        val key = getTypingIndicatorKey(typingIndicator)
        typingIndicatorHashMap.remove(key)
        
        // Cancel any pending debounce job
        typingDebounceJob?.cancel()
        
        // Debounce the update when typing ends to prevent flickering
        typingDebounceJob = viewModelScope.launch {
            delay(TYPING_INDICATOR_DEBOUNCER)
            _typingIndicators.value = typingIndicatorHashMap.toMap()
        }
    }
    
    /**
     * Gets the entity ID (user UID or group GUID) from a conversation.
     */
    private fun getConversationEntityId(conversation: Conversation): String? {
        return when (conversation.conversationType) {
            UIKitConstants.ConversationType.USERS -> (conversation.conversationWith as? User)?.uid
            UIKitConstants.ConversationType.GROUPS -> (conversation.conversationWith as? Group)?.guid
            else -> null
        }
    }
    
    /**
     * Gets a unique key for a typing indicator.
     */
    private fun getTypingIndicatorKey(typingIndicator: TypingIndicator): String {
        return "${typingIndicator.receiverType}_${typingIndicator.receiverId}_${typingIndicator.sender.uid}"
    }
    
    /**
     * Updates user status in conversations.
     */
    private fun updateUserStatus(user: User) {
        viewModelScope.launch {
            _conversations.value = _conversations.value.map { conversation ->
                if (conversation.conversationType == UIKitConstants.ConversationType.USERS) {
                    val conversationUser = conversation.conversationWith as? User
                    if (conversationUser?.uid == user.uid) {
                        // Clone to create new reference for Compose recomposition
                        conversation.clone().apply { 
                            conversationWith = user 
                        }
                    } else {
                        conversation
                    }
                } else {
                    conversation
                }
            }
        }
    }
    
    /**
     * Safely gets the logged-in user, returning null if SDK is not initialized.
     */
    private fun getLoggedInUserSafe(): User? {
        return try {
            CometChatUIKit.getLoggedInUser()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets a group from the conversation list by GUID.
     */
    private fun getGroupFromConversation(guid: String): Group? {
        for (conversation in _conversations.value) {
            if (conversation.conversationType == UIKitConstants.ConversationType.GROUPS) {
                val group = conversation.conversationWith as? Group
                if (group?.guid == guid) {
                    return group
                }
            }
        }
        return null
    }

    /** The list's Group for a room id (frames replace an existing row's group). */
    private fun hubGroupFromList(roomId: String): Group? = getGroupFromConversation(roomId)
    
    /**
     * Fetches the conversation from the server and updates the conversation list.
     * Used for group action events (member added/kicked/banned/etc.) where the
     * locally-constructed Action objects have id=0 and message=null.
     * The server provides the real conversation with proper last message.
     *
     * @param id The conversation entity ID (group GUID or user UID)
     * @param type The conversation type (group or user)
     */
    private fun refreshConversationFromServer(id: String, type: String) {
        viewModelScope.launch {
            try {
                val conversation = com.cometchat.uikit.core.data.repository.MessageListRepositoryImpl()
                    .getConversation(id, type)
                    .getOrNull()
                if (conversation != null && conversation.lastMessage != null) {
                    updateConversation(conversation, isActionMessage = true)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                CometChatLogger.e("CometChatConvListVM", "Failed to refresh conversation: ${e.message}")
            }
        }
    }

    /**
     * Removes a conversation from the list.
     */
    private fun removeConversation(conversation: Conversation) {
        val currentList = _conversations.value
        val newList = currentList.filter { it.conversationId != conversation.conversationId }
        _conversations.value = newList
        
        if (newList.isEmpty()) {
            _uiState.value = UIState.Empty
        } else {
            _uiState.value = UIState.Content(newList)
        }
    }
    
    /**
     * Removes a group conversation from the list.
     */
    private fun removeGroup(group: Group) {
        val currentList = _conversations.value
        val newList = currentList.filter { conversation ->
            if (conversation.conversationType == UIKitConstants.ConversationType.GROUPS) {
                val conversationGroup = conversation.conversationWith as? Group
                conversationGroup?.guid != group.guid
            } else {
                true
            }
        }
        _conversations.value = newList
        
        if (newList.isEmpty()) {
            _uiState.value = UIState.Empty
        } else {
            _uiState.value = UIState.Content(newList)
        }
    }
    
    /**
     * Removes a user conversation from the list.
     */
    private fun removeUser(user: User) {
        val currentList = _conversations.value
        val newList = currentList.filter { conversation ->
            if (conversation.conversationType == UIKitConstants.ConversationType.USERS) {
                val conversationUser = conversation.conversationWith as? User
                conversationUser?.uid != user.uid
            } else {
                true
            }
        }
        _conversations.value = newList
        
        if (newList.isEmpty()) {
            _uiState.value = UIState.Empty
        } else {
            _uiState.value = UIState.Content(newList)
        }
    }
    
    /**
     * Checks if the message should update the conversation based on threading and settings.
     * For threaded messages, checks shouldUpdateOnMessageReplies() setting.
     * Matches the Java implementation's checkAndUpdateConversation() method.
     * 
     * @param message The message to check
     * @param markAsDeliver Whether to mark the message as delivered (true for incoming messages)
     */
    private fun checkAndUpdateConversation(message: BaseMessage, markAsDeliver: Boolean) {
        if (isThreadedMessage(message)) {
            if (CometChatUIKit.getConversationUpdateSettings().shouldUpdateOnMessageReplies()) {
                handleMessageUpdate(message, markAsDeliver)
            }
        } else {
            handleMessageUpdate(message, markAsDeliver)
        }
    }
    
    /**
     * Handles message update with custom message filtering.
     * For CustomMessage, checks shouldUpdateConversationForCustomMessage().
     * Matches the Java implementation's handleMessageUpdate() method.
     * 
     * @param message The message to update
     * @param markAsDeliver Whether to mark the message as delivered
     */
    private fun handleMessageUpdate(message: BaseMessage, markAsDeliver: Boolean) {
        if (message is CustomMessage) {
            if (shouldUpdateConversationForCustomMessage(message)) {
                updateMessageDeliveryStatus(message, markAsDeliver)
            }
        } else {
            updateMessageDeliveryStatus(message, markAsDeliver)
        }
    }
    
    /**
     * Updates the conversation with the message and optionally marks as delivered.
     * Matches the Java implementation's updateMessageDeliveryStatus() method.
     * 
     * @param message The message to update
     * @param markAsDeliver Whether to mark the message as delivered and play sound
     */
    private fun updateMessageDeliveryStatus(message: BaseMessage, markAsDeliver: Boolean) {
        if (markAsDeliver) {
            processMessage(message)
        } else {
            val conversation = conversationFromMessage(message)
            if (conversation != null) {
                updateConversation(conversation, isActionMessage = false)
            }
        }
    }

    /**
     * Processes an incoming message by marking it as delivered and updating the conversation.
     * Matches the Java implementation's processMessage() method.
     *
     * @param message The message to process
     */
    private fun processMessage(message: BaseMessage) {
        markAsDeliverInternally(message)
        val conversation = conversationFromMessage(message)
        if (conversation != null) {
            updateConversation(conversation, isActionMessage = false)
        }
        playIncomingMessageSound()
    }

    /**
     * Marks a message as delivered internally if conditions are met.
     * Only marks as delivered if:
     * - The message sender is not the logged-in user
     * - Receipts are not disabled
     * Matches the Java implementation's markAsDeliverInternally() method.
     *
     * @param message The message to mark as delivered
     */
    private fun markAsDeliverInternally(message: BaseMessage) {
        val loggedInUser = getLoggedInUserSafe() ?: return
        val senderUid = message.sender?.uid ?: return

        if (!senderUid.equals(loggedInUser.uid, ignoreCase = true) && !disableReceipt) {
            // Delivered rides the socket ack (ADR-009) — the hub-backed datasource path.
            viewModelScope.launch {
                runCatching {
                    com.cometchat.uikit.core.data.datasource.HubImpls.delivered(message)
                }
            }
        }
    }
    
    /**
     * Clears unread count for a conversation based on a message.
     */
    private fun clearUnreadCountForMessage(message: BaseMessage) {
        val conversationId = message.conversationId
        val currentList = _conversations.value
        
        CometChatLogger.d("CometChatConvListVM", "clearUnreadCountForMessage() - conversationId=$conversationId, currentListSize=${currentList.size}")
        
        val conversation = currentList.find { it.conversationId == conversationId }
        if (conversation != null) {
            CometChatLogger.d("CometChatConvListVM", "clearUnreadCountForMessage() - Found conversation, current unreadCount=${conversation.unreadMessageCount}, updating to 0")
            updateConversationUnreadCount(conversation, 0)
        } else {
            CometChatLogger.d("CometChatConvListVM", "clearUnreadCountForMessage() - Conversation NOT found in list. Available conversationIds: ${currentList.map { it.conversationId }}")
        }
    }
    
    /**
     * Insertion point for a conversation surfacing on new activity: the first index past the
     * pinned block at the head of the list. Pinned conversations always stay above realtime
     * reordering, matching the backend's pinned-first fetch ordering.
     */
    private fun firstUnpinnedIndex(list: List<Conversation>): Int {
        val index = list.indexOfFirst { !it.isPinned }
        return if (index >= 0) index else list.size
    }

    /**
     * Updates a conversation in the list with proper handling of last message and unread count.
     * Matches the Java implementation's update() method logic.
     *
     * @param conversation The conversation to update
     * @param isActionMessage Whether this is an action message (group action)
     */
    private fun updateConversation(conversation: Conversation, isActionMessage: Boolean) {
        if (conversation.lastMessage == null) return
        
        val loggedInUser = getLoggedInUserSafe()
        val lastMessage = conversation.lastMessage
        val isSentByMe = loggedInUser != null &&
            lastMessage?.sender?.uid?.equals(loggedInUser.uid, ignoreCase = true) == true

        var applied = false
        var scrollToTop = false

        // Atomic read-modify-write. The SDK invokes the message listeners straight off the
        // WebSocket thread — these callbacks are NOT wrapped in viewModelScope.launch — so a
        // plain `_conversations.value = ...` here can interleave with the fetch coroutine on
        // Main and leave the same conversationId in the list twice, which crashes the
        // LazyColumn with "Key ... was already used" (ENG-35566). updateAndGet re-runs this
        // block on contention, so it must stay free of side effects; the UI state and
        // scroll event are published afterwards.
        val newList = _conversations.updateAndGet { currentList ->
            val existingIndex = currentList.indexOfFirst {
                it.conversationId == conversation.conversationId
            }

            when {
                existingIndex >= 0 -> {
                    val oldConversation = currentList[existingIndex]

                    // Clone the conversation to create a new reference for Compose recomposition
                    val updatedConversation = conversation.clone()

                    // Preserve the conversationWith from old conversation (it has more complete data)
                    updatedConversation.conversationWith = oldConversation.conversationWith

                    // CometChatHelper.getConversationFromMessage() never carries per-user pin state,
                    // so keep it from the copy already in the list or the pin would be lost on update.
                    updatedConversation.pinnedAt = oldConversation.pinnedAt
                    updatedConversation.pinnedBy = oldConversation.pinnedBy

                    updatedConversation.unreadMessageCount = when {
                        // Action messages and our own messages never bump the unread count
                        isActionMessage || isSentByMe -> oldConversation.unreadMessageCount
                        // A genuinely new, unread message from someone else does
                        oldConversation.lastMessage?.id != lastMessage?.id && lastMessage?.readAt == 0L ->
                            oldConversation.unreadMessageCount + 1
                        else -> oldConversation.unreadMessageCount
                    }

                    applied = true
                    // A pinned conversation updating in place shouldn't yank the list back to
                    // the top, so only unpinned updates request a scroll.
                    scrollToTop = !updatedConversation.isPinned

                    // Pinned conversations keep their slot (the pinned block mirrors the
                    // backend's pinnedAt ordering); unpinned ones surface at the top of the
                    // unpinned section so new activity never displaces the pinned block.
                    currentList.toMutableList().apply {
                        removeAt(existingIndex)
                        val targetIndex =
                            if (updatedConversation.isPinned) existingIndex else firstUnpinnedIndex(this)
                        add(targetIndex, updatedConversation)
                    }
                }

                // Conversation not in list, check if it should be added based on filter
                isAddToConversationList(conversation) -> {
                    val updatedConversation = conversation.clone()

                    // Set unread count to 1 for new conversations from others (not action messages)
                    if (!isSentByMe && !isActionMessage && lastMessage !is Action) {
                        updatedConversation.unreadMessageCount = 1
                    }

                    applied = true
                    scrollToTop = true

                    // New conversations enter below any pinned block, at the top of the unpinned section.
                    currentList.toMutableList().apply {
                        add(firstUnpinnedIndex(this), updatedConversation)
                    }
                }

                else -> {
                    applied = false
                    currentList
                }
            }
        }

        if (!applied) return

        _uiState.value = UIState.Content(newList)

        // Emit scroll to top event only when the conversation actually surfaced
        if (scrollToTop) {
            viewModelScope.launch {
                _scrollToTopEvent.emit(Unit)
            }
        }
    }
    
    /**
     * Updates group in conversations.
     */
    private fun updateGroupInConversation(group: Group) {
        viewModelScope.launch {
            _conversations.value = _conversations.value.map { conversation ->
                if (conversation.conversationType == UIKitConstants.ConversationType.GROUPS) {
                    val conversationGroup = conversation.conversationWith as? Group
                    if (conversationGroup?.guid == group.guid) {
                        // Clone to create new reference for Compose recomposition
                        conversation.clone().apply { 
                            conversationWith = group 
                        }
                    } else {
                        conversation
                    }
                } else {
                    conversation
                }
            }
        }
    }
    
    // ==================== ListOperations Interface Implementation ====================
    // All methods are open for client override
    
    /**
     * Adds a single conversation to the list.
     * Override to add custom validation or processing.
     *
     * @param item The conversation to add
     */
    override fun addItem(item: Conversation) {
        val accepted = rejectAlreadyPresent(listOf(item))
        if (accepted.isEmpty()) return
        listDelegate.addItem(accepted.first())
        updateUIStateFromList()
    }
    
    /**
     * Adds multiple conversations to the list.
     * Override to add custom validation or processing.
     *
     * @param items The conversations to add
     */
    override fun addItems(items: List<Conversation>) {
        val accepted = rejectAlreadyPresent(items)
        if (accepted.isEmpty()) return
        listDelegate.addItems(accepted)
        updateUIStateFromList()
    }

    /**
     * Drops conversations already present in the list, and duplicates within [items] itself.
     *
     * The list is keyed by `conversationId` in the UI, so appending a conversation that is
     * already rendered crashes Compose with
     * `IllegalArgumentException("Key ... was already used")` (ENG-35566). Conversations without
     * an id cannot be compared and are passed through — the render layer keys those by identity.
     */
    private fun rejectAlreadyPresent(items: List<Conversation>): List<Conversation> {
        val present = _conversations.value.mapNotNull { it.conversationId }.toMutableSet()
        return items.filter { conversation ->
            val id = conversation.conversationId ?: return@filter true
            present.add(id)
        }
    }
    
    /**
     * Removes a conversation from the list.
     * Override to add custom confirmation or logging.
     *
     * @param item The conversation to remove
     * @return true if removed, false if not found
     */
    override fun removeItem(item: Conversation): Boolean {
        val result = listDelegate.removeItem(item)
        if (result) {
            updateUIStateFromList()
        }
        return result
    }
    
    /**
     * Removes a conversation at the specified index.
     * Override to add custom confirmation or logging.
     *
     * @param index The index of the conversation to remove
     * @return The removed conversation, or null if index is out of bounds
     */
    override fun removeItemAt(index: Int): Conversation? {
        val result = listDelegate.removeItemAt(index)
        if (result != null) {
            updateUIStateFromList()
        }
        return result
    }
    
    /**
     * Updates a conversation matching the predicate.
     * Override to add custom validation or transformation.
     *
     * @param item The new conversation to replace with
     * @param predicate Function to find the conversation to update
     * @return true if updated, false if no match found
     */
    override fun updateItem(item: Conversation, predicate: (Conversation) -> Boolean): Boolean {
        val result = listDelegate.updateItem(item, predicate)
        if (result) {
            updateUIStateFromList()
        }
        return result
    }
    
    /**
     * Removes all conversations from the list.
     * Override to add custom confirmation or cleanup.
     */
    override fun clearItems() {
        listDelegate.clearItems()
        updateUIStateFromList()
    }
    
    /**
     * Returns a copy of all conversations in the list.
     *
     * @return Immutable list of all conversations
     */
    override fun getItems(): List<Conversation> {
        return listDelegate.getItems()
    }
    
    /**
     * Returns the conversation at the specified index.
     *
     * @param index The index of the conversation
     * @return The conversation at the index, or null if out of bounds
     */
    override fun getItemAt(index: Int): Conversation? {
        return listDelegate.getItemAt(index)
    }
    
    /**
     * Returns the number of conversations in the list.
     *
     * @return The conversation count
     */
    override fun getItemCount(): Int {
        return listDelegate.getItemCount()
    }
    
    /**
     * Moves a conversation to the top of the list.
     * Override to add custom logic or scroll behavior.
     *
     * @param item The conversation to move to top
     */
    override fun moveItemToTop(item: Conversation) {
        listDelegate.moveItemToTop(item)
        updateUIStateFromList()
    }
    
    /**
     * Performs multiple operations in a single batch, emitting only once.
     * Critical for performance when receiving many updates rapidly.
     * Override to add custom batch processing logic.
     *
     * @param operations Lambda that performs multiple list operations
     */
    override fun batch(operations: ListOperationsBatchScope<Conversation>.() -> Unit) {
        listDelegate.batch(operations)
        updateUIStateFromList()
    }
    
    /**
     * Updates the UI state based on the current list contents.
     */
    private fun updateUIStateFromList() {
        val currentList = _conversations.value
        _uiState.value = if (currentList.isEmpty()) {
            UIState.Empty
        } else {
            UIState.Content(currentList)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        if (enableListeners) {
            removeListeners()
        }
        // Cancel local event listener jobs
        conversationEventsJob?.cancel()
        groupEventsJob?.cancel()
        userEventsJob?.cancel()
        messageEventsJob?.cancel()
        
        // Cancel list delegate debounce operations
        listDelegate.cancel()
        
        typingDebounceJob?.cancel()
        soundManager?.release()
        soundManager = null
    }
}

/**
 * Merges an externally supplied conversation onto an existing list entry.
 *
 * See [CometChatConversationsViewModel.updateConversationInList] for the reasoning; this is
 * the merge itself, kept free of the ViewModel so it can be exercised directly.
 *
 * @param existing The entry currently in the list.
 * @param update The conversation carrying the updated properties.
 */
internal fun mergeConversationUpdate(existing: Conversation, update: Conversation): Conversation =
    existing.clone().apply {
        // Counters are taken unconditionally: an Int/Long has no "unset" value to tell apart
        // from a deliberate zero, so the supplied object stays authoritative for them.
        unreadMessageCount = update.unreadMessageCount
        unreadMentionsCount = update.unreadMentionsCount
        lastReadMessageId = update.lastReadMessageId
        latestMessageId = update.latestMessageId
        // Reference fields are taken only when supplied, so a caller refreshing one of them
        // does not blank the others.
        update.conversationWith?.let { conversationWith = it }
        update.lastMessage?.let { lastMessage = it }
        update.tags?.let { tags = it }
        if (update.updatedAt > 0) updatedAt = update.updatedAt
    }
