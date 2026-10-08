package com.cometchat.uikit.core.hub

import org.junit.Assert.assertEquals
import org.junit.Test

/** The Kit's bubble dispatch keys on message.type: image/video/audio/file, never a MIME string. */
class HubMappersTest {
    private fun typeFor(mime: String): String? =
        HubMappers.message(
            MessageDto(
                id = "01a11984-681a-7c70-a900-288a220db65b",
                roomId = "r",
                authorId = "u",
                body = "x",
                attachments = listOf(AttachmentDto("a", "f", mime, 1)),
                createdAt = "2026-10-07T12:00:00Z"
            ),
            "user", "u"
        ).type

    private val room = RoomDto(
        id = "room-activity", type = "private", name = "g",
        createdAt = "2026-01-01T00:00:00Z", updatedAt = "2026-01-02T00:00:00Z"
    )

    @Test
    fun `conversation date is the newest message time, not the room edit time`() {
        Hub.lastMessageCache[room.id] = MessageDto(
            id = "01a11984-681a-7c70-a900-288a220db65b", roomId = room.id, authorId = "u",
            body = "x", createdAt = "2026-10-07T12:00:00Z"
        )
        try {
            assertEquals(HubMappers.isoToEpoch("2026-10-07T12:00:00Z"), HubMappers.conversation(room).updatedAt)
        } finally {
            Hub.lastMessageCache.remove(room.id)
        }
    }

    @Test
    fun `conversation date falls back to the room time without a cached message`() {
        assertEquals(HubMappers.isoToEpoch(room.updatedAt), HubMappers.conversation(room).updatedAt)
    }

    @Test
    fun `attachment mime maps to the kit message type`() {
        assertEquals("image", typeFor("image/png"))
        assertEquals("video", typeFor("video/mp4"))
        assertEquals("audio", typeFor("audio/ogg"))
        assertEquals("file", typeFor("application/pdf"))
    }
}
