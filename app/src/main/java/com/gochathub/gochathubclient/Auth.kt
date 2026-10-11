package com.gochathub.gochathubclient

import android.content.Context
import com.cometchat.uikit.core.hub.AuthResponseDto
import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.MobileSignIn
import com.gochathub.gochathubclient.push.Push
import com.gochathub.gochathubclient.ui.Accent

/**
 * Session flows: login stores the bearer token and the user payload; app
 * start revalidates against GET /users/me and opens the socket; 401 wipes.
 */
public object Auth {

    /** Failure carries [com.cometchat.uikit.core.hub.HubApiException.challenge] when the account needs a 2FA code. */
    public suspend fun login(baseUrl: String, username: String, password: String): Result<AuthResponseDto> =
        start { Hub.client.login(baseUrl, username, password) }

    public suspend fun login2fa(challenge: String, code: String): Result<AuthResponseDto> =
        start { Hub.client.login2fa(challenge, code) }

    /**
     * Personal API token (minted server-side, e.g. `gochathub-server token create`):
     * skips password, Turnstile and 2FA. Validated with GET /users/me before it is kept.
     */
    public suspend fun loginWithToken(baseUrl: String, token: String): Result<AuthResponseDto> =
        start {
            Hub.client.baseUrl = baseUrl
            Hub.store.token = token
            AuthResponseDto(token, Hub.client.me())
        }.onFailure { Hub.store.token = "" }

    private val handoffPrefs get() = Hub.ctx.getSharedPreferences("gochathub_handoff", Context.MODE_PRIVATE)

    /**
     * Browser sign-in (passkeys): remember the PKCE verifier and `state` across process
     * death and return the web sign-in URL to open. The page deep-links back with a
     * one-time code, see [completeBrowserLogin].
     */
    public fun beginBrowserLogin(server: String): String {
        val p = MobileSignIn.newPending(server)
        handoffPrefs.edit().putString("server", p.server).putString("state", p.state)
            .putString("verifier", p.verifier).apply()
        return p.loginUrl()
    }

    /** Redeem the return link; the pending hand-off is single-use whatever the outcome. */
    public suspend fun completeBrowserLogin(link: MobileSignIn.Link): Result<AuthResponseDto> {
        val prefs = handoffPrefs
        val server = prefs.getString("server", null)
        val state = prefs.getString("state", null)
        val verifier = prefs.getString("verifier", null)
        prefs.edit().clear().apply()
        if (server == null || state == null || verifier == null) {
            return Result.failure(IllegalStateException("No browser sign-in in progress."))
        }
        if (state != link.state) return Result.failure(IllegalStateException("Sign-in link did not match."))
        return try {
            Hub.client.baseUrl = server
            loginWithToken(server, Hub.client.exchangeMobileCode(link.code, verifier))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun start(authenticate: suspend () -> AuthResponseDto): Result<AuthResponseDto> =
        try {
            val response = authenticate()
            Hub.store.token = response.token.orEmpty()
            Hub.me = response.user
            Accent.load()
            Hub.socket.connect()
            Push.register(Hub.ctx)
            Result.success(response)
        } catch (t: Exception) {
            Result.failure(t)
        }

    public suspend fun logout() {
        Push.deleteDevice()
        try {
            Hub.client.logout()
        } catch (_: Exception) { /* wipe regardless */ }
        Hub.socket.close()
        Hub.wipe()
        Push.unregister(Hub.ctx)
        Accent.hex = Accent.DEFAULT
    }

    /**
     * App start with a stored token: validate it and open the socket.
     * False ⇒ token dead; wipe and show login (ADR-015 401 rule).
     */
    public suspend fun restore(): Boolean {
        if (!Hub.store.hasSession()) return false
        return try {
            Hub.me = Hub.client.me()
            Accent.load()
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