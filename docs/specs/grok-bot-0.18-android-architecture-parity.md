# Grok Bot 0.18 → Fabushi Android Architecture & Behavior Parity — Specification

Status: active  
Owner: Fabushi Android  
Last updated: 2026-09-22  
Related issue/task/PR: user-requested Android architecture parity migration; implementation PRs TBD

## 1. Context / problem

Fabushi Android currently provides a native Android application under `mobile/android`, with Jetpack Compose UI, Android ViewModels, Android-specific platform integrations, and a JNI-backed `MahayanaHost`. Important product/runtime responsibilities are currently distributed across large Android surface files such as `MainActivity.kt`, `GrokMobileShellAndroid.kt`, `FabushiScreen.kt`, `MobileBotViewModel.kt`, `MessagingViewModel.kt`, `MarketplaceViewModel.kt`, `FabushiRemoteDeviceGateway.kt`, `FabushiAppAgentSurface.kt`, and `core/MahayanaHost.kt`.

The requested target is not merely a Grok-like skin. The Android product must adopt the same architectural separation and externally observable agent behavior represented by the pinned Grok Bot 0.18 reconstructed reference, while using Android-native platform primitives and the canonical shared Mahayana/Rust core where appropriate.

Reference baselines for this Spec:

- Grok reference repository: `b-nnett/grok-bot-0.18-reconstructed`
- Grok pinned reference commit: `a9f633e09d49a85829b8236331b9e21f7e612634`
- Fabushi Android repository: `bhrumom/fabushi-android`
- Fabushi Android discovery baseline: `59f6fc8885ce1cb8d1ad4fc5d4ab36f690fb99a2`

The Grok reference explicitly states that it is an unofficial reconstruction and that no upstream source-code license is asserted or granted. Therefore this project uses the reference as an architecture, protocol, behavior, and evidence baseline. It does not assume permission to verbatim-copy upstream or reconstructed implementation text.

## 2. Goal

Rebuild Fabushi Android so that the complete Android product is architecturally isomorphic to the relevant Grok Bot 0.18 runtime model:

```
Android UI / renderer
        │
        ▼
thin Android platform bridge
        │
        ▼
Mahayana Coordinator
        │
        ▼
Mahayana Host
        │
        ├── MCP / connectors / tools
        ├── inference / agent execution
        ├── transcript / workflow / automation
        └── Runner / local-exec / remote-exec
```

The migration must:

1. enumerate every relevant module and source file in Grok `source/**` and `frontend/**`;
2. assign every item an Android/Mahayana disposition;
3. implement the same responsibility or behavior where it is relevant to Android;
4. preserve strict Coordinator / Host / Runner / platform-bridge boundaries;
5. make the Android UI consume coordinator state and commands instead of owning agent orchestration;
6. remove superseded legacy orchestration after parity is proven;
7. verify behavior from a fresh Android process, after process death/recreation, and through release packaging.

The end state must feel like the same agent product adapted to Android rather than a separate mobile implementation with a Grok-inspired shell.

## 3. Non-goals / out of scope

- Do not embed Electron in Android.
- Do not add Node.js merely to preserve Grok implementation language.
- Do not duplicate shared Mahayana/Rust runtime code in this repository when `bhrumom/fabushi-platform-core` is the canonical owner.
- Do not preserve desktop-only concepts literally when Android has no corresponding system primitive; implement the equivalent semantic behavior and record the mapping.
- Do not claim pixel-perfect desktop layout on a phone-sized viewport. Functional, state, interaction, typography, animation intent, and information-architecture parity are required with Android-responsive layout.
- Do not copy reconstructed source text where rights are unclear. Reimplement behavior/contracts from inspected evidence and preserve provenance.
- Do not keep two production coordinators, two canonical transcript truths, two auth truths, or two agent execution paths after cutover.
- Do not use a WebView as a shortcut for the primary native Android shell.

## 4. Requirements

### R1 — Mandatory complete module inventory

Before product implementation begins, generate and maintain a parity ledger for every file under the pinned Grok reference:

- `source/**`
- `frontend/**`

Each ledger row must contain:

- Grok path;
- Grok role/responsibility;
- evidence anchor or reason for role classification;
- Android target path or shared-core target;
- target language;
- owner repository;
- parity class: `native-equivalent`, `shared-core`, `platform-adapted`, `not-applicable`;
- implementation status;
- tests/evidence;
- removal/replacement of any legacy Fabushi path.

