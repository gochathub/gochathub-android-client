package com.cometchat.uikit.core.hub

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** REST surface against a fake server: auth header, error envelope, pagination. */
class HubClientTest {
    private lateinit var server: MockWebServer
    private lateinit var store: FakeStore
    private lateinit var client: HubClient

    private class FakeStore : HubSession {
        override var token: String = ""
        override var baseUrl: String = ""
        override var deviceId: String = ""
        override fun hasSession(): Boolean = token.isNotEmpty()
        override fun wipe() { token = ""; deviceId = "" }
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        store = FakeStore()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun connect() {
        client = HubClient(store)
        client.baseUrl = server.url("/").toString().trimEnd('/')
    }

    @Test
    fun `login posts token_request and stores base url`() = runBlocking {
        connect()
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"token":"sess-1","user":{"id":"u1","username":"brian","display_name":"Brian","role":"user","avatar_url":""}}"""
            )
        )
        val response = client.login(server.url("/").toString(), "brian", "pw")
        assertEquals("sess-1", response.token)
        assertEquals("brian", response.user.username)
        val recorded = server.takeRequest()
        assertEquals("/api/v1/auth/login", recorded.path)
        assertEquals(server.url("/").toString().trimEnd('/'), store.baseUrl.trimEnd('/'))
        assertEquals("POST", recorded.method)
        val body = recorded.body.readUtf8()
        org.junit.Assert.assertTrue(body.contains("\"token_request\":true"))
    }

    @Test
    fun `bearer rides every request`() = runBlocking {
        connect()
        store.token = "sess-7"
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"items":[],"next_cursor":null}"""))
        client.messages("room-1")
        assertEquals("Bearer sess-7", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `error envelope decodes to HubApiException with stable code`() = runBlocking {
        connect()
        server.enqueue(
            MockResponse().setResponseCode(403).setBody(
                """{"error":{"code":"forbidden","message":"not permitted"}}"""
            )
        )
        try {
            runBlocking { client.room("r1") }
            fail("should throw")
        } catch (e: HubApiException) {
            assertEquals(403, e.httpStatus)
            assertEquals("forbidden", e.code)
            assertEquals("not permitted", e.message)
        }
    }

    @Test
    fun `retry-after rides rate_limited`() = runBlocking {
        connect()
        server.enqueue(
            MockResponse().setResponseCode(429).setBody("""{"error":{"code":"rate_limited","message":"slow down"}}""")
                .setHeader("Retry-After", "5")
        )
        try {
            runBlocking { client.rooms() }
            fail("should throw")
        } catch (e: HubApiException) {
            assertEquals(5L, e.retryAfterSeconds)
        }
    }

    @Test
    fun `messages passes cursor back verbatim`() = runBlocking {
        connect()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"items":[],"next_cursor":null}"""))
        val page = client.messages("room-1", 50, "abc123")
        assertNull(page.nextCursor)
        val recorded = server.takeRequest()
        assertEquals("/api/v1/rooms/room-1/messages?limit=50&before=abc123", recorded.path)
    }

    @Test
    fun `empty page keeps no cursor`() = runBlocking {
        connect()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"items":[],"next_cursor":null}"""))
        val page = client.messages("room-1")
        assertNull(page.nextCursor)
        assertEquals("/api/v1/rooms/room-1/messages?limit=50", server.takeRequest().path)
    }
}