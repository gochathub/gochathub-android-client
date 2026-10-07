package com.cometchat.uikit.compose.presentation.shared.messagebubble.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gochathub.chat.models.CardMessage
import com.cometchat.uikit.compose.R
import com.cometchat.uikit.compose.theme.CometChatTheme
import com.cometchat.uikit.core.constants.UIKitConstants

/**
 * Card messages (category "card") have no server counterpart and the card renderer
 * library is not bundled, so the bubble shows the message text (or a generic label).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
public fun CometChatCardBubble(
    message: CardMessage,
    alignment: UIKitConstants.MessageBubbleAlignment,
    modifier: Modifier = Modifier,
    onCardAction: ((CardMessage, Any) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    Text(
        text = message.text?.ifEmpty { null } ?: stringResource(R.string.cometchat_message_card),
        style = CometChatTheme.typography.bodyRegular,
        color = CometChatTheme.colorScheme.textColorPrimary,
        modifier = modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onLongClick
            )
            .padding(12.dp)
    )
}
