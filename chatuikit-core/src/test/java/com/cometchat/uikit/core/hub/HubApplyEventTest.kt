package com.cometchat.uikit.core.hub

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HubApplyEventTest {
    @After
    fun clear() {
        Hub.lastMessageCache.clear()
        Hub.rememberMembers("r1", emptyList())
    }

    private fun frame(id: String, createdAt: String): WsEnvelope {
        val message = """{"id":"$id","room_id":"r1","author_id":"u1","body":"b","created_at":"$createdAt"}"""
        return WsEnvelope(
            type = "message.created", roomId = "r1",
            data = mapOf("message" to JSON.parseToJsonElement(message))
        )
    }

    @Test
    fun `older message frame does not replace the newer preview`() {
        Hub.applyEvent(frame("new", "2026-01-02T00:00:00Z"))
        Hub.applyEvent(frame("old", "2026-01-01T00:00:00Z"))
        assertEquals("new", Hub.lastMessageCache["r1"]?.id)
    }

    @Test
    fun `member frame drops the room's cached members`() {
        Hub.rememberMembers("r1", emptyList())
        Hub.applyEvent(WsEnvelope(type = "room.member_added", roomId = "r1"))
        assertNull(Hub.members("r1"))
    }
}