No Grok module may silently disappear. `not-applicable` requires a written Android-specific reason and reviewer acceptance.

### R2 — Grok architectural boundary parity

The target architecture must preserve these logical Grok boundaries even when implementation languages differ:

- renderer/UI;
- trusted platform bridge;
- app/platform lifecycle owner;
- coordinator;
- host;
- runner/local execution;
- remote/box execution;
- shared protocol/contracts;
- MCP/connectors;
- auth/OAuth/WebAuthn;
- persistence/telemetry/observability.

A layer may call the next defined layer through a contract, but must not bypass ownership boundaries for convenience.

### R3 — Mahayana Coordinator

The Grok `source/node-agent-coordinator/**` responsibility must have a first-class Mahayana Coordinator implementation. It must own at minimum:

- UI/renderer port lifecycle;
- request/reply correlation;
- event fan-out;
- streaming turn activity;
- cancellation;
- reconnect and resync;
- transcript routing;
- client-side tool relay;
- gateway routing;
- inference routing;
- local-exec routing;
- routed MCP bridge;
- OAuth forwarding;
- WebAuthn/passkey forwarding where supported;
- telemetry lineage;
- Host supervision;
- crash settlement and deterministic terminal states.

The Android UI and ViewModels must not independently reproduce coordinator behavior.

### R4 — Mahayana Host

The Grok `source/host/**` responsibility must exist behind the Coordinator and own agent-domain execution, including the Android-relevant equivalents of:

- agents and agent isolation;
- automations;
- box/remote execution bindings;
- cloud agents where supported;
- connectors;
- extensions;
- groups;
- gateway protocol/server API;
- event bus;
- initial transcript load;
- host lock/single-owner semantics;
- host paths and durable-file policy;
- roster bookkeeping;
- secrets abstraction;
- local exec;
- MCP auth;
- runner composition;
- storage;
- transcript mirror and mutation events;
- workflows;
- diagnostics and crash guards.

The Host must not own Compose UI or Android Activity navigation.

### R5 — Runner / local-exec / remote-exec

The Grok `source/local-exec-daemon/**`, `source/box-exec-daemon/**`, Host runner modules, and local-exec contracts must map to an explicit Runner architecture.

Android implementation may use Rust, JNI, Android Service/Foreground Service, WorkManager, or remote execution as appropriate, but responsibilities must stay explicit:

- execution request validation;
- lifecycle/identity;
- permission checks;
- cancellation;
- output streaming;
- timeout;
- crash/error normalization;
- capability advertisement;
- remote-device execution;
- sandbox/remote-box routing where local Android execution is not appropriate.

Arbitrary shell/reflection/credential access must not be introduced merely for parity.

### R6 — Thin Android platform bridge

The Grok `source/electron-preload/**` boundary must map to a narrow typed Android bridge. It must expose only reviewed platform capabilities and coordinator APIs.

The bridge must replace free-form `JSONObject` call sites at UI boundaries with typed Kotlin/Rust contracts over time. Raw JSON may remain only at explicitly defined wire/protocol boundaries.

The bridge must cover Android equivalents of:

- coordinator port bridge;
- main RPC runtime;
- VNC/remote-computer liveness and visibility;
- clipboard transfer where allowed;
- WebView bridge where Mini Apps require it;
- passkey/WebAuthn mediation;
- debug/dev controls;
- RPC edge/runtime failure settlement.

### R7 — Android platform lifecycle layer

The Grok `source/electron-main/**` responsibility must be decomposed into Android platform modules rather than accumulated in `MainActivity`.

`MainActivity` final responsibility is limited to Activity lifecycle, Compose root hosting, intent/deep-link forwarding, permission/result forwarding, and platform window/system UI concerns.

The Android platform layer must provide equivalents for Grok main-process modules including:

