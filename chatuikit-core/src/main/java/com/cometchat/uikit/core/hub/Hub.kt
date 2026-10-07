package com.cometchat.uikit.core.hub

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * GoChatHub client facade: one store, client, socket per process. Kit
 * datasources and the entry object delegate here. Built once at app start
 * with the application context.
 */
public object Hub {
    private var store: HubStore? = null
    private var _client: HubClient? = null
    private var _socket: HubSocket? = null
    private lateinit var appContext: android.content.Context

    /** room id → RoomDto, refreshed on every rooms fetch / room event. */
    public val roomsCache = ConcurrentHashMap<String, RoomDto>()

    public fun rememberRoom(room: RoomDto) {
        roomsCache[room.id] = room
    }

    public fun roomById(roomId: String): RoomDto? = roomsCache[roomId]

    /** Receiver (type, receiverUid) context for a room per kit semantics. */
    public fun receiverOf(room: RoomDto?): Pair<String, String> {
        if (room == null) return ("group" to "")
        if (room.type == "direct") {
            val peer = memberPeer(room.id)
            return ("user" to (peer?.id ?: room.id))
        }
        return ("group" to room.id)
    }

    /** Cached room→members for direct-room peer resolution and presence. */
    private val membersCache = ConcurrentHashMap<String, List<UserDto>>()

    /** user id → "online"/"offline", fed by WS presence.changed. */
    private val presenceCache = ConcurrentHashMap<String, String>()

    public fun init(context: android.content.Context): Hub {
        appContext = context.applicationContext
        if (store == null) {
            store = HubStore(appContext)
            _client = HubClient(store!!)
            _socket = HubSocket(store!!)
        }
        return this
    }

    public val ctx: android.content.Context
        get() = appContext

    public val client: HubClient get() = requireNotNull(_client)
    public val socket: HubSocket get() = requireNotNull(_socket)

    public fun meId(): String = _me?.id.orEmpty()

    private var _me: UserDto? = null
    public var me: UserDto?
        get() = _me
        set(value) { _me = value }

    public fun wipe() {
        store?.wipe()
        _socket?.close()
        _me = null
        membersCache.clear()
        presenceCache.clear()
    }

    /** Presence stamp for a user id ("" = unknown). */
    public fun presence(userId: String): String = presenceCache[userId].orEmpty()

    public fun setPresence(userId: String, state: String) {
        presenceCache[userId] = state
    }

    /** Cached member list for a room (null when unknown). */
    public fun members(roomId: String): List<UserDto>? = membersCache[roomId]

    public fun rememberMembers(roomId: String, members: List<UserDto>) {
        membersCache[roomId] = members
    }

    /** All users seen in member caches (presence/lookup only). */
    public fun allCachedUsers(): List<UserDto> =
        membersCache.values.flatten().distinctBy { it.id }

    /** The other member of a direct room, from cache (fetch on miss). */
    public fun memberPeer(roomId: String): UserDto? =
        membersCache[roomId]?.firstOrNull { it.id != meId() }

    public fun rememberPresenceFromEvent(envelope: WsEnvelope) {
        val userId = (envelope.data["user_id"] as? kotlinx.serialization.json.JsonPrimitive)?.content
            ?: return
        val state = (envelope.data["state"] as? kotlinx.serialization.json.JsonPrimitive)?.content
            ?: return
        setPresence(userId, state)
    }

    /** Fetches members for direct rooms missing from cache (webui parity). */
    public fun hydrateDirectMembers(rooms: List<RoomDto>) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        for (room in rooms) {
            rememberRoom(room)
            if (room.type != "direct") continue
            if (membersCache.containsKey(room.id)) continue
            scope.launch {
                try {
                    rememberMembers(room.id, client.roomMembers(room.id))
                } catch (_: Exception) { /* resync covers it */ }
            }
        }
    }
}