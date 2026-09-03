# Flash Card backlog

This is the single authoritative project backlog. Design documents may describe limitations, and source comments may reference a task ID, but priority and completion state belong here.

Last reviewed: 2026-09-03

## Working agreement

- `P0` protects user data or is required before a dependable public release.
- `P1` improves core behavior, maintainability, or accessibility.
- `P2` is useful but can wait without compromising the current study workflow.
- Keep each item independently reviewable. Link an issue or pull request beside its ID when work begins.
- Move completed work to `CHANGELOG.md`; do not retain checked-off tasks indefinitely.
- Storage or scheduling changes require focused tests and corresponding updates to `docs/APP_DESIGN.md` and `docs/DATA_FORMAT.md`.

## P0 — data integrity and release safety

- [ ] **DATA-002 — Version and structure collection metadata.**
  Replace the positional comma-delimited zero-card body with a versioned representation or dedicated schema while retaining a reader for existing data. Done when old Samsung/device data upgrades in place and legacy/new round trips are tested and documented.

- [ ] **DATA-003 — Define Android backup and device-transfer rules.**
  Decide explicitly whether the Room database and collection preferences are included in cloud backup and device transfer, then replace the template extraction rules. Done when the policy is documented and a restore preserves a valid selected collection and card cycle.

- [ ] **REL-001 — Establish a repeatable release upgrade path.**
  Configure release signing outside source control, increment `versionCode` for releases, produce a release candidate, and exercise an update from the previous installed version. Done when the release artifact installs without data loss and the smoke checklist passes.

## P1 — core product and engineering quality

- [ ] **IO-001 — Move import/export to the Storage Access Framework.**
  Add file create/open flows while keeping clipboard interoperability only if deliberately supported. Done when users can back up and restore a set through a document provider and cancellation/malformed-file paths are tested.

- [ ] **VAL-001 — Validate names and imported content at boundaries.**
  Define allowed collection names, reject reserved delimiters/empty values, and return actionable errors for malformed or structurally invalid payloads. Done when invalid input cannot create ambiguous IDs or partially overwrite a collection.

- [ ] **CONC-001 — Replace ad-hoc coroutine scopes with lifecycle-aware work.**
  Move activity/fragment jobs into ViewModels or lifecycle scopes, keep UI work on the main thread, and make repository work structured and cancellable. Done when leaving a screen cannot orphan database/UI work and focused lifecycle tests cover the affected flows.

- [ ] **TEST-001 — Expand high-risk behavior coverage.**
  Add focused cases for invalid-import rollback, duplicate titles, empty/one/many-card repositories, daily rollover with an injected clock, and ViewModel error states. Keep the suite layered according to `docs/TESTING.md`.

- [ ] **ALGO-001 — Upgrade the memory algorithm (specification pending).**
  Wait for the user-provided algorithm specification; do not assume SM-2, FSRS, or another existing model. Once specified, document its states and transitions, define how existing card levels/state migrate, coordinate any persisted fields with `DATA-002`, and add deterministic boundary and sequence tests before changing study behavior.

- [ ] **SET-001 — Expose bijective-mode and font-size controls.**
  Add per-collection Settings controls with validation and live study-screen behavior. This depends on the metadata compatibility plan in `DATA-002`.

- [ ] **SET-002 — Make Settings navigation task-focused.**
  Split set management, TTS, import/export, and study behavior into clear destinations. Finish edit/save navigation and show success/failure feedback without duplicating coroutine orchestration.

- [ ] **A11Y-001 — Complete resource, accessibility, and localization cleanup.**
  Move remaining user-visible text into string resources, add meaningful content descriptions, verify large-font focus/layout behavior, and prepare resources for additional locales.

- [ ] **STUDY-001 — Design bookmark editing.**
  Decide whether insertion bookmarks are global or per collection, validate increasing positive positions, and refresh dealer state immediately after a change.

## P2 — enhancements and utilities

- [ ] **STUDY-002 — Add concise in-app behavior help.**
  Explain remembered/forgotten scheduling, reverse prompts, bookmarks, audio mode, and data backup in language suitable for users.

- [ ] **AUDIO-001 — Support deliberate TTS-on-tap behavior.**
  Define which card regions speak which side, avoid conflict with reveal gestures, and cover missing-language/engine behavior.

- [ ] **UI-001 — Refine themes and visual polish.**
  Consolidate light/dark styling and verify contrast, system bars, disabled states, and Material component consistency on API 34–36.

- [ ] **TOOL-001 — Resolve the vocabulary tool's documented append mode.**
  Either implement and test `--meta` append behavior or remove it from the script documentation so the CLI and examples agree.

- [ ] **TOOL-002 — Add a scheduling-reset utility.**
  Define whether reset belongs in the app or Python tooling, preserve card content and metadata, rebuild a valid cycle, and require confirmation plus round-trip tests.

- [ ] **REL-002 — Prepare the app for Google Play publication.**
  After `REL-001`, produce a signed Android App Bundle and complete Play App Signing, store listing assets, privacy policy, Data safety, content rating, current target-API/policy checks, and an internal testing release. Done when the Play pre-launch report is reviewed and an internal-track install/update passes the release smoke checklist without data loss.

## Not currently committed

Cloud sync, accounts, rich-media cards, tags, and detailed analytics are product ideas, not active backlog items. The memory-algorithm upgrade is active as `ALGO-001`, but its model is intentionally undecided until the user supplies the specification.