| Grok electron-main area | Android target responsibility |
| --- | --- |
| `account/**` | account/session platform adapter |
| `adapters/**` | Android capability adapters |
| `application-menu.ts` | app navigation/action menu semantics |
| `attachments/**` | ContentResolver / picker / URI grant adapters |
| `auth/**` | auth browser, Custom Tabs, Credential Manager/Keystore integration |
| `box/**` | remote execution / remote computer connector |
| `coordinator/**` | Mahayana Coordinator ownership/bootstrap |
| `deep-link/**` | Intent/deep-link router |
| `dev/**`, `electron-dev-controls/**` | debug-only Android dev controls |
| `downloads/**` | DownloadManager/WorkManager-backed downloads |
| `experiments/**` | typed feature flags |
| `feedback/**` | Android feedback surface |
| `generated/**` | generated typed bindings |
| `host-window-chords.ts` | keyboard/shortcut intent mapping where applicable |
| `local-exec/**` | Android Runner integration |
| `mcp/**` | MCP lifecycle/platform integration |
| `media/**` | Android media playback/recording/viewing |
| `models/**` | model/provider platform settings |
| `notifications/**` | NotificationManager/channels |
| `onepassword/**` | credential-provider equivalent if supported; otherwise reviewed N/A |
| `prefs/**` | DataStore/typed settings |
| `process-metrics/**` | Android process/runtime metrics |
| production adapters/bindings/IPC | Kotlin/JNI typed production bindings |
| `secrets/**` | Android Keystore-backed secrets abstraction |
| `startup/**` | Application/process bootstrap |
| `telemetry/**` | Android telemetry adapter |
| `update/**` | Play/GitHub update channel integration |
| `vnc/**` | RemoteComputer surface/liveness/input bridge |
| window broadcast/chrome/shortcuts/state | Activity/multi-window/configuration/navigation equivalents |

Desktop-only window details may be `platform-adapted` or `not-applicable`, but the decision must be explicit in the ledger.

### R8 — Native Android renderer parity

Grok `frontend/**` is mapped to native Jetpack Compose rather than a primary WebView renderer.

The Android UI must provide Android-responsive equivalents for the reference's production/recovered renderer concepts, including at minimum:

- production renderer/root shell;
- bot/agent roster/sidebar;
- conversation transcript;
- composer;
- streaming/thinking/running/completed states;
- stop/cancel;
- agent naming/edit/delete flows;
- row actions;
- command palette/search/action dispatch;
- settings and notices;
- reactions;
- group members;
- attachments/media;
- MCP/connector affordances;
- remote-computer/context surfaces;
- errors/retry/recovery;
- onboarding/auth transitions.

`GrokMobileShellAndroid.kt` and `FabushiScreen.kt` must not remain giant all-purpose renderer/controller files after migration. UI files render immutable state and emit typed user intents.

### R9 — Shared contracts parity

Grok `source/shared/**` responsibilities must map to canonical shared contracts, preferably in `bhrumom/fabushi-platform-core` when cross-platform.

The mapping must cover Android-relevant equivalents of:

- agents;
- auth;
- automation schedule/automations;
- box migration/runtime/secrets;
- channels/channel messaging;
- persistence;
- errors and retry;
- deep links;
- gateway reachability/wire;
- host settings;
- inference router;
- local exec gateway/process identity/permissions;
- MCP instructions/auth/contracts;
- media;
- message references;
- observability;
- ordering;
- notifications;
- RPC;
- transcript/thread contracts;
- workflow model;
- usage;
- VNC/remote-computer liveness;
- WebAuthn gateway;
- write epochs/versioning.

Platform-specific wrappers remain Android-owned; wire/domain truth must not diverge per platform.

### R10 — Packages parity

Every Grok module under `source/packages/**` must be represented in the parity ledger. The initial package set is:

- `agent-analytics`
- `agent-client`
- `agent-core`
- `agent-exec`
- `agent-kv`
- `agent-store-sync`
- `agent-summarization`
- `agent-transcript`
- `agent`
- `analytics-client`
- `chat-inference-proto`
- `chat-inference`
- `constants`
- `context-rpc`
- `context`
- `cursor-config`
- `cursor-plugins`
- `git-core`
- `hooks-carriers`
- `hooks-exec`
- `hooks`
- `local-exec`
- `mcp-agent-exec`
- `mcp-core`
- `metrics`
- `prompt-jsx`
- `proto`
- `redacted-protos`
- `redaction`
- `shell-exec`
- `utils`

Each must be classified as shared-core, Android-native equivalent, platform-adapted, or reviewed N/A. Package semantics must not be collapsed into a single untestable monolith.

### R11 — Feature-effect parity

For features supported on Android, externally observable behavior must match the Grok reference contract, not merely share names.

Required parity dimensions include:

- request lifecycle;
- first-token/first-state progression;
- streaming updates;
- tool call presentation and settlement;
- thinking/running/completed/failed/recovered states;
- cancellation behavior;
- reconnection;
- transcript restoration;
- duplicate-event handling;
- reactions;
- connector/MCP discovery and invocation;
- OAuth return flow;
- attachment upload/open;
- remote-computer activity;
- error and retry semantics;
- settings persistence;
- notification behavior;
- background/foreground transition;
- process recreation.

