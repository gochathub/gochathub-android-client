package com.gochathub.gochathubclient.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.cometchat.uikit.compose.presentation.conversations.style.CometChatConversationsStyle
import com.cometchat.uikit.compose.presentation.messageheader.style.CometChatMessageHeaderStyle
import com.cometchat.uikit.compose.presentation.messagelist.style.CometChatMessageListStyle
import com.cometchat.uikit.compose.presentation.shared.messagebubble.style.CometChatMessageBubbleStyle
import com.cometchat.uikit.compose.theme.CometChatColorScheme
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.compose.theme.blendColors
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

    // light theme: tinted canvas under white rows, charcoal bar, AA-contrast accent
    public val canvas: Color = Color(0xFFEDF0F4)
    public val container: Color = Color(0xFFE3E8EE)
    public val stroke: Color = Color(0xFFD5DBE2)
    public val strokeStrong: Color = Color(0xFFB4BEC9) // card borders
    public val text2: Color = Color(0xFF566372)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun hubTopBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = GoChatHubColors.charcoal,
    titleContentColor = GoChatHubColors.white,
    navigationIconContentColor = GoChatHubColors.white,
    actionIconContentColor = GoChatHubColors.white
)

@Composable
public fun hubColorScheme(dark: Boolean, primaryLight: Color, primaryDark: Color): CometChatColorScheme {
    val c = GoChatHubColors
    return if (dark) {
        darkColorScheme(
            primary = primaryDark,
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
            textColorHighlight = primaryDark,
            iconTintPrimary = c.white,
            iconTintSecondary = c.lightSlate,
            iconTintTertiary = c.slate,
            iconTintHighlight = primaryDark,
            primaryButtonBackgroundColor = primaryDark,
            primaryButtonTextColor = c.white,
            successColor = c.green500,
            errorColor = c.red400
        )
    } else {
        lightColorScheme(
            primary = primaryLight,
            neutralColor50 = c.canvas,
            neutralColor100 = c.white,
            neutralColor200 = c.container,
            neutralColor300 = c.stroke,
            neutralColor400 = c.gray300,
            neutralColor500 = c.text2,
            neutralColor600 = c.text2,
            neutralColor700 = c.gray600,
            neutralColor800 = c.gray700,
            neutralColor900 = c.gray900,
            backgroundColor1 = c.canvas,
            backgroundColor2 = c.white,
            backgroundColor3 = c.container,
            backgroundColor4 = c.stroke,
            strokeColorDefault = c.stroke,
            strokeColorLight = c.stroke,
            strokeColorDark = c.strokeStrong,
            textColorPrimary = c.gray900,
            textColorSecondary = c.gray600,
            textColorTertiary = c.text2,
            textColorDisabled = c.gray300,
            textColorHighlight = primaryLight,
            iconTintPrimary = c.gray800,
            iconTintSecondary = c.text2,
            iconTintTertiary = c.slate,
            iconTintHighlight = primaryLight,
            primaryButtonBackgroundColor = primaryLight,
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
    // server-loaded accent (default indigo) drives light+dark primaries
    val primaryLight = Accent.color(Accent.hex)
    val primaryDark = Accent.dark(Accent.hex)
    val material = if (dark) androidx.compose.material3.darkColorScheme(
        primary = primaryDark, onPrimary = c.white,
        secondary = c.slate, onSecondary = c.white,
        background = c.darkBackground, onBackground = c.white,
        secondaryContainer = c.gray700, onSecondaryContainer = c.white, // selected chips
        surface = c.gray800, onSurface = c.white,
        surfaceVariant = c.gray700, onSurfaceVariant = c.gray300,
        outline = c.gray600, outlineVariant = c.gray700,
        error = c.red400, onError = c.white
    ) else androidx.compose.material3.lightColorScheme(
        primary = primaryLight, onPrimary = c.white,
        secondary = c.slate, onSecondary = c.white,
        background = c.canvas, onBackground = c.gray900,
        surface = c.white, onSurface = c.gray900,
        surfaceVariant = c.container, onSurfaceVariant = c.text2,
        secondaryContainer = blendColors(primaryLight, c.white, 0.88), // selected chips
        surfaceContainerHighest = c.container, // switch off-track
        outline = c.gray300, outlineVariant = c.stroke,
        error = c.red400, onError = c.white
    )
    MaterialTheme(colorScheme = material) {
        // paints the whole window (incl. under system bars) so no screen leaks the light window bg
        androidx.compose.material3.Surface(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box {
                CometChatTheme(colorScheme = hubColorScheme(dark, primaryLight, primaryDark), content = content)
                // light icons stay readable on every screen: charcoal behind the status bar
                Spacer(
                    Modifier.fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(c.charcoal)
                )
            }
        }
    }
}

/** Kit message header on the same charcoal as the app bars. */
@Composable
public fun hubHeaderStyle(): CometChatMessageHeaderStyle = CometChatMessageHeaderStyle.default().copy(
    backgroundColor = GoChatHubColors.charcoal,
    titleTextColor = GoChatHubColors.white,
    subtitleTextColor = GoChatHubColors.lightSlate,
    backIconTint = GoChatHubColors.white,
    menuIconTint = GoChatHubColors.white
)

/** Conversation rows are white cards on the tinted canvas (card shape lives in ConversationListContent). */
@Composable
public fun hubConversationsStyle(): CometChatConversationsStyle = CometChatConversationsStyle.default().let {
    it.copy(
        itemStyle = it.itemStyle.copy(
            backgroundColor = CometChatTheme.colorScheme.backgroundColor2,
            separatorColor = CometChatTheme.colorScheme.strokeColorDark // card border
        )
    )
}

/** Own-message bubble uses the dark accent twin in both modes (indigo400 parity). */
@Composable
public fun hubMessageListStyle(): CometChatMessageListStyle = CometChatMessageListStyle.default().copy(
    outgoingMessageBubbleStyle = CometChatMessageBubbleStyle.outgoing(backgroundColor = Accent.dark(Accent.hex))
)
