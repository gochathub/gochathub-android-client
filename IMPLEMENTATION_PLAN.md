# Implementation Plan

Contract snapshot refreshed 2026-10-06 from `~/projects/gochatserver/api/openapi.yaml`
(gained `/users/me/password`, `GET /rooms` schema, `Room.archived`,
`CreateRoomRequest` direct/group_direct + `members[]`, `Message.author`).

Decisions: Kit modules copied into this Gradle build; verification on physical
device (ntfy distributor); hand-mapped kotlinx.serialization DTOs mirroring
server schema names 1:1; app name/display "GoChatHub" (app_id `com.gochathub.gochathubclient`); login screen
takes server URL (pre-filled debug default).

## Stage 1: Kit import + call-site audit
**Goal**: chatuikit-compose + chatuikit-core modules in the Gradle build, calls modules deleted, app shell builds.
**Success Criteria**: `./gradlew assembleDebug` green; `grep -rn "com.cometchat.chat"` audit recorded (first implementation commit); no CometChat networking on any path.
**Tests**: build passes; audit artifact reviewed.
**Status**: Not Started

## Stage 2: REST client, DTOs, auth
**Goal**: typed DTOs, HTTP client with `Authorization: Bearer`, login flow (`token_request: true`), EncryptedSharedPreferences token store, 401 → login + wipe, error envelope decoding by `code`.
**Tests**: unit — token store round trip, 401 handling, error code mapping.
**Status**: Not Started

## Stage 3: Datasource seam over REST
**Goal**: implement Kit datasource contracts for conversations/rooms, messages (cursor pagination), users/contacts, invites; room list + timelines render; markdown subset rendering; receipts per ADR-009 rendering rules.
**Tests**: unit — mapping, receipt rendering, pagination cursor handling.
**Theme**: mirror webui (`~/projects/gochatwebui`, Tailwind) palette — light: white/gray-50 surfaces, gray-200 borders, black text; dark: gray-800 surfaces, gray-700 inputs, gray-600 borders, white text; accent indigo-300 (light) / indigo-400 (dark); success green-500, danger red-300/400; fonts Open Sans / Fredoka; radii 0.75rem.
**Status**: Not Started

## Stage 4: Realtime
**Goal**: WS client (bearer upgrade), subscribe, ack/read/typing frames, event dispatch to state; reconnect + REST resync on drop.
**Tests**: unit — frame encode/decode, resync ordering (mock transport, frame shapes verified against server source).
**Status**: Not Started

## Stage 5: Push, attachments, settings, device verification
**Goal**: attachment upload/download flow, UnifiedPush (vapid → register → validate → renew, notification modes UI), preferences UI, instrumented tests env-configured.
**Success Criteria**: on-device round trip: login → rooms → post → WS event; push validation when ntfy present.
**Tests**: instrumented per CLAUDE.md.
**Status**: Not Started