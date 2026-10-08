package com.cometchat.uikit.core.hub

import java.net.URI
import java.net.URLDecoder

/**
 * The web UI's mobile sign-in QR: `gochathub://login?server=<origin>&token=<api key>`.
 * Anything else (other scheme/host, missing parts, non-http server) is rejected.
 */
public object MobileSignIn {
    public data class Credentials(val server: String, val token: String)

    private const val MAX_TOKEN = 512

    public fun parse(text: String): Credentials? {
        val uri = try { URI(text.trim()) } catch (_: Exception) { return null }
        if (uri.scheme != "gochathub" || uri.host != "login") return null
        val query = uri.rawQuery?.split('&')?.mapNotNull { pair ->
            pair.split('=', limit = 2).takeIf { it.size == 2 }
                ?.let { URLDecoder.decode(it[0], "UTF-8") to URLDecoder.decode(it[1], "UTF-8") }
        }?.toMap() ?: return null
        val server = query["server"]?.trim()?.trimEnd('/')
        val token = query["token"]
        if (server.isNullOrEmpty() || token.isNullOrEmpty() || token.length > MAX_TOKEN) return null
        if (!server.startsWith("https://") && !server.startsWith("http://")) return null
        return Credentials(server, token)
    }
}
