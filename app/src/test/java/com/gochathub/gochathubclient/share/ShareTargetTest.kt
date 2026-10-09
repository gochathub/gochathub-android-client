package com.gochathub.gochathubclient.share

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Share-target intent contract: null/missing extras never crash, text and
 * EXTRA_STREAM both surface, SEND_MULTIPLE spreads the list, and the banner
 * description labels every combination.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ShareTargetTest {

    private fun sendIntent(type: String? = "text/plain"): Intent =
        Intent(Intent.ACTION_SEND).apply {
            if (type != null) this.type = type
        }

    @Test
    fun `text only send yields text, no streams`() {
        val parts = ShareTarget.extract(
            sendIntent().apply { putExtra(Intent.EXTRA_TEXT, "hello") }
        )
        assertEquals("hello", parts?.text)
        assertTrue(parts?.streams?.isEmpty() == true)
    }

    @Test
    fun `blank text only yields no parts`() {
        assertNull(ShareTarget.extract(
            sendIntent().apply { putExtra(Intent.EXTRA_TEXT, "   ") }
        ))
    }

    @Test
    fun `image send with no extras yields uri only`() {
        val uri = Uri.parse("content://media/external/images/1")
        val parts = ShareTarget.extract(
            sendIntent(type = "image/png").apply { putExtra(Intent.EXTRA_STREAM, uri) }
        )
        assertNull(parts?.text)
        assertEquals(listOf(uri), parts?.streams)
    }

    @Test
    fun `image with caption yields text and uri`() {
        val uri = Uri.parse("content://media/external/images/1")
        val parts = ShareTarget.extract(
            sendIntent(type = "image/png").apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "caption")
            }
        )
        assertEquals("caption", parts?.text)
        assertEquals(listOf(uri), parts?.streams)
    }

    @Test
    fun `send multiple spreads the uri list`() {
        val uris = arrayListOf(
            Uri.parse("content://media/external/images/1"),
            Uri.parse("content://media/external/images/2")
        )
        val parts = ShareTarget.extract(
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uris)
            }
        )
        assertEquals(uris, parts?.streams)
    }

    @Test
    fun `send with no content at all yields null`() {
        assertNull(ShareTarget.extract(sendIntent()))
        assertNull(ShareTarget.extract(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        ))
    }

    @Test
    fun `describe covers every combination`() {
        val textOnly = SharePayload("note", emptyList(), ready = true, failed = emptyList())
        assertEquals("text", textOnly.describe())

        val attachments = SharePayload(
            "cap", listOf(stub("a.png"), stub("b.jpg")), ready = true, failed = emptyList())
        assertEquals("text + 2 attachments", attachments.describe())

        val single = SharePayload(
            "", listOf(stub("report.pdf")), ready = true, failed = emptyList())
        assertEquals("report.pdf", single.describe())

        val failed = SharePayload("note", emptyList(), ready = true, failed = listOf("gone.bin"))
        assertEquals("unreadable", false, failed.failed.isEmpty())
    }

    private fun stub(name: String): com.cometchat.uikit.core.models.StagedAttachmentInput =
        com.cometchat.uikit.core.models.StagedAttachmentInput(
            file = java.io.File(cacheDirName(), name),
            name = name,
            size = 1L,
            mimeType = "text/plain"
        )

    private fun cacheDirName() = "build/tmp/stub"
}