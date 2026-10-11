package com.cometchat.uikit.core.hub

import java.net.URI
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * The web UI's mobile sign-in QR: `gochathub://login?server=<origin>&token=<api key>`.
 * Anything else (other scheme/host, missing parts, non-http server) is rejected.
 *
 * Also the browser hand-off (passkeys): the app opens the web sign-in with a PKCE
 * challenge and `state` ([Pending.loginUrl]); the page deep-links back
 * `gochathub://login?server=&code=&state=` ([parseHandoff]) and the app redeems the
 * one-time code with its verifier.
 */
public object MobileSignIn {
    public data class Credentials(val server: String, val token: String)

    /** Hand-off the app started and is waiting on; persist it across process death. */
    public data class Pending(val server: String, val state: String, val verifier: String) {
        public val challenge: String
            get() = B64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))

        public fun loginUrl(): String = "$server/access/sign-in?mobile=1&challenge=$challenge&state=$state"
    }

    /** The return link: one-time [code] and the [state] this app started with. */
    public data class Link(val code: String, val state: String)

    private const val MAX_TOKEN = 512
    private val B64 = Base64.getUrlEncoder().withoutPadding()
    private val random = SecureRandom()

    public fun newPending(server: String): Pending = Pending(server.trim().trimEnd('/'), randomToken(), randomToken())

    private fun randomToken(): String = ByteArray(32).also(random::nextBytes).let(B64::encodeToString)

    private fun query(text: String): Map<String, String>? {
        val uri = try { URI(text.trim()) } catch (_: Exception) { return null }
        if (uri.scheme != "gochathub" || uri.host != "login") return null
        return uri.rawQuery?.split('&')?.mapNotNull { pair ->
            pair.split('=', limit = 2).takeIf { it.size == 2 }
                ?.let { URLDecoder.decode(it[0], "UTF-8") to URLDecoder.decode(it[1], "UTF-8") }
        }?.toMap()
    }

    public fun parse(text: String): Credentials? {
        val query = query(text) ?: return null
        val server = query["server"]?.trim()?.trimEnd('/')
        val token = query["token"]
        if (server.isNullOrEmpty() || token.isNullOrEmpty() || token.length > MAX_TOKEN) return null
        if (!server.startsWith("https://") && !server.startsWith("http://")) return null
        return Credentials(server, token)
    }

    /** The server comes from [Pending], never from the link: an attacker's link cannot redirect the exchange. */
    public fun parseHandoff(text: String): Link? {
        val query = query(text) ?: return null
        val code = query["code"]
        val state = query["state"]
        if (code.isNullOrEmpty() || state.isNullOrEmpty() || code.length > MAX_TOKEN || state.length > MAX_TOKEN) return null
        return Link(code, state)
    }
}
