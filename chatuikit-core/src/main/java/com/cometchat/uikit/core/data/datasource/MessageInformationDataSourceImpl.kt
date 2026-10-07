package com.cometchat.uikit.core.data.datasource

import com.gochathub.chat.models.MessageReceipt
import com.gochathub.chat.models.User

/**
 * Server exposes only aggregate receipts (ADR-009); per-user receipt details
 * are not available over the contract.
 */
internal class MessageInformationDataSourceImpl : MessageInformationDataSource {
    override suspend fun getMessageReceipts(messageId: Long): Result<List<MessageReceipt>> =
        Result.success(emptyList())
}
