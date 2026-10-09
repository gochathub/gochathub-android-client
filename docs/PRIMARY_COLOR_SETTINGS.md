# Requirements: User-chosen Primary (Accent) Color

Status: requirements + design complete (2026-10-09). Swatch list and
contract are pinned below; ready for one-pass implementation.

## Goal

A user can pick a primary/accent color for the app and have that choice
stored server-side, so it follows the account across the Android client and
the web UI on any device.

## Decisions made (discovery answers)

1. **Private preference, synced.** The color is a per-account client-rendering
   preference. Other users never see or receive it. It rides the existing
   `PATCH /api/v1/users/me/preferences` mechanism (JSONB, merge semantics) —
   no new endpoint, no DB migration, no `render()` change.
2. **Curated swatch list.** The server accepts only values from a fixed,
   documented set of colors (~12–16 exact hexes, shipped in the contract).
   Free-form hex is rejected with `400 {error:{code:"validation"}}`. Pickers
   are swatch grids; contrast stays predictable.
3. **One stored value.** A single swatch (the light-mode hex) is stored; dark
   shades are derived by each client. The web UI already uses one accent hex
   for both modes (`--accent` in light and dark); Android blends its dark
   twin from the stored hex using the kit's existing `getExtendedPrimaryColor`
   / `blendColors` helpers.
4. **No live propagation.** Devices fetch preferences at login/startup; a
   change made on one device appears on other devices at their next session
   (client already reloads fresh prefs there). No WS envelope.

## Functional requirements

- **R1 — Contract:** the preferences object gains a `primary_color` field:
  a string, empty string = unset/default, otherwise an exact hex from the
  allowed swatch set enumerated in the contract (e.g. `"#4f46e5"`).
- **R2 — Server validation:** `PatchPreferences` validates membership in the
  allowed set; unknown, malformed, or non-hex values are rejected with
  `400 {error:{code:"validation"}}` naming the field. Empty string resets to
  the default. (Note: existing `*string` patch pointer cannot distinguish
  "absent" from JSON `null`; the reset path is explicitly `""` — mirrors the
  `"" = clear` convention used for `PATCH /users/me` fields.)
- **R3 — Default:** absent/empty `primary_color` renders the current default
  indigo accent everywhere; fresh accounts see no change. The default light
  hex is `#4f46e5` (web `--accent`, Android `indigo600` / `indigo400` dark).
- **R4 — GET shape:** `GET /users/me/preferences` returns the stored value so
  clients can restore the choice without extra resolution (contract pins
  whether unset is `""` or omitted).
- **R5 — Privacy:** the color appears only on self prefs payloads; no public
  user payload (`render()`, `GET /users/{id}/`, contacts, message senders)
  ever includes it.
- **R6 — Web picker:** Settings → Appearance gains an "Accent color" row with
  the swatch grid (plus Default/Reset). Selecting a swatch applies it
  immediately (set the `--accent` CSS variable, no reload) and saves via the
  PATCH; failures revert the UI and show the standard error surface.
- **R7 — Android picker:** the in-app Settings screen gains the same swatch
  grid; selection applies immediately to `GoChatHubColors`-derived scheme.
- **R8 — Android theme coverage:** the stored hex replaces the hardcoded
  indigo primary in `hubColorScheme()` (light and dark) and in the M3 scheme,
  and the hardcoded `indigo400` own-message bubble color in
  `hubMessageListStyle()`; derived shades cover `indigoSoft`/secondary
  container. Status bar charcoal, neutral palette, and alert colors stay
  fixed (only the accent hue moves).
- **R9 — Startup fetch:** Android loads preferences at sign-in/start (it
  already calls `GET /users/me/preferences`) and applies the stored color
  before first render.
- **R10 — Contract order:** server `api/openapi.yaml` changes first and its
  repo/CI is authoritative; then the web UI regenerates
  (`bun run generate-schema`), and the Android snapshot is refreshed
  (`cp ../gochatserver/api/openapi.yaml api/openapi.yaml`). Existing
  `checkContract` CI keeps guarding the snapshot.
- **R11 — Reset:** both pickers include a reset-to-default action that sends
  `""`.

## Non-functional requirements

- No DB migration (preference joins the existing `users.preferences` JSONB).
- Every swatch passes ≥ 4.5:1 contrast with white label text on light
  surfaces (the set itself is chosen to guarantee this; individual client
  contrast handling is out of scope).
- Palette lives where contract lives: swatch hexes enumerated in
  `api/openapi.yaml` (single source), mapped by clients — no server-side
  rendering or caching of the key.
- No new dependencies (server stdlib; web CSS variable; Android already has
  Color/blending helpers).

## User stories / acceptance criteria

- As a user, I pick a swatch in web Settings → Appearance; the theme changes
  immediately without reload, persists through reload, and still applies
  after cache clear.