### R12 — One canonical state truth

After cutover there must be exactly one canonical owner for each of:

- account/auth state;
- agent/bot roster;
- conversation/transcript;
- active turn/operation;
- MCP/connector state;
- installed Mini App/plugin state;
- remote-device identity;
- settings;
- execution permissions.

ViewModels are presentation adapters. They must not maintain durable competing product truths.

### R13 — Android process-death resilience

Unlike Electron desktop, Android may destroy the app process at any time. The target architecture must support:

- durable coordinator session identity where appropriate;
- safe recreation of Coordinator and Host;
- reattachment/resync of renderer state;
- no duplicate send after recreation;
- deterministic settlement of in-flight turns;
- restored transcript/draft/navigation state where product-appropriate;
- remote gateway re-registration;
- cancellation/timeout cleanup;
- no leaked JNI/native handles.

Process-death and recreation is a first-class parity extension required for Android correctness.

### R14 — Security boundaries

- Secrets use Android Keystore or the canonical protected secret provider; never UI state, logs, or plain files.
- Platform bridge APIs are allowlisted and typed.
- Mini App WebView bridges are origin/capability scoped.
- Local-exec requires explicit capability and permission gates.
- Remote-device control exposes reviewed semantic actions, not unrestricted shell/reflection/credential mutation.
- OAuth/WebAuthn tokens must not be copied into transcript/UI logs.
- External URLs/deep links must use an allowlisted policy equivalent to Grok shared policy.
- Telemetry must scrub secrets and sensitive content.

### R15 — Provenance / rights

Because the Grok reconstruction states that no upstream source-code license is granted:

- use Grok source as architecture/protocol/behavior reference;
- do not represent reconstructed material as official upstream source;
- do not bulk-copy implementation text into Fabushi without an explicit rights determination;
- record provenance for behavior-sensitive mappings;
- preserve a rights-review gate before public redistribution of any directly derived material.

### R16 — Legacy removal

Once replacement paths pass parity acceptance:

- remove superseded orchestration from `MainActivity.kt`;
- split or remove monolithic responsibilities in `GrokMobileShellAndroid.kt` and `FabushiScreen.kt`;
- remove duplicate event pumps and duplicate native-host ownership;
- remove legacy direct Host calls from presentation code;
- remove temporary adapters that bypass Coordinator;
- remove unused compatibility state and dead feature flags.

A migration is not complete while the old architecture remains the hidden fallback.

## 5. Current state

At baseline, Fabushi Android is a native Android application with Compose and ViewModels.

Observed architecture characteristics include:

- `MainActivity.kt` directly instantiates/coordinates Marketplace, Messaging, Bot, update, Mini App, remote-device gateway, login/deep-link, and shell-selection responsibilities.
- `GrokMobileShellAndroid.kt` combines Compose UI with semantic agent-surface projection/action registration.
- `MobileBotViewModel.kt` calls `MahayanaHost` directly and owns bot-specific presentation/cache behavior.
- `MahayanaHost.kt` owns a process-shared JNI native host handle and fans out feature events to multiple Kotlin consumers.
- `FabushiRemoteDeviceGateway.kt` owns a WebSocket lifecycle and directly instantiates `MahayanaHost`.
- `app/build.gradle` currently expresses a single Android application module rather than explicit runtime/coordinator/platform/UI Gradle module boundaries.

These pieces provide useful existing functionality, but they do not yet express the full Grok Coordinator/Host/Runner/platform separation.

## 6. Target state

Target logical flow:

```
Compose screens/components
        │ user intents / immutable state
        ▼
Android Renderer Adapter
        │ typed coordinator contract
        ▼
Android Platform Bridge
        │ JNI / typed IPC
        ▼
Mahayana Coordinator
        │
        ├── renderer ports
        ├── request/reply/event/cancel
        ├── reconnect/resync
        ├── inference router
        ├── MCP/OAuth/WebAuthn relay
        ├── gateway + local-exec routing
        └── Host supervision
        │
        ▼
Mahayana Host
        │
        ├── agents / workflows / automations
        ├── transcript/storage
        ├── tools/MCP/connectors
        └── runner composition
        │
        ├──────────────┐
        ▼              ▼
Android Runner     Remote/Box Runner
        │              │
        └──── typed results/events ────┘
```

