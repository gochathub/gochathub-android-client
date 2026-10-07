package com.gochathub.gochathubclient

import com.cometchat.uikit.core.hub.AuthResponseDto
import com.cometchat.uikit.core.hub.Hub
import com.gochathub.gochathubclient.push.Push

/**
 * Session flows: login stores the bearer token and the user payload; app
 * start revalidates against GET /users/me and opens the socket; 401 wipes.
 */
public object Auth {

    public suspend fun login(baseUrl: String, username: String, password: String): Result<AuthResponseDto> =
        try {
            val response = Hub.client.login(baseUrl, username, password)
            Hub.store.token = response.token.orEmpty()
            Hub.me = response.user
            Hub.socket.connect()
            Push.register(Hub.ctx)
            Result.success(response)
        } catch (t: Exception) {
            Result.failure(t)
        }

    public suspend fun logout() {
        try {
            Hub.client.logout()
        } catch (_: Exception) { /* wipe regardless */ }
        Hub.socket.close()
        Hub.wipe()
    }

    /**
     * App start with a stored token: validate it and open the socket.
     * False ⇒ token dead; wipe and show login (ADR-015 401 rule).
     */
    public suspend fun restore(): Boolean {
        if (!Hub.store.hasSession()) return false
        return try {
            Hub.me = Hub.client.me()
            Hub.socket.connect()
            Push.register(Hub.ctx)
            true
        } catch (e: com.cometchat.uikit.core.hub.HubApiException) {
            if (e.httpStatus == 401) Hub.wipe()
            false
        } catch (_: Exception) {
            false // transport error: treat as logged-out state until retry
        }
    }
}