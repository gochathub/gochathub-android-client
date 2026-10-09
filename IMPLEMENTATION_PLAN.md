# Implementation Plan

Last updated 2026-10-07. Delete this file once every stage and open issue below is closed.

Decisions: Kit modules copied into this Gradle build; verification on a physical
device (Pixel 8a) with a self-hosted ntfy as the UnifiedPush distributor;
hand-mapped kotlinx.serialization DTOs mirroring server schema names 1:1; app
name "GoChatHub" (`com.gochathub.gochathubclient`); login screen takes the
server URL (blank, no default; cleartext allowed only
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

### Auth after server Turnstile + 2FA (2026-10-08)
- Done: 2FA second step on the login screen; API-token sign-in (skips password,
  Turnstile, 2FA); `captcha_failed` surfaces as a stable code; contract snapshot
  refreshed. Token sign-in verified on the Pixel (rooms, messages, push register
  + validate, sign-out). Unit tests cover the challenge, `/auth/login/2fa` body
  and `captcha_failed`.
- Not verified on device: the 2FA code step itself (needs a 2FA account's
  authenticator; on dev only one test account has one).
- Mobile sign-in QR (2026-10-08): the login screen opens in API-token mode, with
  "Scan QR code" (CameraX + ZXing core; `CAMERA` permission, camera optional) that
  signs in immediately. Server: `POST/GET /users/me/tokens`, `DELETE
  /users/me/tokens/{tokenId}` (gochathub-server `86ee194`, sessions only mint,
  tokens never expire by decision). Web UI: Settings → Account → Mobile sign-in
  creates the QR (`gochathub://login?server=...&token=...`, shown once) and lists
  / revokes tokens (gochathub-webui `6bc381c`, `b883df5`). Verified: web QR decodes
  to the expected payload; scanner opens the camera on the Pixel; parser unit
  tests. **Not verified: a real camera scan of the web UI QR end to end.**
- F-Droid: new runtime deps (CameraX 1.6.2, ZXing core 3.5.4, both Apache-2.0) and
  the `CAMERA` permission; no proprietary services.
- Because tokens never expire, losing a phone means revoking from the web UI
  list; add expiry in `UserService.MintAPIToken` if that policy changes.
- Server finding: `POST /auth/logout` with an API token returns 500
  (`mapError` has no `ErrBadRequest` case); the client wipes locally anyway.
- Not built: Turnstile widget in a WebView (would need a server-hosted widget
  page + site-key endpoint, and a third-party script load from Cloudflare).

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
6. **Push renewal** — server fix landed (gochathub-server `5b73677`: endpoint is globally
   UNIQUE, renewal re-sent the same endpoint); verified on device 2026-10-07 (PATCH 204,
   validate 204) and a push round trip through ntfy with the app in the background.
   Behaviour with the app in the foreground is untested. No distributor picker UI:
   ntfy is preferred by name, else a lone distributor, else push is skipped.
7. **Attachments** — verified on device 2026-10-07 (attach → upload → send → presigned
   download → image bubble). Found and fixed: send NPE'd (composer uploads on attach, the
   datasource tried to upload again), MIME string used as message type. Attachment-only
   send posts the file name as body (server rejects an empty body). The sent message did
   not appear in the live timeline until the chat was reopened (not re-checked after the fixes).
8. **Light theme** viewed 2026-10-07: renders, but contrast is weak (white initials on pale
   avatars, white text on the light own-message bubble, pale "Sign out").
9. **New chat (people search)** and **Settings writes** verified 2026-10-07. Invites and
   contacts remain untested.
10. **Group header shows "0 Member"** — the server Room schema has no member count.
11. **Conversation-list previews and times go stale** after returning from a chat or
    receiving frames; a relaunch refreshes them.
12. **Sign-out against an unreachable server** left the user stuck on the list (not
    re-checked after the sign-out fix, which now navigates explicitly).
