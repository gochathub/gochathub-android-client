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

    @Test
    fun `attachment mime maps to the kit message type`() {
        assertEquals("image", typeFor("image/png"))
        assertEquals("video", typeFor("video/mp4"))
        assertEquals("audio", typeFor("audio/ogg"))
        assertEquals("file", typeFor("application/pdf"))
    }
}
