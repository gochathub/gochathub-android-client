package com.cometchat.uikit.core.data.datasource

import com.cometchat.uikit.core.hub.Hub
import com.cometchat.uikit.core.hub.HubEvents
import com.cometchat.chat.models.MessageReceipt
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Helper class that exposes GoChatHub message-receipt events as Kotlin Flows for
 * reactive consumption.
 *
 * Receipts ride `message.receipts_changed` (to the author only, per ADR-009); the
 * aggregate truth is the suspend REST decode in [HubEvents.receiptsChangedOf], so
 * each flow forwards it and only emits when the decode returns a receipt.
 */
public class MessageReceiptEventListener {

    /**
     * Creates a Flow that emits MessageReceipt events for both delivered and read receipts.
     * The registration is live while the flow is collected and closed when collection stops.
     *
     * @return Flow emitting MessageReceipt for delivered and read events
     */
    public fun receiptEvents(): Flow<MessageReceipt> = callbackFlow {
        val job = receiptsChangedJob { trySend(it) }
        awaitClose { job.cancel() }
    }

    /**
     * Creates a Flow that emits only delivered receipt events.
     *
     * @return Flow emitting MessageReceipt for delivered events only
     */
    public fun deliveredEvents(): Flow<MessageReceipt> = callbackFlow {
        val job = receiptsChangedJob { receipt ->
            if (receipt.receiptType == MessageReceipt.RECEIPT_TYPE_DELIVERED ||
                receipt.receiptType == MessageReceipt.RECEIPT_TYPE_DELIVERED_TO_ALL
            ) trySend(receipt)
        }
        awaitClose { job.cancel() }
    }

    /**
     * Creates a Flow that emits only read receipt events.
     *
     * @return Flow emitting MessageReceipt for read events only
     */
    public fun readEvents(): Flow<MessageReceipt> = callbackFlow {
        val job = receiptsChangedJob { receipt ->
            if (receipt.receiptType == MessageReceipt.RECEIPT_TYPE_READ ||
                receipt.receiptType == MessageReceipt.RECEIPT_TYPE_READ_BY_ALL
            ) trySend(receipt)
        }
        awaitClose { job.cancel() }
    }

    /**
     * Shared registration: collect [Hub]'s receipt frames, decode each through
     * [HubEvents.receiptsChangedOf] (suspend — REST truth) and hand the receipt to [emit].
     */
    private fun receiptsChangedJob(emit: (MessageReceipt) -> Unit): Job {
        val scope = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
        )
        return scope.launch {
            Hub.socket.events.collectLatest { env ->
                if (env.type != "message.receipts_changed") return@collectLatest
                HubEvents.receiptsChangedOf(env)?.let(emit)
            }
        }
    }
}