- As a user, I pick a swatch in the Android settings screen; bubbles,
  buttons, and highlighting switch immediately and survive app restart.
- As a user on my phone, I see the color chosen in the browser on the next
  app session (and vice versa) — same account.
- Sending an invalid color (`#fff`, `ff00cc`, a hex outside the set) returns
  a 400 and the UIs fall back to showing only valid swatches.
- A fresh account or a reset choice renders exactly the current default
  look.
- The server test suite covers set/reset/invalid-value on the prefs PATCH;
  Android unit test covers hex → scheme mapping; web test covers the
  `--accent` application.

## Known indigo residue (accepted or cleanup-tail decisions)

- Web focus-ring utilities pin `ring-indigo-100/200` (style.css);
  ghost-primary hover pins `indigo-600/400`. Default: leave (subtle),
  revisit if the chosen swatch is far from indigo.

## Open questions (for design stage)

- Exact swatch list: which 12–16 hexes (brand-consistent, AA-safe). Proposed
  anchor: current Tailwind indigo `#4f46e5`, then hue-neighbors from the
  existing palette.
- Android dark-twin derivation: lighten/blended dark shade exact percentages
  (current dark primary is `#818cf8`).
- Contract detail: unset stored as `""` vs key omitted on GET (R4 tiebreak).
- Where Android applies color pre-render: boot flow vs login response field.
---

# Design: Swatch List and Contract (one pass)

## 1. Swatch list

15 swatches, all pass ≥ 4.5:1 contrast against white text (tightest: pink
4.59:1; default indigo 6.3:1). Picker order below; the first entry is the
default. Hexes are lowercase, stored as sent-by-list values.

| # | Name  | Hex       | Source (Tailwind) | Dark-twin check |
|---|-------|-----------|-------------------|-----------------|
| 1 | Indigo | #4f46e5 | indigo-600 (current default) | blend 30% = #847eed (~ old #818cf8) |
| 2 | Violet | #7c3aed | violet-600 | |
| 3 | Purple | #9333ea | purple-600 | |
| 4 | Pink   | #db2777 | pink-600 | |
| 5 | Red    | #dc2626 | red-600 | |
| 6 | Orange | #c2410c | orange-700 step (600 fails AA) | |
| 7 | Amber  | #b45309 | amber-700 step | |
| 8 | Lime   | #4d7c0f | lime-700 step | |
| 9 | Green  | #15803d | green-700 step (600 fails AA) | |
| 10| Teal   | #0f766e | teal-700 step | |
| 11| Cyan   | #0e7490 | cyan-700 step | |
| 12| Sky    | #0369a1 | sky-700 step | |
| 13| Blue   | #2563eb | blue-600 | |
| 14| Slate  | #475569 | slate-600 (monochrome) | |
| 15| Charcoal | #27313a | brand charcoal (GoChatHubColors) | |

Allowed set in the contract = exactly these 15 hex values.

## 2. Contract changes (server `api/openapi.yaml`)

`GET /users/me/preferences` always returns a fully-resolved object
(unchanged pattern: concrete values, no absent keys). `primary_color` is
**required** in the GET response and carries the stored swatch or the
default when unset.

`PATCH /users/me/preferences` request: `primary_color` optional; omitted =
keep; `""` = reset to default; otherwise must be in the swatch set.

```yaml
    Preferences:
      type: object
      required: [last_seen_visible, read_receipts, allow_group_invites,
                 allow_private_messages, spellcheck_enabled, spellcheck_words,
                 primary_color]
      properties:
        # ... existing six fields unchanged ...
        primary_color:
          type: string
          enum: ["#4f46e5", "#7c3aed", "#9333ea", "#db2777", "#dc2626",
                 "#c2410c", "#b45309", "#4d7c0f", "#15803d", "#0f766e",
                 "#0e7490", "#0369a1", "#2563eb", "#475569", "#27313a"]
          description: >-
            Accent color, one of the fixed primary-color swatches. Clients
            derive dark-mode shades and derived palette steps from this hex.
            Empty string is never returned (default resolves to "#4f46e5").

    UpdatePreferencesRequest:
      # partial update, omitted = keep (unchanged)
      properties:
        primary_color:
          type: string
          maxLength: 7
          description: >-
            Empty string resets to the default (#4f46e5). Any other value
            must be exactly one of the supported swatch hexes; others are
            rejected with 400 validation.
```

Errors: reuse `400 {error:{code:"validation"}}`, message names
`primary_color`. No new codes, no other endpoints touched.

`GET shape` decision (was open): resolved default on GET, not
""/omitted — matches the "concrete values" convention comment at the top of
`model.Preferences` and deletes absent-key handling from both clients.

## 3. Server implementation sketch (gochatserver)

