package com.cometchat.uikit.compose.presentation.messagecomposer.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cometchat.uikit.compose.R
import com.cometchat.uikit.compose.presentation.messagecomposer.style.CometChatMessageComposerStyle

/**
 * Default auxiliary button composable for the message composer.
 * Contains buttons in order: Rich Text Toggle, AI, Voice Recording
 * (matching chatuikit-kotlin layout order)
 *
 * Uses 40dp touch targets with 24dp icons (8dp padding) to match v5 design specifications.
 *
 * @param modifier Modifier for the button row
 * @param hideRichTextToggle Whether to hide the rich text toggle button
 * @param hideAIButton Whether to hide the AI button
 * @param hideVoiceRecordingButton Whether to hide the voice recording button
 * @param isRichTextToolbarExpanded Whether the rich text toolbar is currently expanded
 * @param style Style configuration for the buttons
 * @param onRichTextToggleClick Callback when the rich text toggle button is clicked
 * @param onAIClick Callback when the AI button is clicked
 * @param onVoiceRecordClick Callback when the voice recording button is clicked
 */
@Composable
public fun DefaultAuxiliaryButton(
    modifier: Modifier = Modifier,
    hideRichTextToggle: Boolean = true,
    hideAIButton: Boolean = true,
    hideVoiceRecordingButton: Boolean = true,
    isRichTextToolbarExpanded: Boolean = false,
    style: CometChatMessageComposerStyle = CometChatMessageComposerStyle.default(),
    onRichTextToggleClick: () -> Unit = {},
    onAIClick: () -> Unit = {},
    onVoiceRecordClick: () -> Unit = {}
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Rich Text Toggle button (first, matching chatuikit-kotlin order)
        if (!hideRichTextToggle) {
            val cdHoist4 = stringResource(R.string.cometchat_a11y_format_text)
            IconButton(
                onClick = onRichTextToggleClick,
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = cdHoist4 }
            ) {
                style.richTextToggleIcon?.let { icon ->
                    Icon(
                        painter = icon,
                        contentDescription = if (isRichTextToolbarExpanded) stringResource(R.string.cometchat_a11y_hide_formatting_options) else stringResource(R.string.cometchat_a11y_show_formatting_options),
                        tint = if (isRichTextToolbarExpanded) style.richTextToggleIconActiveTint else style.richTextToggleIconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 2. AI button
        if (!hideAIButton) {
            val cdHoist2 = stringResource(R.string.cometchat_a11y_ai_assistant)
            IconButton(
                onClick = onAIClick,
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = cdHoist2 }
            ) {
                style.aiIcon?.let { icon ->
                    Icon(
                        painter = icon,
                        contentDescription = stringResource(R.string.cometchat_a11y_open_ai_options),
                        tint = style.aiIconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 3. Voice Recording button (last, matching chatuikit-kotlin order)
        if (!hideVoiceRecordingButton) {
            val cdHoist1 = stringResource(R.string.cometchat_a11y_voice_recording)
            IconButton(
                onClick = onVoiceRecordClick,
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = cdHoist1 }
            ) {
                style.voiceRecordingIcon?.let { icon ->
                    Icon(
                        painter = icon,
                        contentDescription = stringResource(R.string.cometchat_a11y_record_voice_message),
                        tint = style.voiceRecordingIconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