The Android repository owns native UI and platform integration. Shared Coordinator/Host/contracts live in `bhrumom/fabushi-platform-core` when they are cross-platform canonical code, then are consumed here through versioned bindings. The requirement "all Grok modules are migrated for Android" means every Grok responsibility has an Android-product disposition and parity result; it does not authorize duplicating shared-core implementation into the Android repository.

## 7. Architecture and ownership boundaries

### 7.1 Target Android source organization

The implementation should converge toward clear source boundaries such as:

```
mobile/android/
  app/
    ... minimal application assembly ...
  ui/
    shell/
    agents/
    conversation/
    composer/
    commandpalette/
    settings/
    connectors/
    remotecomputer/
    miniapps/
  presentation/
    coordinator/
    model/
    agent-surface/
  platform/
    android/
      lifecycle/
      auth/
      deeplink/
      attachments/
      media/
      notifications/
      downloads/
      prefs/
      secrets/
      webview/
      remotecomputer/
      updates/
      telemetry/
      devcontrols/
    bridge/
  runner/
    android/
  bindings/
    mahayana/
```

Exact Gradle module boundaries may be introduced incrementally, but package dependency rules must match the logical layers from the first cutover PR.

### 7.2 Dependency direction

Allowed:

```
UI -> presentation -> bridge -> coordinator -> host -> runner
platform adapters -> bridge/coordinator through interfaces
shared contracts <- all layers
```

Forbidden:

- UI -> Host direct;
- UI -> Runner direct;
- Activity -> agent-domain state mutation;
- Runner -> Compose UI;
- Host -> Activity/ViewModel;
- feature-specific ViewModel -> independent JNI host creation after cutover;
- arbitrary bidirectional global singleton dependencies.

## 8. Interfaces / contracts / schemas / data flow

### 8.1 Coordinator envelope

Define a versioned contract equivalent in capability to Grok coordinator-port/RPC contracts:

```
Request {
  protocolVersion
  requestId
  sessionId
  method
  params
  deadline?
}

Reply {
  requestId
  ok
  result?
  error?
}

Event {
  eventId
  sessionId
  sequence
  type
  payload
}

Cancel {
  requestId | operationId
  reason?
}
```

The exact schema belongs in canonical shared contracts and must support forward-compatible versioning.

### 8.2 Renderer state

Compose consumes immutable state projections. All commands are typed intents. Rendering must not poll the Host directly.

### 8.3 Transcript ordering

Coordinator/Host must define stable event IDs, sequence/order semantics, duplicate suppression, mutation handling, and resync snapshots.

### 8.4 Execution flow

```
UI intent
 -> Coordinator request
 -> Host command
 -> Runner/tool/MCP
 -> Host event
 -> Coordinator ordered event
 -> presentation projection
 -> Compose render
```

Cancellation follows the same ownership path in reverse and must settle every layer.

## 9. Constraints and non-functional requirements

- Android min/target SDK constraints remain repository-defined.
- Primary shell remains native Compose.
- Shared runtime should prefer Rust where it improves cross-platform canonical behavior, safety, or performance, but language is not an architectural goal.
- Android platform APIs remain Kotlin/Android where that is the native boundary.
- No long-running blocking JNI call on the main thread.
- Streaming must be incremental; do not buffer a full answer before UI update.
- Background work must respect Android lifecycle and platform restrictions.
- Idle UI animation must avoid unnecessary continuous high-frequency work.
- Crash/restart must not corrupt transcript or duplicate commands.
- Contracts require deterministic error codes rather than UI-parsed exception strings.
- Logs and evidence must be privacy-scrubbed.
- New architecture must be testable without a production network for contract/state-machine tests.

## 10. Failure modes and edge cases

The implementation and tests must explicitly cover:

- Coordinator starts but Host fails;
- Host crashes mid-turn;
- Runner crashes or times out;
- MCP server disconnects/reconnects;
- OAuth callback arrives after Activity recreation;
- WebAuthn/passkey cancellation;
- network loss during streaming;
- duplicate/out-of-order events;
- renderer reconnects after backgrounding;
- Android process death;
- native library load failure;
- stale native handle;
- multiple UI collectors;
- double-send/double-cancel;
- attachment URI permission expiry;
- unavailable local-exec capability;
- remote-device gateway token/session rollover;
- Mini App WebView process failure;
- storage full/read-only/corrupt state;
- version/protocol mismatch between Android bindings and shared core;
- incompatible migration data;
- update/restart during an active operation.

Each failure must end in a deterministic recoverable or terminal state, never indefinite "thinking".

