# Hub bridge — kit ViewModels on the GoChatHub connection

## What exists already

`com.cometchat.uikit.core.hub` (chatuikit-core):

- `Hub` — facade. `Hub.client` (HubClient, REST, suspend, throws HubApiException),
  `Hub.socket` (HubSocket: `events: SharedFlow<WsEnvelope>`, `state`,
  `subscribe/unsubscribe/read/ack/typingStarted/typingStopped`, auto-reconnect),
  `Hub.store` (encrypted token/device/baseUrl), `Hub.me: UserDto?`.
  Caches: `Hub.roomsCache` (`rememberRoom/roomById`), member caches
  (`rememberMembers/memberPeer/allCachedUsers`), `Hub.presence(u)`,
  `Hub.receiverOf(room) = (receiverType, receiverUid)`,
  `Hub.hydrateDirectMembers(rooms)`, `Hub.rememberPresenceFromEvent`.
- `HubMappers` — DTO→SDK models: `user/group/groupMemberDto/conversation/
  message(dto, receiverType, receiverUid)/attachment`, `toUuid(msg)`,
  `META_ID/META_ROOM_ID/META_REPLY_TO`, `isoToEpoch/epochToIso`.
- `HubIds` — UUIDv7↔Long: `toLong(uuid)`, `toStringId(long)` (session cache
  populated by every fetch/message event; `derive()` is the fallback).
- `HubEvents` — envelope decode helpers:
  `messageOf(env): BaseMessage?`, `messageDtoOf(env)`, `messageIdOf(env)`,
  `userIdOf(env)`, `emojiOf(env)`, `presenceStateOf(env)`,
  `receiptsChangedOf(env): suspend MessageReceipt?` (REST truth),
  `typingIndicatorOf(env): TypingIndicator?`.
- `HubImpls` (internal, package `...core.data.datasource`): `uuidOf(msg)`,
  `roomIdOf(msg)`, `delivered(msg)`, `refetchMessage(uuid)`,
  `roomForReceiver(receiverUid, receiverType): room id
   (creates the direct room on first DM)`, `unsupported(what)`.

Server WS events (envelope `{type,id,timestamp,room_id,data}`):
`connected, message.created|updated|deleted|receipts_changed,
message.reaction_added|removed, room.member_added|removed,
room.pinned_changed, room.read_state_changed, typing.started|stopped,
presence.changed {user_id,state}, invite.created|accepted|revoked,
contact.added|removed|updated, error`.
- message payloads: `data.message` (MessageDto JSON, `author` hydrated).
- reactions: `{message_id, emoji, count, user_id}`.
- receipts_changed: `{message_id,user_id}` → to author only (own delivered/read state).
- member_added/removed: `{user_id}`; pinned_changed: `{pinned_message_id|null}`;
  read_state_changed: `{user_id, message_id}`; invite.*: `{invite_id,…}`;
  presence: `{user_id, state}`.

## The rule

ViewModels must never call closed-SDK networking (`CometChat.*` statics) —
replace each SDK listener registration (`CometChat.addMessageListener`,
`addUserListener`, `addGroupListener`, `addConnectionListener`,
`addConversationListener`, `CometChat.*Requests`, `CometChat.addReaction`,
`CometChat.markAs*`, etc.) with:

1. state fetches through `Hub.client` + `HubMappers` (or the datasource seam,
   which is already hub-backed), and
2. realtime via `Hub.socket.events` collected in `viewModelScope.launch`,
   mapping envelopes with `HubEvents`, calling the SAME handler methods the
   SDK listener callbacks called.

Match kit receiver semantics: message.receiverUid for a direct room is the
peer user id (not the room id); `conversationId` is the server room id.
Message ids: `HubIds.toLong/toStringId`; always stamp/read `hub_id` metadata.