# Kit call-site audit (fork seam)

Executed 2026-10-06 after copying chatuikit-core + chatuikit-compose from
cometchat/cometchat-uikit-android (@ HEAD, clone in ~/projects/uikit-upstream).

## Upstream surface

- ~1200 .kt source files across the two modules (602 core, ~690 compose pre-clean).
- 595 files imported `com.cometchat.chat.*`; distribution: models 1078, core 347, exceptions 208, constants 207.
- Kit architecture fork upstream: ViewModels -> domain interfaces -> data/repository impls -> datasource impls -> SDK statics.

## Replaced networking (datasource seam)

| Datasource impl | GoChatHub surface |
| --- | --- |
| ConversationListDataSourceImpl |
| REST /rooms (+ /rooms/{id}/members for direct-room peers) |
|  MessageListDataSourceImpl |
| REST /rooms/{id}/messages (before= cursor), /messages/{id}, reactions, /rooms/{id}/read |
| UsersDataSourceImpl |
| REST /users/search + /contacts (no user directory on server) |
| GroupsDataSourceImpl |
| REST /rooms filtered to group types; join = invites only |
| GroupMembersDataSourceImpl |
| REST /rooms/{id}/members CRUD |
| MessageComposerDataSourceImpl |
| REST /rooms/{id}/messages + attachment upload-session flow |
| MessageHeaderDataSourceImpl |
| REST /users/{id}, /rooms/{id} |
| MessageInformationDataSourceImpl |
| aggregate receipts only (ADR-009); per-user receipts unsupported |
| ReactionListDataSourceImpl |
| REST /messages/{id}/reactions (aggregate counts) |
| SearchDataSourceImpl |
| REST /rooms (name match) + page filter |

## Entry surgery

- `CometChatUIKit` object rewritten: init wires the kit to `Hub`; session/`getLoggedInUser` come from HubStore/Hub; login/logout/createUser/initFromSettings deleted (server is username+password over REST; accounts CLI-administered — ADR-014).
- `Hub` facade (this repo): HubStore (encrypted token/device), HubClient (REST, error envelope), HubSocket (WS frames + reconnect + resync hook), HubMappers (DTO->SDK models), HubIds (UUIDv7<->Long, deterministic, uuid in BaseMessage.metadata `hub_id`).
- Closed SDK `chat-sdk-android` kept as dependency for model/config types only: zero `CometChat.init` (verified by grep), no cloud host is ever contacted; calls-sdk + calls UI deleted (ADR-012); polls/stickers/AI-assistant/notification-feed deleted (no server surface).

## Deleted

- calls modules/UI: callbuttons, calllogs, incoming/ongoing/outgoing call, CometChatCallActivity, CallManager/CallsUtils, call usecases/repos/datasources/events.
- unmapped features: createpoll, stickerkeyboard (+bubbles), aiassistant bubble & history, notificationfeed, custom-message send.
- upstream test suites (robolectric/roborazzi/dokka/jacoco apparatus); this repo writes its own tests.

## Left in place (still compiling against SDK models)

- SDK model/config types (User, Group, TextMessage, MediaMessage, Requests...) — data holders only.
