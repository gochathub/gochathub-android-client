# gochatandroid — Android Client for gochatserver

Private repo. Backend repo: `~/projects/gochatserver` (github.com/gochathub/gochathub-server).

## First task

Read `docs/CLIENT_BRIEF.md` completely before anything else — it is the
authoritative integration brief (fork strategy, auth, UnifiedPush flow,
data mapping, must-not list). Then read `api/openapi.yaml` — the contract
snapshot this repo builds against.

## Contract authority

- The server's `api/openapi.yaml` is the normative contract; `api/openapi.yaml`
  here is the pinned snapshot.
- Before feature work: refresh the snapshot (`cp ../gochatserver/api/openapi.yaml
  api/openapi.yaml` or fetch from GitHub) and diff it — contract changes drive
  your work. Client-side contract changes land server-side first.

## Baseline and fork strategy

[CometChat Android UI Kit](https://github.com/cometchat/cometchat-uikit-android)
(`chatuikit-compose` over `chatuikit-core`). The Kit is UI + ViewModels +
per-screen data sources on top of a closed cloud SDK; the SDK's networking
cannot reach our server. Per the brief:

1. Import the Compose UI + ViewModels + theme.
2. Implement the Kit's `chatuikit-core/.../data/datasource/*` contracts
   against our REST/WS client — that seam is where all integration lives.
3. First implementation commit is a call-site audit: `grep -rn
   "com.cometchat.chat"` — model classes stay where they are plain data
   holders; everything that performs networking/routes through SDK statics
   gets rerouted via the datasource seam; anything unfixable gets mirrored
   in `data/model/` and mapped at the boundary.
4. Remove calls modules entirely (no WebRTC scope on the server).
5. Never initialize the CometChat cloud SDK (no network calls to their
   services may exist in this app).

## Stack constraints

- minSdk 28, Java 17 toolchain, Kotlin (Jetpack Compose; no XML where Compose
  exists in the Kit).
- Prefer official libraries already pinned by the Kit (lifecycle, coroutines)
  and UnifiedPush `android-connector` for push. Do not add
  `embedded_fcm_distributor`, FCM, APNs, or any other push provider.
- No secondary chat-state database that pretends to be authoritative; a cache
  for offline UX must reconcile via REST after reconnect.

## Authentication

- `POST /api/v1/auth/login {username, password, token_request: true}` →
  `{token, user}`; token into EncryptedSharedPreferences/Keystore. It rides
  `Authorization: Bearer` on API calls and the WS upgrade.
- 401 ⇒ drop to login and wipe token state (sessions can be revoked
  server-side).

## UnifiedPush flow (verified live; follow exactly)

Brief §"Realtime"/§"Push" is normative. Digest:

1. `GET /api/v1/push/vapid` → public key for the connector registration.
2. Connector (`android-connector`) registers with the installed distributor
   (ntfy app) → `endpoint` + `p256dh` key + `auth` secret.
3. `POST /api/v1/devices {..., push_registration:{endpoint, public_key,
   auth_secret}}` → `{device_id, validation_required}`.
4. Connector delivers the encrypted validation ping token →
   `POST /api/v1/devices/{id}/validate {token}` → 204.
5. Renewal: `PATCH /api/v1/devices/{id}` with the new registration; re-register
   on app start (distributors expire silent registrations after ~30 days).
6. Push payloads are identifiers only: fetch the message over REST.

Notification modes are server-enforced
(`POST /api/v1/users/me/notifications {mode: all|mentions|directs|never}`,
optional `room_id`): set them in settings UI; never filter pushes locally
beyond mode semantics the server already applied.

## Behavior rules

- Error envelope `{error:{code,message}}`; switch on `code`.
- Cursor pagination (`before=<cursor>`), newest page first.
- RFC 3339 UTC on the wire; render local; peer `timezone` hint for display.
- Receipts (ADR-009): same rendering rules as in the brief (own receipt vs
  sender aggregate; opting out keeps you at "delivered" for others).
- Bodies are markdown from the server subset; no raw HTML outside code spans.

## Must NOT build

No self-registration/password-reset UI, no calls/WebRTC, no FCM/APNs/other
push providers, no client-side thread trees (the server models 1-level
replies — Kit thread UI maps onto it or flattens), no business-rule
duplication.

## Testing

- Unit: mapping, receipt rendering, notification-mode UI state, ping/validate
  orchestration (mock transport).
- Instrumented (env-configured base URL, skipped when unset): login → rooms →
  post → WS event → push validation round trip when the emulator carries an
  ntfy distributor.
- Emulator targets API 28+.
- Generated types: keep the snapshot's shapes checked in under `api/`;
  failing on drift belongs in CI alongside `ktlint`.

## Git

`main` with feature-checkpoint commits; concise bullet messages, no AI
attribution. Author: btafoya@briantafoya.com.