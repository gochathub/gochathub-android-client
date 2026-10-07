package com.cometchat.uikit.core.data.datasource

import android.util.Base64
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.CustomMessage
import com.cometchat.chat.models.MediaMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.uikit.core.data.datasource.HubImpls.unsupported
import com.cometchat.uikit.core.hub.AttachmentDto
import com.cometchat.uikit.core.hub.CreateMessageRequest
import com.cometchat.uikit.core.hub.CreateRoomRequest
import com.cometchat.uikit.core.hub.CreateUploadRequest
import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.HubIds
import com.cometchat.uikit.core.hub.HubMappers
import java.security.MessageDigest
import java.util.UUID

/**
 * GoChatHub message composer datasource. Messages post to a server room; a
 * first DM resolves the direct room by peer (server dedupes per peer pair).
 */
internal class MessageComposerDataSourceImpl : MessageComposerDataSource {

    override suspend fun sendTextMessage(message: TextMessage): TextMessage {
        val roomId = HubImpls.roomForReceiver(message.receiverUid, message.receiverType)
        val created = Hub.client.createMessage(
            roomId,
            CreateMessageRequest(
                body = message.text,
                replyToMessageId = HubMappers.toUuid(message)?.let {
                    if (message.parentMessageId > 0) HubIds.toStringId(message.parentMessageId) else null
                }
            )
        )
        return HubMappers.message(created, message.receiverType, message.receiverUid) as TextMessage
    }

    override suspend fun sendMediaMessage(message: MediaMessage): MediaMessage {
        val roomId = HubImpls.roomForReceiver(message.receiverUid, message.receiverType)
        val files = if (message.file != null) listOf(message.file) else message.files
        val ids = files.map { file ->
            val mime = guessMime(file.name, message.type)
            val bytes = file.readBytes()
            val session = Hub.client.createUpload(
                CreateUploadRequest(
                    filename = file.name,
                    mimeType = mime,
                    sizeBytes = bytes.size.toLong(),
                    sha256 = MessageDigest.getInstance("SHA-256").digest(bytes)
                        .joinToString("") { "%02x".format(it) }
                )
            )
            Hub.client.uploadToPresigned(session.uploadUrl, bytes, mime)
            Hub.client.completeUpload(session.attachment.id)
            session.attachment.id
        }
        val created = Hub.client.createMessage(
            roomId,
            CreateMessageRequest(body = message.caption, attachmentIds = ids)
        )
        return HubMappers.message(created, message.receiverType, message.receiverUid) as MediaMessage
    }

    override suspend fun sendCustomMessage(message: CustomMessage): CustomMessage {
        unsupported("custom messages")
    }

    override suspend fun editMessage(message: BaseMessage): BaseMessage {
        val uuid = HubImpls.uuidOf(message)
        val body = when (message) {
            is TextMessage -> message.text
            is MediaMessage -> message.caption
            else -> HubImpls.unsupported("editing ${message.category}")
        }
        val updated = Hub.client.updateMessage(uuid, com.cometchat.uikit.core.hub.UpdateMessageDto(body))
        return HubImpls.refetchMessage(updated.id)
    }

    private fun guessMime(name: String, fallback: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return mapOf(
            "png" to "image/png", "jpg" to "image/jpeg", "jpeg" to "image/jpeg",
            "gif" to "image/gif", "webp" to "image/webp", "svg" to "image/svg+xml",
            "mp4" to "video/mp4", "webm" to "video/webm",
            "mp3" to "audio/mpeg", "ogg" to "audio/ogg", "wav" to "audio/wav",
            "pdf" to "application/pdf", "txt" to "text/plain", "zip" to "application/zip"
        )[ext]
            ?: when (fallback) {
                com.cometchat.chat.constants.CometChatConstants.MESSAGE_TYPE_IMAGE -> "image/jpeg"
                com.cometchat.chat.constants.CometChatConstants.MESSAGE_TYPE_VIDEO -> "video/mp4"
                com.cometchat.chat.constants.CometChatConstants.MESSAGE_TYPE_AUDIO -> "audio/mpeg"
                else -> "application/octet-stream"
            }
    }
}
