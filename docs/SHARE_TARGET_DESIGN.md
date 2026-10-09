# Share target — design (implements docs/SHARE_TARGET_REQUIREMENTS.md)

## Architecture

Two layers only: app-side share plumbing + two new params on the in-tree
Kit composer. No new dependency, no new module, no coordinator class.

```
share sheet (any app)
    │  ACTION_SEND / ACTION_SEND_MULTIPLE, mimeType */ *
    ▼
MainActivity (singleTop) — onCreate(fresh) / onNewIntent(running)
    │  parse: ShareIntentParser                      ← unit tested
    │  state: sharePayload = null → Parsing → Ready/Failed
    │  copy shared URIs → app cache (IO, reuses Kit
    │     createMediaSelectionResult/copyUriToFile)
    ▼
  sharePayload parked; if user was inside a chat,
  pop openRoomId so the pick list shows
    │
HomeScreen (sharePayload)                  ShareTargetBanner
    │  tap room                            "Send shared content
    ├─ X → sharePayload = null             to… tap a room" + [X]
    ▼
ChatScreen (roomId, sharedContent = sharePayload)
    │  LaunchedEffect, once per room entry
    ├─ CometChatMessageComposer(initialText = payload.text)
    └─ CometChatMessageComposer(initialAttachments = payload.inputs)
         └─ viewModel.stageAttachments() — tray tiles, presign→PUT,
            send gated on all DONE (existing behavior)
    ▼
back press from chat while share applied
    → AlertDialog "Discard shared content?"  Keep→pop (banner returns)
                                             Discard→sharePayload = null
```

## Components

### 1. App share plumbing — `app/src/main/java/com/gochathub/gochathubclient/share/`

**`SharePayload.kt`**

```kotlin
data class SharePayload(
    val text: String?,
    val attachments: List<StagedAttachmentInput>?,
    val ready: Boolean,          // false while copy in flight
    val failed: List<String>     // per-item copy errors (display names)
)
```

**`ShareIntentParser.kt`** — pure parsing, no state:

- `parse(intent): String?` — TEXT only, for unit tests.
- `parseStreams(intents): List<Uri>` — EXTRA_STREAM single/array,
  IntentCompat accessors.
- Parser object holds no context; mime resolution by the caller.

**Copy logic (MainActivity private, not its own class)** — per URI:
`createMediaSelectionResult(context, uri, copyToCache = true)` →
`StagedAttachmentInput(file, name, size, mime ?: "application/octet-stream")`;
duplicate display names uniquified with a sequence suffix before copy
(`copyUriToFile` overwrites at `File(cacheDir, name)`). MIME→category
mapping is `defaultAttachmentCategory` (existing). Item whose copy
returns null is reported in `failed`; payload still Ready if text or ≥1
file survived. All fail, nothing else: toast + cancel payload.

### 2. Manifest (`app/src/main/AndroidManifest.xml`)

Two filters on MainActivity — broad mime, parser routes:

```xml
<intent-filter>
    <action android:name="android.intent.action.SEND" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:mimeType="*/*" />
</intent-filter>
<intent-filter>
    <action android:name="android.intent.action.SEND_MULTIPLE" />
    <category android:name="android.intent.category.DEFAULT" />
    <data android:mimeType="image/*" />
</intent-filter>
```

### 3. Kit composer — two public params (in-tree, `chatuikit-compose/.../CometChatMessageComposer.kt`)

```kotlin
initialText: String? = null,
initialAttachments: List<StagedAttachmentInput>? = null,
```

- **Attachments**: `LaunchedEffect(initialAttachments) { vm?.stageAttachments(it) }`
  — uploads start immediately, tray shows tiles; nothing changes on the
  send path (send is already gated "all tiles DONE").
- **Text**: `LaunchedEffect(initialText)` — one-shot sync into the
  segment editor exactly like the composer's mention-insert path:
  resolve the empty composer's first Normal segment controller, run
  `ctrl.onTextChanged(text, text.length, text.length)` to a full set
  (not `insertAtCursor` — empty composer makes that a no-op), then bump
  `formatVersion` + `segmentVersion` to force the TextFieldValue resync.
  Plain text only; spans/markdown-shortcut detection runs naturally.
- Consumed once per ChatScreen entry: ChatScreen passes a `remember(roomId)`
  snapshot so a recomposition does not re-stage.

Composer prefill is safe on a fresh room VM (drafts never survive
re-entry: ChatScreen's per-room `ViewModelStoreOwner` is cleared on
dispose), so "set text" never discards a live draft.

### 4. Screens

**MainActivity**: `sharePayload` state; `parseShareIntent(intent)` runs on
create/new-intent; newer share replaces payload (FR8) and pops
`openRoomId`/`route` to HOME when user sat in a chat. Back-out dialog
(AlertDialog) when popping a ChatScreen that received the payload.

**HomeScreen**: new params `sharedContent: SharePayload?`,
`onShareCancel: () -> Unit`; `ShareTargetBanner` composable in the same
file (Column above `CometChatConversations`): summary line ("1 image",
"PDF + caption"…), spinner while `ready == false`, X cancel.

**ChatScreen**: new param `sharedContent: SharePayload? = null` mapped onto
the composer params; `onShareConsumed` not needed — payload ownership
stays with MainActivity for the keep/discard dialog.

## Data structures on the wire

None. Share target is device-only; payloads never leave the app except
through the normal message send path (`POST /rooms/{id}/messages`, presign
upload) — no API/OpenAPI changes.

## Validation against requirements

| Req | How met |
|---|---|
| FR1 filters | `*/*` SEND + image SEND_MULTIPLE |
| FR2 parsing | ShareIntentParser, pure |
| FR3 parked + banner | MainActivity state, banner composable |
| FR4 pick → handoff | existing onOpenChat path |
| FR5 text prefill | composer `initialText` (segment controller) |
| FR6 staged attachments | composer `initialAttachments` → `stageAttachments` |
| FR7 back-out dialog | MainActivity AlertDialog |
| FR8 replace | newest intent wins |
| NFR no deps | IntentCompat + existing Kit utils only |
| NFR never auto-send | composer untouched send logic |

## Stage plan (stages appended to repo IMPLEMENTATION_PLAN.md)

1. **Parser + manifest + payload state + banner** — unit tests for parser
   (text only / single stream / mixed / multiple / null extras), red→green
   in a new `app/src/test` tree (`application/octet-stream` default mime
   included). Emulator: `adb shell am start -a android.intent.action.SEND
   -t image/png` round trip.
2. **Composer prefill (text)** — Kit param + segment injection; emulator
   verify: share text → pick room → text in WYSIWYG, edit, send.
3. **Attachments + mixed + back-out dialog** — share image (gallery),
   PDF (files app), album (SEND_MULTIPLE), image+caption; verify staged
   tiles upload and single send; verify keep/discard dialog; run full
   `./gradlew assembleDebug assembleRelease :chatuikit-core:testDebugUnitTest`.

## Failure modes

- Copy failure (source app revoked/finished grant): item reported in
  `failed`, others proceed; empty payload drops with toast.
- 3 attempts rule applies at implement time on the segment prefill
  (fallback: `RichTextEditorState.setText` direct — spans empty, plain
  text unaffected).
- No persistence: process death drops the parked share (per requirement
  assumption).