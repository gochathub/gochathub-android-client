package com.cometchat.uikit.core.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** QR payload built by the web UI: gochathub://login?server=<origin>&token=<key>. */
class MobileSignInTest {
    @Test
    fun `parses what the web UI emits`() {
        val c = MobileSignIn.parse("gochathub://login?server=https%3A%2F%2Fchat.example.com&token=a-b_c%3D")
        assertEquals(MobileSignIn.Credentials("https://chat.example.com", "a-b_c="), c)
    }

    @Test
    fun `trims whitespace and a trailing slash on the server`() {
        val c = MobileSignIn.parse(" gochathub://login?server=http%3A%2F%2F10.0.0.5%3A8090%2F&token=t \n")
        assertEquals(MobileSignIn.Credentials("http://10.0.0.5:8090", "t"), c)
    }

    @Test
    fun `rejects anything that is not a goChatHub login link`() {
        listOf(
            "https://login?server=https%3A%2F%2Fx.test&token=t", // wrong scheme
            "gochathub://evil?server=https%3A%2F%2Fx.test&token=t", // wrong host
            "gochathub://login?server=https%3A%2F%2Fx.test", // no token
            "gochathub://login?server=https%3A%2F%2Fx.test&token=", // empty token
            "gochathub://login?token=t", // no server
            "gochathub://login?server=ftp%3A%2F%2Fx.test&token=t", // server not http(s)
            "gochathub://login?server=javascript%3Aalert(1)&token=t",
            "gochathub://login?server=https%3A%2F%2Fx.test&token=" + "a".repeat(513), // oversize
            "not a url at all",
            "",
        ).forEach { assertNull(it, MobileSignIn.parse(it)) }
    }
}