## 11. Implementation strategy

### Phase 0 — Evidence and inventory

1. Pin Grok SHA and Android starting SHA.
2. Generate complete file-level parity ledger.
3. Capture Android current architecture/dependency map.
4. Record Grok behavior evidence for critical flows.
5. Resolve rights/provenance classification.
6. Define acceptance fixtures before destructive cutover.

Exit gate: 100% of Grok `source/**` and `frontend/**` files classified.

### Phase 1 — Shared contracts and bindings

1. Define versioned coordinator/request/event/cancel contracts.
2. Move/share cross-platform contracts in `fabushi-platform-core`.
3. Generate or implement typed Kotlin bindings.
4. Add contract compatibility tests.

Exit gate: Android can connect to a test Coordinator without direct feature JSON calls from UI.

### Phase 2 — Mahayana Coordinator

Implement the complete logical equivalent of Grok `node-agent-coordinator/**`, including renderer ports, routing, MCP relay, OAuth/WebAuthn forwarding, local-exec routing, Host supervision, cancellation, reconnect/resync, and crash settlement.

Exit gate: deterministic coordinator state-machine tests cover normal, cancel, reconnect, duplicate, and crash paths.

### Phase 3 — Host and Runner parity

Map `host/**`, `local-exec-daemon/**`, `box-exec-daemon/**`, `internal/**`, and `packages/**` to shared core and Android adapters.

Exit gate: all agent-domain actions flow Coordinator -> Host -> Runner/tool and return ordered events.

### Phase 4 — Android platform-main parity

Extract Android platform ownership from `MainActivity` into lifecycle/auth/deeplink/media/notification/prefs/secrets/download/update/remote-computer adapters.

Exit gate: `MainActivity` is a thin lifecycle/Compose host.

### Phase 5 — Renderer/UI parity

Rebuild UI around immutable coordinator projections. Split monolith screens. Reproduce relevant Grok behaviors using responsive native Compose.

Exit gate: primary conversation, roster, composer, command palette, settings, connectors/MCP, attachments, remote computer, and recovery states all operate without direct Host access.

### Phase 6 — Product feature parity

Complete Marketplace/Mini Apps, messaging, bot projection, account/entitlement, remote-device gateway, and Android-specific features through the new architecture.

Exit gate: no feature owns a competing durable truth.

### Phase 7 — Legacy cutover/removal

Remove direct Host calls, duplicate event pumps, legacy orchestration, fallback shell paths, and obsolete state stores.

Exit gate: architecture checker proves forbidden dependencies do not exist.

### Phase 8 — Full verification and delivery

Run exact-HEAD CI, Android device/emulator acceptance, process-death tests, release packaging, install/upgrade verification, and evidence collection.

Exit gate: all ACs passed or explicitly blocked; no "complete" state while required AC is blocked.

## 12. Verification / test strategy

### 12.1 Static architecture checks

Add automated rules that fail CI when:

- Compose/UI imports Host/Runner implementation;
- ViewModel creates `MahayanaHost` directly after cutover;
- `MainActivity` regains feature orchestration;
- presentation depends on Android secret/storage implementations;
- duplicate coordinator implementations appear;
- legacy forbidden paths are reintroduced.

### 12.2 Contract tests

Test request/reply/event/cancel compatibility across Kotlin and Rust/shared core.

### 12.3 Coordinator state-machine tests

Minimum scenarios:

- send -> streaming -> complete;
- send -> tool call -> result -> complete;
- send -> cancel -> settled;
- disconnect -> reconnect -> resync;
- duplicate event -> one UI effect;
- Host crash -> terminal failure/recovery;
- stale generation/session -> rejected/resynced.

### 12.4 Android lifecycle tests

- rotate/configuration change;
- background/foreground;
- Activity recreation;
- process kill/relaunch;
- notification tap/deep link;
- OAuth callback;
- attachment picker result;
- remote gateway reconnect.

### 12.5 UI parity tests

Use Compose tests and visual evidence for:

- agent list/row actions;
- conversation;
- composer/send/stop;
- thinking/running/completed/failed/recovered;
- command palette;
- settings;
- MCP/connectors;
- reactions;
- attachments/media;
- Mini App entry;
- remote computer;
- errors/retry.

### 12.6 Real packaged acceptance

Acceptance must use a packaged Android artifact produced from the exact tested commit. Validate fresh install and upgrade paths, app-owned device registration where applicable, account login, normal chat, tool/MCP flow, background/recovery, logout, and relaunch.

