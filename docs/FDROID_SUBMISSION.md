# F-Droid submission

Status as of 2026-10-07. Sources: F-Droid [Inclusion Policy](https://f-droid.org/docs/Inclusion_Policy/),
[Quick Start Guide](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/),
[Build Metadata Reference](https://f-droid.org/docs/Build_Metadata_Reference/).

## Decisions

- **Signing:** F-Droid builds and signs with its own key. No `Binaries:` / `AllowedAPKSigningKeys`.
  Consequence: users cannot swap between an F-Droid install and a GitHub-release APK without reinstalling.
  Moving to reproducible builds later changes the signing key.
- **Category:** `Internet`. **Anti-features:** none (the server is FOSS and self-hosted; ntfy is FOSS).
- **Submitter:** the maintainer, on GitLab.com (fork of `fdroiddata`, branch named `com.gochathub.gochathubclient`).
- **Demo server:** a public demo instance for reviewers is wanted (hosting location and owner not decided).
- **Release gate:** do not tag `v1.0.0` until the device checks below pass.

## Done

- Repo is public, MIT, no proprietary dependencies; wrapper pinned; no prebuilt `.so`/`.jar`/`.aar`; `dependenciesInfo` off.
- Removed the Cloudsmith Maven repo and dead CometChat/Firebase catalog entries (`b7ae241`).
- fastlane metadata: title, descriptions, icon, changelog `1.txt`, five phone screenshots (`fastlane/metadata/android/en-US/`).
- Server push-renewal fix (gochathub-server `5b73677`), verified on device.
- Device-verified on the release (R8) build: login, rooms, chat, markdown, live receive, send, attachments,
  new chat search, settings writes, push through ntfy, light theme (weak contrast), sign-out.

## Remaining

1. Test invites (and contacts) on device.
2. Decide what to do about the open findings below (none is an F-Droid policy blocker).
3. Tag `v1.0.0` on the commit whose `versionName`/`versionCode` are `1.0.0` / `1`. Pushing the tag also
   publishes the signed GitHub release APK via `.github/workflows/release.yml` (needs the four
   `RELEASE_KEYSTORE*` repository secrets). Device checks were reported complete on 2026-10-08;
   retake the five phone screenshots first, since they predate the light-theme rework.
4. Public demo server for reviewers (credentials go in the MR description).
5. Push both repos.
6. Create `metadata/com.gochathub.gochathubclient.yml` in the `fdroiddata` fork (draft below) and run, from the fdroiddata directory:
   `fdroid readmeta`, `fdroid rewritemeta com.gochathub.gochathubclient`,
   `fdroid checkupdates --allow-dirty com.gochathub.gochathubclient`, `fdroid lint com.gochathub.gochathubclient`,
   `fdroid build com.gochathub.gochathubclient`.
7. Commit `New App: com.gochathub.gochathubclient`, push, open the MR with the "New App" label, answer packager questions.
   The app appears roughly 24–48 hours after the merge.

## Draft metadata

```yaml
Categories:
  - Internet
License: MIT
AuthorName: Brian Tafoya
SourceCode: https://github.com/gochathub/gochathub-android-client
IssueTracker: https://github.com/gochathub/gochathub-android-client/issues

RepoType: git
Repo: https://github.com/gochathub/gochathub-android-client.git

Builds:
  - versionName: 1.0.0
    versionCode: 1
    commit: <full hash of v1.0.0>
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 1.0.0
CurrentVersionCode: 1
```

## Risks to check

- **Multi-module build:** `subdir: app` must resolve `:chatuikit-core` and `:chatuikit-compose` from the repo root. Unverified until `fdroid build` runs.
- **Reviewer questions:** the app is useless without a self-hosted gochathub-server (hence the demo server); the UI is derived from the CometChat UI Kit (MIT, attribution in `chatuikit-core/LICENSE` and the README), brand name is "GoChatHub".
- **Maintenance:** F-Droid expects an actively maintained app.

## Open findings from the 2026-10-07 device pass

Not fixed unless noted. Also tracked in `IMPLEMENTATION_PLAN.md`.

- **Group header shows "0 Member".** The server's Room schema has no member count. Options: hide the count in the header, or add a count server-side first (contract changes land server-side first).
- **Conversation-list previews and timestamps go stale** after returning from a chat or receiving frames; a relaunch refreshes them.
- **Sent attachment missing from the live timeline** until the chat is reopened. Not re-checked after the attachment fixes.
- **Attachment-only messages carry the file name as the body**, because the server rejects an empty body. Decide whether the server should accept attachment-only messages.
- **Light theme contrast:** white initials on pale avatars, white text on the light own-message bubble, pale "Sign out".
- **Sign-out against an unreachable server** left the user on the list. Sign-out now navigates explicitly; the unreachable case is not re-checked.
- **Server:** registering a new device with an endpoint another device of the same user already holds still hits the `push_endpoints.endpoint` unique constraint (500). Unhandled errors log only "internal error", which hid the original renewal bug.
- **Untested:** invites, contacts, push with the app in the foreground, typing indicator display, presence flip, `HubRoundTripTest` (instrumented), R8 paths beyond those exercised above.
- **Inert Kit surfaces** (pin, saved messages, mark unread, report, moderation, per-user receipt details) are listed in `IMPLEMENTATION_PLAN.md`.

## Dev environment notes

The dev stack is `~/projects/gochathub-dev-env` (`gochat.sh`), reached through `https://gochat.example.com`.
Server integration tests truncate their database: use `chat_test`, never `gochat_migrated` (real Rocket.Chat import).
