package com.cometchat.uikit.core.hub

import com.gochathub.chat.models.TextMessage
import com.gochathub.chat.models.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** The data holders replace the closed SDK models; the Kit relies on clone/equals/contentEquals. */
class ModelShimTest {
    @Test
    fun `clone is independent and equal by key`() {
        val a = User("u1", "Ann")
        val b = a.clone().apply { status = "online" }
        assertNotSame(a, b)
        assertEquals(a, b)
        assertFalse(a.contentEquals(b))
        assertEquals("Ann", b.name)
    }

    @Test
    fun `contentEquals compares every field`() {
        val a = TextMessage("r", "hi", "user").apply { id = 7 }
        val b = a.clone() as TextMessage
        assertTrue(a.contentEquals(b))
        b.text = "bye"
        assertFalse(a.contentEquals(b))
        assertEquals(a, b) // same id
    }
}
