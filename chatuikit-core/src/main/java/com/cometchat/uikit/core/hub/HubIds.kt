package com.cometchat.uikit.core.hub

import java.util.concurrent.ConcurrentHashMap

/**
 * Kit models use Long ids and the WS/REST layer uses UUIDv7 strings. Deterministic
 * derivation keeps lookups stable across process restarts:
 * `(uuidMillis << 20) | fnv1a(uuid) & 0xFFFFF` (~61 bits, collision window is
 * same-millisecond UUIDs differing only in 20 hash bits). The real uuid rides in
 * BaseMessage metadata ("hub_id"); [resolve] consults the session cache first,
 * which every fetch populates.
 */
public object HubIds {
    private val forward = ConcurrentHashMap<String, Long>()
    private val reverse = ConcurrentHashMap<Long, String>()

    public fun toLong(uuid: String): Long =
        forward[uuid] ?: derive(uuid).also { remember(uuid, it) }

    public fun toStringId(longId: Long): String? = reverse[longId]

    /** Resolves a numeric id to the server uuid, preferring the message cache. */
    public fun remember(uuid: String, longId: Long) {
        forward[uuid] = longId
        reverse[longId] = uuid
    }

    public fun derive(uuid: String): Long {
        val hex = uuid.replace("-", "")
        val millis = hex.substring(0, 12).toLong(16) and 0xFFFFFFFFFFFFL
        var h = 0x811c9dc5L
        for (i in 12 until hex.length) {
            h = (h xor hex[i].code.toLong()) * 0x01000193L
        }
        return (millis shl 20) or (h and 0xFFFFFL)
    }
}