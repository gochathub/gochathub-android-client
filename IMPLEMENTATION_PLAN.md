# Implementation Plan

Last updated 2026-10-07. Delete this file once every stage and open issue below is closed.

Decisions: Kit modules copied into this Gradle build; verification on a physical
device (Pixel 8a) with a self-hosted ntfy as the UnifiedPush distributor;
hand-mapped kotlinx.serialization DTOs mirroring server schema names 1:1; app
name "GoChatHub" (`com.gochathub.gochathubclient`); login screen takes the
server URL (debug default `http://192.0.2.10:8090`, cleartext allowed only
for LAN dev hosts in `network_security_config.xml`).

## Stage status

| Stage | Status |
| --- | --- |
| 1 Kit import + call-site audit | Complete (`docs/KIT_AUDIT.md`) |
| 2 REST client, DTOs, auth | Complete (hub package, `HubClientTest`) |
| 3 Datasource seam over REST | Complete for the screens below; see open issues |
| 4 Realtime (WebSocket) | Complete — live receive, typing, read receipts verified on device |
| 5 Push, attachments, settings | Push + settings verified on device; attachments **not tested** |

Verified on device (2026-10-06/07): sign-in + session restore, conversation list,
chat with markdown, live receive over WS, send, typing frames (peer sees them),
live read-receipt tick, dark theme on all screens, settings (prefs + notification
modes render), UnifiedPush through the self-hosted ntfy (register → validate →
push → REST fetch → notification), system Back navigation.

## Open issues

### Bugs / gaps in shipped features
1. **Sign-out push cleanup** — coded (`Push.deleteDevice` before logout,
   `Push.unregister` after wipe); **verified on device 2026-10-06**.
2. **Conversation-list preview** — coded (`Hub.lastMessageCache`, one `limit=1`
   fetch per room, kept current by `Hub.applyEvent` on WS frames); verified on device 2026-10-06.
3. **Rate limit** — coded: members fetched only for direct rooms missing from
   cache, dropped on `room.member_*` frames; client retries a 429 once after
   `Retry-After` (cap 10 s). Unit-tested; device: second room-list load made no members/messages calls (cache hit); the 429 path itself was not exercised on device. Cold start still issues each room fetch twice (two concurrent loads).
4. **Header presence always "Offline".** Presence wiring exists
   (`presence.changed`) but the online flip was never exercised.
5. **Typing indicator display** on the phone is untested (outgoing frames verified).
6. **Push renewal** (`PATCH /devices/{id}` returns 500 from the server on app start, 2026-10-06 — server-side bug, no error detail logged), re-register after sign-out/in, and
   behaviour with the app in the foreground are untested. No distributor picker UI:
   ntfy is preferred by name, else a lone distributor, else push is skipped.
7. **Attachments** (upload session → presigned PUT → complete; download via
   presigned URL, refetch on 403) implemented but never exercised.
8. **Light theme** never viewed (only dark was checked on device).
9. **New chat (people search), Invites, contacts, Settings toggle writes** — screens
   exist; only Settings rendering was verified.

### Kit surfaces with no server counterpart (deliberately inert)
Conversation pin; saved messages; mark-as-unread; message report/flag (option
removed); moderation; member actions rendered as timeline action messages;
per-user receipt details (`MessageInformation` returns empty); thread follow toggle
is session-local; `CometChatAIStreamService` is still referenced by the composer
ViewModel's public API (its SDK listeners no-op). Calls/polls/stickers/AI assistant
were deleted.

### Tests / tooling
- Instrumented round trip (`HubRoundTripTest`) was never run (package fixed; compile unchecked, androidTest deps not cached offline).
- No unit tests yet for mappers, receipt shaping (`HubEvents.receiptsChangedOf`)
  or per-request pagination state.
- No release signing / R8 config; no per-ABI or store metadata.
- CI actions bumped to checkout@v5 / setup-gradle@v5 (Node 20 warning; confirm on next run).
- `HubIds` derives the numeric Kit id from the UUID (ms<<20 | 20-bit hash): by-id
  lookups (pin/delete/react) only work for messages seen this session.

### Other repos
- `gochathub-server` README links the client as `gochathub-androidclient`; the repo
  is `gochathub-android-client`.
- `~/projects/gochatserver` has an untracked `gochathub-server` binary I built at the
  repo root (`bin/` was missing) and an unrelated modified `.gitignore`.

## Dev environment (not in the repo — will not survive a reboot)
- Server: `~/projects/gochatserver/gochathub-server serve` with
  `DATABASE_URL=postgres://chatdev:chatdev@127.0.0.1:5532/chatdev?sslmode=disable`
  `LISTEN_ADDR=0.0.0.0:8090 LOG_LEVEL=debug`; database = docker container `gochat-db`.
  Port 8080 on that machine is an unrelated app — do not use it.
- Test accounts (CLI-created): `gochathub-test` (notification mode `all`) and
  `peer-test`; direct room `01a11403-e41a-77eb-8de3-441557dc1b00`.
- ntfy: `https://ntfy.example.com` (anonymous publish to `up…` topics accepted;
  the server has no token setting). Phone needs the ntfy app with that default server.
- Phone: Pixel 8a over wireless debugging (`adb connect <ip:port>`); signed in as
  `gochathub-test`.
