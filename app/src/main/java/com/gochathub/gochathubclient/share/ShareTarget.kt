package com.gochathub.gochathubclient.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import com.cometchat.uikit.core.models.StagedAttachmentInput
import com.cometchat.uikit.compose.presentation.shared.mediaselection.createMediaSelectionResult
import com.cometchat.uikit.compose.presentation.shared.mediaselection.getFileName
import java.io.File

/**
 * A share received through the Android share sheet, prepared for composer
 * prefill: text is available immediately, attachments are already copied out
 * of the sender's [Uri] grant into app cache (`StagedAttachmentInput` needs a
 * real file, mirroring the picker flow).
 */
public data class SharePayload(
    val text: String?,
    val attachments: List<StagedAttachmentInput>,
    val ready: Boolean,
    val failed: List<String>
) {
    public val isEmpty: Boolean
        get() = text.isNullOrEmpty() && attachments.isEmpty()

    /** Banner summary line, e.g. "text + 2 attachments". */
    public fun describe(): String = when {
        isEmpty -> "shared content"
        attachments.isEmpty() -> "text"
        text.isNullOrEmpty() -> attachmentSummary()
        else -> "text + ${attachmentSummary()}"
    }

    private fun attachmentSummary(): String {
        if (attachments.size == 1) return attachments[0].name.takeLast(24)
        return "${attachments.size} attachments"
    }
}

/** Parse the share intent and stage its files. */
public object ShareTarget {

    public data class Parts(val text: String?, val streams: List<Uri>)

    /**
     * Extracts text and stream URIs from ACTION_SEND / ACTION_SEND_MULTIPLE.
     * Returns null when the intent carries no share content at all.
     */
    public fun extract(intent: Intent): Parts? {
        when (intent.action) {
            Intent.ACTION_SEND -> {}
            Intent.ACTION_SEND_MULTIPLE -> {}
            else -> return null
        }
        val streams: List<Uri> = when (intent.action) {
            Intent.ACTION_SEND ->
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?.let { listOf(it) }
                    ?: emptyList()
            else ->
                IntentCompat.getParcelableArrayListExtra(
                    intent, Intent.EXTRA_STREAM, Uri::class.java)
                    ?.filterNotNull()
                    ?: emptyList()
        }
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()?.takeIf { it.isNotEmpty() }
        if (text == null && streams.isEmpty()) return null
        return Parts(text, streams)
    }

    /**
     * Copies each uri into the app cache (the moment the grant can be consumed)
     * and builds a [StagedAttachmentInput] per file. Duplicate display names are
     * uniquified so `copyUriToFile` cannot overwrite an earlier copy.
     *
     * @return successfully staged inputs, and the display names of items whose
     *         copy failed (the uri grant expired or unreadable).
     */
    public fun stageAll(context: Context, streams: List<Uri>): Pair<List<StagedAttachmentInput>, List<String>> {
        val inputs = ArrayList<StagedAttachmentInput>(streams.size)
        val failed = ArrayList<String>()
        val usedNames = HashSet<String>()
        for (uri in streams) {
            var name = getFileName(context, uri)
            val unique = uniquify(name, usedNames)
            usedNames.add(unique)
            val selection = createMediaSelectionResult(context, uri, copyToCache = true)
            var file = selection.file
            if (file == null) {
                failed.add(name)
                continue
            }
            // The copy landed at the original display name (copyUriToFile writes
            // cacheDir/name); move it to the uniquified name so a later same-name
            // copy cannot overwrite it. Same-dir rename, no re-copy.
            if (file.name != unique) {
                val target = File(file.parentFile, unique)
                if (target.exists() || !file.renameTo(target)) {
                    failed.add(name)
                    continue
                }
                file = target
            }
            inputs.add(
                StagedAttachmentInput(
                    file = file,
                    name = unique,
                    size = selection.fileSize.takeIf { it > 0 } ?: file.length(),
                    mimeType = selection.mimeType ?: "application/octet-stream"
                )
            )
        }
        return inputs to failed
    }

    private fun uniquify(name: String, used: Set<String>): String {
        if (name !in used) return name
        val dot = name.indexOfLast { it == '.' }
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var n = 2
        while (("$base-$n$ext") in used) n++
        return "$base-$n$ext"
    }
}