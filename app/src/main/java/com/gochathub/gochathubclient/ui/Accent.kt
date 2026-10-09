package com.gochathub.gochathubclient.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.cometchat.uikit.compose.theme.blendColors
import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.UpdatePreferencesDto

/**
 * Accent color, loaded from the server preference (one fixed swatch hex;
 * dark shades derive). Set list mirrors the contract's `primary_color`
 * enum. `hex` is compose-observed so a change re-themes live.
 */
public object Accent {
    public const val DEFAULT: String = "#4f46e5"

    public val swatches: List<String> = listOf(
        "#4f46e5", "#7c3aed", "#9333ea", "#db2777", "#dc2626", "#c2410c",
        "#b45309", "#4d7c0f", "#15803d", "#0f766e", "#0e7490", "#0369a1",
        "#2563eb", "#475569", "#27313a"
    )

    public var hex: String by mutableStateOf(DEFAULT)

    /** Parse #rrggbb (case-insensitive); unparseable values fall back to [DEFAULT]. */
    public fun color(hex: String): Color {
        val rgb = hex.takeIf { it.length == 7 && it[0] == '#' }
            ?.substring(1)?.lowercase()?.toLongOrNull(16)
        return if (rgb != null) Color((0xFF000000L or rgb).toInt()) else color(DEFAULT)
    }

    /** Primary in light mode; the dark twin blends 30% toward white. */
    public fun dark(hex: String): Color = blendColors(color(hex), Color.White, 0.30)

    /** Pull the preference into [hex] (call after auth; errors keep default). */
    public suspend fun load() {
        try {
            hex = Hub.client.preferences().primaryColor.ifEmpty { DEFAULT }
        } catch (_: Exception) { /* default until next session */ }
    }

    /** Save a swatch optimistically (revert to prior value on failure). */
    public suspend fun set(hex: String) {
        val previous = this.hex
        this.hex = hex
        try {
            this.hex = Hub.client.updatePreferences(UpdatePreferencesDto(primaryColor = hex)).primaryColor
        } catch (_: Exception) {
            this.hex = previous
        }
    }
}