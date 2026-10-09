# Share target — requirements (brainstorm output)

Status: requirements only. Design/architecture is `/sc:design` territory.

## Goal

User shares content from any Android app via the native share sheet to
gochatandroid. App opens on the conversation list with the share parked;
user taps a room; the content prefills that room's composer (text in the
WYSIWYG input, media/files as staged attachments); user edits and sends
manually. App never auto-sends.

## Decisions made

| Question | Decision |
|---|---|
| Content types | Text + images + files |
| Target room | Always show pick list, never auto-open a last room |
| Back out without sending | Ask before discard (dialog) |
| Pick list state | Banner + cancel |

## Flow

1. Any app `ACTION_SEND` / `ACTION_SEND_MULTIPLE` to gochatandroid
   → MainActivity (singleTop): fresh start via `onCreate`, running via
   `onNewIntent` (same pattern as the existing push `room_id` extra).
2. Parse payload:
   - `text/plain` → `EXTRA_TEXT` string prefill.
   - single `EXTRA_STREAM` URI (image/video/audio/application) → staged attachment.
   - `SEND_MULTIPLE` `EXTRA_STREAM` list → multiple staged attachments.
   - `EXTRA_STREAM` + `EXTRA_TEXT` together → attachments + text prefill.
   - Shared image with a screenshot URL case: treat both fields
     ([Android docs](https://developer.android.com/develop/ui/compose/sharing/receive)).
3. Payload parked. HomeScreen shows banner "Send shared content to… tap a
   room" with an X cancel. Share arriving while signed out: hold payload,
   show after login completes.
4. User taps room → ChatScreen opens → composer prefilled:
   - text: plain text into WYSIWYG input at cursor,
   - media/files: existing staged-attachment tray path
     (`StagedAttachmentInput`), uploads gated on send as usual.
5. User edits and sends. No auto-send ever.
6. Back press with payload applied but not sent → dialog "Discard shared
   content?" keep/discard.
7. New share arriving while any payload parked → replace, newest wins.

## Functional requirements

- FR1 Manifest intent-filters on MainActivity: `ACTION_SEND`
  `text/plain`, `ACTION_SEND` `image/*`, `ACTION_SEND` application/file
  types, `ACTION_SEND_MULTIPLE` (singleTop already set).
- FR2 Intent parsing into a share payload (text, uris, mixed), robust to
  null/missing extras.
- FR3 Parked-payload state at MainActivity level; banner + X on
  HomeScreen.
- FR4 Room pick → payload handed to ChatScreen once.
- FR5 Composer prefill: small in-tree Kit addition —
  `CometChatMessageComposer` has no text-injection parameter today
  (`RichTextEditorState.setText` exists); add a prefill path (param or
  controller write).
- FR6 Attachments from shared `content://` URIs flow through the
  existing staged-attachment tray; URI read grant is temporary — consume
  promptly (design decides copy-to-cache vs direct upload).
- FR7 Back-out confirm dialog when prefill staged but unsent.
- FR8 Replace on newer share.

## Non-functional

- No new dependencies; canonical AndroidX pattern
  (`IntentCompat.getParcelableExtra`).
- Never auto-send; no user-visible action happens without user taps.
- F-Droid clean: no cloud services, only system share sheet.
- minSdk 28, compose in-tree Kit; no XML.

## User stories

- S1 Share link from browser → pick room → edits text → send.
- S2 Share image from gallery → pick room → staged tray preview → send.
- S3 Share PDF from files app → same as S2.
- S4 Share album (multiple images) → all staged in tray → send as one
  multi-attachment message (existing `enableMultipleAttachments` path).
- S5 Share image with caption (EXTRA_TEXT + stream) → both staged.
- S6 Cancel from banner → payload dropped, app normal.
- S7 Back out of room with staged prefill → confirm dialog.

## Assumptions (state if wrong)

- Signed-out share: payload held in memory only; process death during
  login drops it (acceptable). Persisting would be a small upgrade path.
- Shared text prefills as plain text (share sheet gives plain strings);
  no markdown sniffing.
- Direct rooms (1-1) count in the pick list same as group rooms.
- `ACTION_PROCESS_TEXT` (context-menu share of selected text) not in
  scope; future option.

## Open items for design

- URI lifecycle: copy-to-cache immediately vs upload straight from grant.
- Composer prefill API shape (`initialText` param vs shared controller).
- Banner placement within existing HomeScreen layout.