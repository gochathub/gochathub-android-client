package com.gochathub.gochathubclient

import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gochathub.gochathubclient.ui.Accent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Accent swatch set + derivation (docs/PRIMARY_COLOR_SETTINGS.md). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AccentTest {

    @Test
    fun `swatch list matches the contract enum`() {
        assertEquals(15, Accent.swatches.size)
        assertEquals("#4f46e5", Accent.DEFAULT)
        assertTrue(Accent.swatches.contains(Accent.DEFAULT))
        assertTrue(Accent.swatches.all { Regex("^#[0-9a-f]{6}$").matches(it) })
        assertEquals(Accent.swatches.distinct(), Accent.swatches)
    }

    @Test
    fun `default swatch reproduces the indigo palette`() {
        // light primary = the stored hex; dark twin = 30% toward white
        // (#4f46e5 -> #847eed, close to the old fixed indigo400 #818cf8)
        assertEquals(0xFF4F46E5.toInt(), Accent.color(Accent.DEFAULT).toArgb())
        assertEquals(0xFF847EED.toInt(), Accent.dark(Accent.DEFAULT).toArgb())
    }

    @Test
    fun `unparseable hex falls back to the default`() {
        assertEquals(Accent.color(Accent.DEFAULT), Accent.color("#fff"))
        assertEquals(Accent.color(Accent.DEFAULT), Accent.color("ff00cc"))
        assertEquals(Accent.color(Accent.DEFAULT), Accent.color(""))
    }
}