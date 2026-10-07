package com.cometchat.uikit.core.data.datasource

import com.gochathub.chat.core.ReactionsRequest
import com.gochathub.chat.models.BaseMessage
import com.gochathub.chat.exceptions.CometChatException
import com.gochathub.chat.models.MessageReceipt
import com.gochathub.chat.models.Reaction
import com.cometchat.uikit.core.data.datasource.HubImpls
import com.cometchat.uikit.core.hub.HubIds
import com.cometchat.uikit.core.hub.Hub

/**
 * Aggregated reaction counts only (ADR-009 contract); no per-user breakdown
 * exists server-side.
 */
internal class ReactionListDataSourceImpl : ReactionListDataSource {
    override suspend fun fetchReactions(request: ReactionsRequest): Result<List<Reaction>> {
        val uuid = HubIds.toStringId(request.messageId)
            ?: return Result.failure(CometChatException("hub_unknown_id", "Message ${request.messageId} unknown"))
        val counts = Hub.client.reactions(uuid)
        return Result.success(counts.map { dto ->
            Reaction().apply {
                reactionId = ""
                messageId = request.messageId
                reaction = dto.emoji
                uid = ""
            }
        })
    }

    override suspend fun removeReaction(messageId: Long, emoji: String): Result<BaseMessage> =
        try {
            val uuid = needUuid(messageId)
            Hub.client.unreact(uuid, emoji)
            Result.success(HubImpls.refetchMessage(uuid))
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun needUuid(messageId: Long): String =
        HubIds.toStringId(messageId)
            ?: throw CometChatException("hub_unknown_id", "Message $messageId unknown")
}