13. Fixed 2026-10-07: shim `TextMessage` had no `type` ("Unsupported: message_null"); chat
    screens shared one ViewModel store, so a second room showed the first room's messages.

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
- Release build: R8 + optional signing from the gitignored `keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`; unsigned without it), CI builds it unsigned. Tag-triggered signed GitHub releases: `.github/workflows/release.yml` (see CLAUDE.md, Releases). Verified on device 2026-10-06 (self-signed test key): login, rooms, device register + validate under R8. Chat/message paths under R8 not exercised.
- **F-Droid blocker removed (2026-10-06):** `chat-sdk-android` / `cards-android` dropped. `chatuikit-core/src/main/java/com/gochathub/chat/**` is now an in-tree, API-compatible set of plain data holders, request/builder holders and listener types (no networking; `fetchNext` and `callExtension` return `hub_unsupported`), so the Kit sources are unchanged. Card bubble shows text only. Debug + release build, unit tests pass; release smoke test on device pending.
- F-Droid remaining: v1.0.0 tag, fdroiddata merge request (see docs in the F-Droid brainstorm: F-Droid signs, `UpdateCheckMode: Tags`), public demo server for reviewers; screenshots done (`fastlane/.../phoneScreenshots`); shim renamed to com.gochathub.chat.* (2026-10-07); repos are public.
- CI actions bumped to checkout@v5 / setup-gradle@v5 (Node 20 warning; confirm on next run).
- `HubIds` derives the numeric Kit id from the UUID (ms<<20 | 20-bit hash): by-id
  lookups (pin/delete/react) only work for messages seen this session.

### Other repos
- `~/projects/gochatserver` has an untracked `gochathub-server` binary I built at the
  repo root (`bin/` was missing) and an unrelated modified `.gitignore`.

### Share target (2026-10-09)
- Coded in one pass per `docs/SHARE_TARGET_REQUIREMENTS.md` + `docs/SHARE_TARGET_DESIGN.md`:
  ACTION_SEND/SEND_MULTIPLE filters on MainActivity, `ShareTarget` (app `share`
  package: intent extract + copy-to-cache staging reusing the Kit's
  `createMediaSelectionResult`), parked `sharePayload` state with a banner + X on
  HomeScreen, composer `initialText` (rides the existing `composeText` flow) and
  `initialAttachments` (straight into `stageAttachments`), keep/discard dialog on
  back.
- Robolectric unit tests cover intent extraction (text/uri/mixed/multiple/empty)
  and banner descriptions. Release contract snapshot refreshed (server added
  webhooks, unrelated).
- **Not verified on device** (Pixel not connected): banner flow, prefill of the
  WYSIWYG input, staged-image send, SEND_MULTIPLE, keep/discard dialog, share
  to the app while signed out.

## Dev environment (not in the repo — will not survive a reboot)
- Server: `~/projects/gochatserver/gochathub-server serve` with
  `DATABASE_URL=postgres://chatdev:chatdev@127.0.0.1:5532/chatdev?sslmode=disable`
  `LISTEN_ADDR=0.0.0.0:8090 LOG_LEVEL=debug`; database = docker container `gochat-db`.
  Port 8080 on that machine is an unrelated app — do not use it.
- Test accounts (CLI-created): `gochathub-test` (notification mode `all`) and
  `peer-test`; direct room `01a11403-e41a-77eb-8de3-441557dc1b00`.
- ntfy: a self-hosted instance (anonymous publish to `up…` topics accepted;
  the server has no token setting). Phone needs the ntfy app with that default server.
- Phone: Pixel 8a over wireless debugging (`adb connect <ip:port>`); signed in as
  `gochathub-test`.

## Stage 6: Accent color preference (2026-10-09)

Goal: user-picked accent following the account (phone + web), stored server-side.
Success criteria: server swatch validation + tests green; gradle full check green;
web typecheck/lint green; contract in sync.
Tests: AccentTest (app), preferences_test.go (service), CI contract check.
Spec: docs/PRIMARY_COLOR_SETTINGS.md.
Status: Complete (server + webui + android implemented in this pass;
server change not yet committed — see git status in all three repos).
