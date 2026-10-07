package com.gochathub.gochathubclient.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.cometchat.uikit.compose.theme.CometChatColorScheme
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.darkColorScheme
import com.cometchat.uikit.compose.theme.lightColorScheme

/**
 * GoChatHub palette (BRAND.md neutral base + webui accent), mapped onto the
 * kit's color tokens. Web parity from ~/projects/gochatwebui/src/style.css:
 * white/gray surfaces, indigo accent, dark mode class-based.
 */
public object GoChatHubColors {
    // brand neutral base
    public val charcoal: Color = Color(0xFF27313A)
    public val slate: Color = Color(0xFF71808E)
    public val lightSlate: Color = Color(0xFFAAB4BC)
    public val darkBackground: Color = Color(0xFF182027)

    // webui accents (Tailwind)
    public val indigo300: Color = Color(0xFFA5B4FC)
    public val indigo400: Color = Color(0xFF818CF8)
    public val indigo50: Color = Color(0xFFEEF2FF)
    public val green500: Color = Color(0xFF22C55E)
    public val red400: Color = Color(0xFFF87171)
    public val gray100: Color = Color(0xFFF3F4F6)
    public val gray200: Color = Color(0xFFE5E7EB)
    public val gray300: Color = Color(0xFFD1D5DB)
    public val gray500: Color = Color(0xFF6B7280)
    public val gray600: Color = Color(0xFF4B5563)
    public val gray700: Color = Color(0xFF374151)
    public val gray800: Color = Color(0xFF1F2937)
    public val gray900: Color = Color(0xFF111827)
    public val white: Color = Color(0xFFFFFFFF)
}

@Composable
public fun hubColorScheme(dark: Boolean): CometChatColorScheme {
    val c = GoChatHubColors
    return if (dark) {
        darkColorScheme(
            primary = c.indigo400,
            neutralColor50 = c.darkBackground, // page bg
            neutralColor100 = c.gray800,       // cards/surfaces
            neutralColor200 = c.gray700,       // inputs
            neutralColor300 = c.gray600,
            neutralColor400 = c.gray600,
            neutralColor500 = c.lightSlate,
            neutralColor600 = c.lightSlate,
            neutralColor700 = c.gray300,
            neutralColor800 = c.gray200,
            neutralColor900 = c.white,
            backgroundColor1 = c.darkBackground,
            backgroundColor2 = c.gray800,
            backgroundColor3 = c.gray700,
            backgroundColor4 = c.charcoal,
            strokeColorDefault = c.gray700,
            strokeColorLight = c.gray700,
            strokeColorDark = c.gray600,
            textColorPrimary = c.white,
            textColorSecondary = c.gray300,
            textColorTertiary = c.lightSlate,
            textColorDisabled = c.gray600,
            textColorHighlight = c.indigo400,
            iconTintPrimary = c.white,
            iconTintSecondary = c.lightSlate,
            iconTintTertiary = c.slate,
            iconTintHighlight = c.indigo400,
            primaryButtonBackgroundColor = c.indigo400,
            primaryButtonTextColor = c.white,
            successColor = c.green500,
            errorColor = c.red400
        )
    } else {
        lightColorScheme(
            primary = c.indigo300,
            neutralColor50 = c.white,
            neutralColor100 = c.gray100,
            neutralColor200 = c.gray200,
            neutralColor300 = c.gray300,
            neutralColor400 = c.gray300,
            neutralColor500 = c.gray500,
            neutralColor600 = c.gray500,
            neutralColor700 = c.gray600,
            neutralColor800 = c.gray700,
            neutralColor900 = c.gray900,
            backgroundColor1 = c.white,
            backgroundColor2 = c.gray100,
            backgroundColor3 = c.gray200,
            backgroundColor4 = c.gray300,
            strokeColorDefault = c.gray200,
            strokeColorLight = c.gray200,
            strokeColorDark = c.gray300,
            textColorPrimary = c.gray900,
            textColorSecondary = c.gray600,
            textColorTertiary = c.gray500,
            textColorDisabled = c.gray300,
            textColorHighlight = c.indigo400,
            iconTintPrimary = c.gray800,
            iconTintSecondary = c.gray500,
            iconTintTertiary = c.slate,
            iconTintHighlight = c.indigo400,
            primaryButtonBackgroundColor = c.indigo300,
            primaryButtonTextColor = c.white,
            successColor = c.green500,
            errorColor = c.red400
        )
    }
}

@Composable
public fun GoChatHubTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val c = GoChatHubColors
    val material = if (dark) androidx.compose.material3.darkColorScheme(
        primary = c.indigo400, onPrimary = c.white,
        secondary = c.slate, onSecondary = c.white,
        background = c.darkBackground, onBackground = c.white,
        surface = c.gray800, onSurface = c.white,
        surfaceVariant = c.gray700, onSurfaceVariant = c.gray300,
        outline = c.gray600, outlineVariant = c.gray700,
        error = c.red400, onError = c.white
    ) else androidx.compose.material3.lightColorScheme(
        primary = c.indigo300, onPrimary = c.white,
        secondary = c.slate, onSecondary = c.white,
        background = c.white, onBackground = c.gray900,
        surface = c.gray100, onSurface = c.gray900,
        surfaceVariant = c.gray200, onSurfaceVariant = c.gray600,
        outline = c.gray300, outlineVariant = c.gray200,
        error = c.red400, onError = c.white
    )
    MaterialTheme(colorScheme = material) {
        // paints the whole window (incl. under system bars) so no screen leaks the light window bg
        androidx.compose.material3.Surface(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            CometChatTheme(colorScheme = hubColorScheme(dark), content = content)
        }
    }
}