## 13. Acceptance criteria / Definition of Done

- **AC-1**: A complete parity ledger covers 100% of pinned Grok `source/**` and `frontend/**` files with no unclassified item.
- **AC-2**: All top-level Grok architecture responsibilities have an Android/shared-core equivalent or reviewed N/A rationale.
- **AC-3**: A first-class Mahayana Coordinator owns renderer-port, request/reply/event, cancel, reconnect/resync, routing, relay, supervision, and crash settlement.
- **AC-4**: Host and Runner are independent architectural boundaries; UI cannot invoke them directly.
- **AC-5**: `MainActivity` is reduced to Android lifecycle/root UI/platform result forwarding and does not orchestrate product features.
- **AC-6**: `GrokMobileShellAndroid` / `FabushiScreen` no longer combine primary UI rendering with agent runtime orchestration.
- **AC-7**: Direct `MahayanaHost` construction is removed from product ViewModels/presentation paths except explicitly approved binding/bootstrap ownership.
- **AC-8**: All user-visible supported Grok-equivalent flows stream and settle correctly, including cancellation and failure recovery.
- **AC-9**: Android process death followed by relaunch restores/resyncs without duplicate sends, stuck turns, or leaked native handles.
- **AC-10**: MCP/connector discovery, auth, invocation, tool result, and error states work through Coordinator/Host boundaries.
- **AC-11**: Attachments, media, deep links, OAuth/WebAuthn where supported, notifications, and remote-computer flows use Android-native platform adapters.
- **AC-12**: One canonical state truth exists for auth, roster, transcript, operation, MCP, Mini Apps, remote device, settings, and permissions.
- **AC-13**: Legacy orchestration/fallback paths are removed after cutover.
- **AC-14**: Architecture checks prevent regression to monolithic UI/Activity/Host coupling.
- **AC-15**: Exact-HEAD CI is green for compile, unit, contract, architecture, and Android UI/instrumentation tests required by this Spec.
- **AC-16**: Packaged artifact acceptance passes on a fresh Android environment, including background/process-recreation scenarios.
- **AC-17**: Provenance/rights review has no unresolved release-blocking item for redistributed derived material.
- **AC-18**: The final Spec compliance record marks every requirement and AC `passed`, `blocked`, or `not-applicable` with evidence; completion requires all mandatory items `passed`.

## 14. Release / migration / rollback

Migration must be staged behind explicit internal architecture cutover flags only while both paths are required for controlled transition. Such flags are temporary and must be removed by AC-13.

Persistent data migrations must be versioned, idempotent, and reversible when feasible. Shared protocol versions must reject incompatible peers cleanly.

Rollback must preserve user account/session safety and transcript integrity. Never roll back by silently selecting an old parallel state store.

No release may be called architecture-complete from source tests alone; packaged Android acceptance is required.

## 15. Observability / evidence

Each implementation phase must produce evidence appropriate to the layer:

- parity ledger diff;
- architecture dependency report;
- contract test report;
- coordinator state-machine report;
- Android lifecycle/process-death report;
- UI screenshots/video for critical flows;
- MCP/connector trace with secrets scrubbed;
- exact commit SHA;
- CI workflow/run IDs;
- packaged artifact identity/checksum;
- install/upgrade acceptance report;
- final Spec compliance table.

Logs must include request/operation/session lineage without raw credentials or sensitive message content.

## 16. References / provenance

Primary reference:

- `b-nnett/grok-bot-0.18-reconstructed@a9f633e09d49a85829b8236331b9e21f7e612634`
- `README.md`
- `NOTICE.md`
- `PROVENANCE.md`
- `docs/ARCHITECTURE.md`
- `source/electron-main/**`
- `source/electron-preload/**`
- `source/node-agent-coordinator/**`
- `source/host/**`
- `source/local-exec-daemon/**`
- `source/box-exec-daemon/**`
- `source/internal/**`
- `source/packages/**`
- `source/shared/**`
- `frontend/**`

Fabushi discovery baseline:

- `bhrumom/fabushi-android@59f6fc8885ce1cb8d1ad4fc5d4ab36f690fb99a2`
- `AGENTS.md`
- `docs/specs/spec-first-ai-development.md`
- `mobile/android/app/src/main/java/com/ombhrum/fabushi/MainActivity.kt`
- `GrokMobileShellAndroid.kt`
- `FabushiScreen.kt`
- `MobileBotViewModel.kt`
- `MessagingViewModel.kt`
- `MarketplaceViewModel.kt`
- `FabushiAppAgentSurface.kt`
- `FabushiRemoteDeviceGateway.kt`
- `core/MahayanaHost.kt`
- `mobile/android/app/build.gradle`

