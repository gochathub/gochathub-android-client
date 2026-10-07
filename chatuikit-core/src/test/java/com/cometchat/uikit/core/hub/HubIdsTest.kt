package com.cometchat.uikit.core.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HubIdsTest {
    @Test
    fun `derivation is stable across restarts`() {
        val uuid = "019a1234-5678-7abc-89de-f0123456789a"
        val first = HubIds.derive(uuid)
        val again = HubIds.derive(uuid)
        assertEquals(first, again)
    }

    @Test
    fun `uuid millis dominate the ordering`() {
        val earlier = HubIds.derive("019a0000-0000-7000-8000-000000000000")
        val later = HubIds.derive("019b0000-0000-7000-8000-000000000000")
        assertTrue(earlier < later)
    }

    @Test
    fun `session cache round trips`() {
        val uuid = "019acafe-babe-7000-8000-000000000042"
        val long = HubIds.toLong(uuid)
        assertEquals(uuid, HubIds.toStringId(long))
        assertEquals(long, HubIds.toLong(uuid))
        assertTrue(long > 0)
    }
}