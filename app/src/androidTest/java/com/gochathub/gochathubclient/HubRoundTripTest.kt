package com.gochathub.gochathubclient

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cometchat.uikit.core.hub.CreateMessageRequest
import com.cometchat.uikit.core.hub.CreateRoomRequest
import com.cometchat.uikit.core.hub.Hub
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith

/**
 * env-gated round trip (CLAUDE.md testing): runs only when instrumentation
 * args carry a live base URL + credentials, e.g.
 * adb shell am instrument -e BASE_URL=https://... -e USERNAME=... -e PASSWORD=... \
 *   -w com.gochathub.gochathubclient.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class HubRoundTripTest {

    private fun args() = InstrumentationRegistry.getArguments()

    private fun configured(): Boolean =
        !args().getString("BASE_URL").isNullOrEmpty() &&
            !args().getString("USERNAME").isNullOrEmpty()

    @Test
    fun loginRoomsPostSocket() = runBlocking {
        assumeFalse("env args missing — skipped", !configured())
        Hub.init(InstrumentationRegistry.getInstrumentation().targetContext)
        val login = Auth.login(
            args().getString("BASE_URL")!!,
            args().getString("USERNAME")!!,
            args().getString("PASSWORD")!!
        )
        assertTrue(login.isSuccess)

        val rooms = Hub.client.rooms()
        assertNotNull(rooms)

        val room = rooms.firstOrNull() ?: run {
            Hub.client.createRoom(
                CreateRoomRequest(type = "public", name = "instrumented-test")
            )
        }
        val created = Hub.client.createMessage(
            room.id,
            CreateMessageRequest(body = "instrumented hello ${System.currentTimeMillis()}")
        )
        assertTrue(created.id.isNotEmpty())

        val fetched = Hub.client.messages(room.id, 10)
        assertEquals(room.id, fetched.items.first().roomId)
    }
}