### Initial top-level Grok module parity map

| Grok reference | Required Android/Fabushi disposition |
| --- | --- |
| `frontend/**` | native Compose renderer + presentation projections |
| `source/electron-main/**` | Android lifecycle/platform adapters + Coordinator bootstrap |
| `source/electron-preload/**` | thin typed Android bridge |
| `source/electron-dev-controls/**` | debug-only Android dev controls |
| `source/node-agent-coordinator/**` | Mahayana Coordinator |
| `source/host/**` | Mahayana Host + shared domain services |
| `source/local-exec-daemon/**` | Android/local Runner |
| `source/box-exec-daemon/**` | remote/box Runner adapter |
| `source/internal/**` | shared internal host extensions/scheduling |
| `source/packages/**` | classified package-by-package into shared core or Android adapters |
| `source/shared/**` | canonical versioned shared contracts/policies |

### Initial Coordinator submodule map

| Grok coordinator module | Target |
| --- | --- |
| `carrier.ts` | coordinator transport/carrier contract |
| `client-side-tool-v2-relay.ts` | client tool relay |
| `control-port-client.ts` | typed control-port client |
| `gateway/**` | gateway router/session |
| `inference-router.ts` | provider/model inference routing |
| `local-exec/**` | runner routing |
| `main.ts` | Coordinator assembly/bootstrap |
| `oauth/**` | OAuth forwarding/session settlement |
| `renderer-port-server.ts` | Android renderer-port server/adapter |
| `routed-mcp-bridge.ts` | MCP routing bridge |
| `telemetry/**` | coordinator observability |
| `webauthn/**` | Android Credential Manager/WebAuthn bridge where supported |

### Initial Host area map

Every one of these Grok Host areas requires a ledger disposition: `agent-isolation`, `agents`, `automations`, `box`, `cloud-agents`, `connectors`, `extensions`, `groups`, `local-exec`, `mcp-auth`, `ports`, `runner`, `storage`, `transcript-mirror`, `workflows`, plus Host gateway, event-bus, transcript-load, lock, paths, diagnostics, secrets, roster, crash guard, runner composition/bridges, activity, user identity, and trace responsibilities represented by top-level Host files.

## 17. Spec compliance record

| Requirement / AC | Status | Evidence / reason |
| --- | --- | --- |
| R1 | pending | complete file-level parity ledger not yet created |
| R2 | pending | implementation not started |
| R3 | pending | Coordinator parity implementation not yet verified |
| R4 | pending | Host parity implementation not yet verified |
| R5 | pending | Runner parity implementation not yet verified |
| R6 | pending | thin bridge cutover not yet verified |
| R7 | pending | Android platform-main decomposition not yet verified |
| R8 | pending | native renderer parity not yet verified |
| R9 | pending | shared contract parity not yet verified |
| R10 | pending | package-by-package disposition not yet complete |
| R11 | pending | behavioral parity acceptance not yet run |
| R12 | pending | canonical state ownership audit not yet complete |
| R13 | pending | process-death acceptance not yet run |
| R14 | pending | security review not yet complete |
| R15 | pending | rights/provenance review required before release |
| R16 | pending | legacy removal not yet complete |
| AC-1 | pending | file-level ledger required |
| AC-2 | pending | top-level parity mapping defined; implementation pending |
| AC-3 | pending | implementation pending |
| AC-4 | pending | implementation pending |
| AC-5 | pending | current MainActivity still orchestrates features |
| AC-6 | pending | current renderer files remain mixed |
| AC-7 | pending | current ViewModels still construct/use MahayanaHost directly |
| AC-8 | pending | acceptance pending |
| AC-9 | pending | process-death acceptance pending |
| AC-10 | pending | MCP/connector parity pending |
| AC-11 | pending | platform adapter parity pending |
| AC-12 | pending | state ownership audit pending |
| AC-13 | pending | legacy cutover pending |
| AC-14 | pending | architecture checker pending |
| AC-15 | pending | exact-HEAD CI pending |
| AC-16 | pending | packaged acceptance pending |
| AC-17 | pending | rights review pending |
| AC-18 | pending | final compliance review pending |

Allowed final statuses: `passed`, `blocked`, `not-applicable`.