`internal/model/model.go` — `Preferences` gains
`PrimaryColor string `omitempty``-style field (lowercase hex), and
`PreferencesPatch` gains `PrimaryColor *string` (pointer nil = keep; empty
string = reset; value = set, case-normalized with `strings.ToLower`).

`internal/service/service.go` / `users.go`:

```go
// PrimaryColorDefault anchors the swatch list (contract, PRIMARY_COLOR doc).
const PrimaryColorDefault = "#4f46e5"
var PrimaryColorSet = map[string]bool{ "#4f46e5": true, /* ...15 total */ }

// applied in MergePreferences:
if patch.PrimaryColor != nil {
    v := strings.ToLower(strings.TrimSpace(*patch.PrimaryColor))
    if v != "" && !PrimaryColorSet[v] {
        // caller maps bad(...) to 400 validation
    }
    p.PrimaryColor = v
}
// decodePrefs defaults:
if p.PrimaryColor == "" { p.PrimaryColor = PrimaryColorDefault }
```

`PatchPreferences` keeps validating inline (`bad("primary_color must be one
of the supported swatches")`) before the store write, like spellcheck_words.

JSONB: no migration. Stored `""` and missing key both resolve to the
default at decode — old rows need nothing.

Tests (`internal/service` or `internal/httpapi`):
- `TestPatchPreferencesPrimaryColor` — set violet; GET returns violet.
- `...Invalid` — `#fff`, `ff00cc`, `#4f46e6` → 400 validation, prefs unchanged.
- `...Reset` — send `""` after setting; GET returns default.
- `...Default` — fresh user; GET returns `#4f46e5`.
- `...CaseInsensitive` — `#7C3AED` stored as `#7c3aed`.

## 4. Web UI (gochatwebui)

- Regenerate schema (`bun run generate-schema` + prettier) —
  `Preferences.primary_color` type lands automatically.
- `src/store/prefs.ts` (existing per-user prefs store): add
  `primaryColor` + save path through the same PATCH it already does for
  spellcheck; boot fetch populates it.
- `src/style.css`: keep `--accent: #4f46e5` as the CSS default; on
  authentication, set the root inline `style.setProperty("--accent", …)`.
  Derived shades without new data:
  - `--accent-hover: color-mix(in srgb, var(--accent) 85%, white)`
  - `--accent-soft: color-mix(in srgb, var(--accent) 12%, white)` (select
    backgrounds/focus rings that currently pin indigo-100/200)
  Update `contained-primary`, `outlined-primary`, `ghost-primary`,
  `ic-btn-*`, focus-ring utilities to consume the vars; literal
  `indigo-*` classes elsewhere stay.
- `AppearanceSettings.vue`: swatch row of 15 buttons (accessible labels,
  aria-checked for selection) + Default/reset button; writes PATCH, applies
  immediately, reverts on error.

## 5. Android (gochatandroid)

- DTOs (`chatuikit-core .../hub/HubDtos.kt`): `Preferences.primaryColor`,
  `UpdatePreferencesDto.primaryColor` (null = omit, "" = reset, value = set).
  `HubClient.preferences()` unchanged — already `GET /users/me/preferences`.
- Startup apply: app-scoped `accentHex` Compose state (set right after login
  and at app start from the boot prefs fetch that already happens for
  notification-mode UI); `GoChatHubTheme` replaces
  `primary = c.indigo600/indigo400` with the parsed hex:
  - light: `Color(accentHex)`
  - dark: `blendColors(base, Color.White, 0.30)` — reproduces the current
    #4f46e5→#818cf8 feel for any hue
  - M3 scheme `primary`/`secondaryContainer`(as 12% blend)
- `hubMessageListStyle()` own-bubble becomes the dark/light derived primary
  instead of hardcoded `indigo400`.
- Settings screen: swatch grid (15 round color buttons + Default) writing
  via `HubClient.updatePreferences`; optimistic local apply, revert on
  error; unit test for hex→scheme derivation (light vs dark, default).
- Refresh snapshot: `cp ../gochatserver/api/openapi.yaml api/openapi.yaml`;
  `checkContract` must pass.

## 6. One-pass implementation order

1. gochatserver: model + validation + tests + openapi → run
   `go test -p 1` (chat_test only).
2. gochatandroid: snapshot cp, `checkContract` green.
3. In parallel: webui (schema regen + prefs/accent binding + picker) and
   android (DTOs + theme + picker + unit test).
4. Full check: `./gradlew assembleDebug assembleRelease
   :chatuikit-core:testDebugUnitTest checkContract`.
5. Verify live on dev stack (gochat.premadev.com): swap on phone, reload web,
   and vice versa.

Rollback: revert server contract commit; JSONB key is inert for old clients
(unknown key merges harmlessly — `PatchPreferences` rebuilds on stored merge;
`decodePrefs` ignores unknowns).
