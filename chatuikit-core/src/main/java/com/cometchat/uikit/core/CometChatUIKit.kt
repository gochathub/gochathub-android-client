package com.cometchat.uikit.core

import android.content.Context
import com.gochathub.chat.core.CometChat
import com.gochathub.chat.exceptions.CometChatException
import com.gochathub.chat.models.ConversationUpdateSettings
import com.gochathub.chat.models.User
import com.cometchat.uikit.core.utils.CometChatLogger
import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.HubMappers

/**
 * Kit entry point, rewired to GoChatHub (datasource seam). The closed cloud
 * SDK is never initialized — session state, presence and realtime come from
 * `com.cometchat.uikit.core.hub` (HubStore/HubClient/HubSocket).
 *
 * Login happens at the app level (`HubClient.login` with `token_request:
 * true`). `init(context, settings, listener)` only wires the kit.
 */
public object CometChatUIKit {
    private const val TAG = "GoChatHubUIKit"

    @Volatile
    private var authenticationSettings: UIKitSettings? = null

    /**
     * Wires the kit to the server deployment. `appId`/`region` on
     * [UIKitSettings] are carried but never sent anywhere.
     */
    public fun init(
        context: Context,
        authSettings: UIKitSettings,
        callbackListener: CometChat.CallbackListener<String>?
    ) {
        CometChatLogger.initFromContext(context)
        authenticationSettings = authSettings
        Hub.init(context)
        callbackListener?.onSuccess("gochathub")
    }

    public fun isSDKInitialized(): Boolean = Hub.me != null

    /** Maps the session payload (GET /users/me) into the kit User. */
    public fun getLoggedInUser(): User? = Hub.me?.let { HubMappers.user(it) }

    public fun getLoggedInUserAsync(callbackListener: CometChat.CallbackListener<User>?) {
        val mapped = getLoggedInUser()
        if (mapped != null) callbackListener?.onSuccess(mapped)
        else callbackListener?.onError(CometChatException("ERR_NOT_LOGGED_IN", "No session"))
    }

    /**
     * Thread-subscription (follow/unfollow) gating: the server models
     * single-level replies without following, so nothing subscribes; the flag
     * stays for kit UI gating.
     */
    public fun isThreadSubscriptionEnabled(): Boolean =
        authenticationSettings?.enableThreadSubscription ?: true

    /** Server supports pinning a message per room (admin). */
    public fun isPinMessageEnabled(): Boolean = true

    /** No saved-messages endpoint on the server. */
    public fun isSaveMessageEnabled(): Boolean = false

    /** No conversation pinning on the server. */
    public fun isPinConversationEnabled(): Boolean = false

    public fun getConversationUpdateSettings(): ConversationUpdateSettings =
        ConversationUpdateSettings()

    /** Kept for kit code reading its own settings back; the cloud fields are ignored. */
    public fun getAuthSettings(): UIKitSettings? = authenticationSettings
}