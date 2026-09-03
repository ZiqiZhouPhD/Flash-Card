# Flash Card repository guide

This file applies to the entire repository. It is written for coding agents and contributors working on the Android app or its vocabulary conversion utility.

## Project at a glance

Flash Card is a single-module Android application written in Kotlin. It presents one card at a time, schedules cards by moving them through a circular linked list, supports multiple named sets, and stores all app data locally.

- UI: Android XML layouts, Material components, View Binding, activities, and one fragment
- Presentation: `ViewModel`, `LiveData`, and one-shot `Event` wrappers
- Business logic: card dealing, editing, collection selection, and the daily counter
- Persistence: Room over one `card` table, plus `SharedPreferences` for the current/previous set and bookmark positions
- Dependency injection: Hilt
- Serialization: Gson
- Auxiliary tool: `raw-vocab-to-meta-tool.py`, which converts tab-separated vocabulary into an importable JSON set
- Android configuration: compile/target SDK 36, minimum SDK 34; portrait orientation
- Build stack: Gradle 9.6.1, AGP 9.4, built-in Kotlin, KSP, and Java 17 bytecode

The authoritative design references are:

- `docs/APP_DESIGN.md` for product behavior, architecture, and scheduling
- `docs/DATA_FORMAT.md` for Room invariants, zero-card metadata, and JSON interchange
- `docs/TESTING.md` for test placement, naming, isolation, and device checks

## Repository map

```text
app/src/main/java/com/ziqiphyzhou/flashcard/
├── card_main/          # Study screen, scheduling/dealing, daily counter
├── card_add/           # Add-card screen
├── card_delete/        # Search/list screen used to edit or delete cards
├── card_edit/          # CardEditor business facade and edit screen
├── card_database/      # Repository contract and Room implementation
├── import_export/      # Clipboard JSON import/export
├── settings_manage/    # Set management, navigation, and TTS settings
├── dependency_injection/
└── shared/             # Domain model, constants, current-set state, Event

app/src/main/res/       # XML layouts, drawables, themes, and strings
docs/                   # Design and data-contract documentation
raw-vocab-to-meta-tool.py
```

## Build and verification

Use the checked-in Gradle wrapper. On Windows PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat lint
```

On macOS or Linux:

```bash
./gradlew test
./gradlew assembleDebug
./gradlew lint
```

Instrumented tests require an API 34+ emulator or device:

```powershell
.\gradlew.bat connectedAndroidTest
```

The Gradle JVM criteria select JetBrains JDK 21 while application sources target Java 17 bytecode. Android Studio's bundled runtime is the simplest compatible setup. Install Android SDK 36 and Build Tools 36.0.0, and do not edit or commit `local.properties`; it is machine-specific.

Tests mirror their production packages. JVM classes use the `*Test` suffix, Android component tests use `*IntegrationTest`, and critical UI entry points use `*SmokeTest`. The automated suite covers card-review scheduling, core Room repository invariants, export/import round trips, and application launch. For behavior changes, add focused tests around the changed contract. Prioritize repository link integrity and `CardDealerImpl` level/state transitions. For UI-only changes, build the app and manually exercise the affected screen in addition to the narrowest relevant automated task.

For a connected-device check that preserves the installed application's data, run `scripts/device-smoke-test.ps1`. Do not replace it with `adb uninstall`, `pm clear`, or another workflow that resets the target package.

## Architectural boundaries

Follow the existing dependency direction:

```text
Activity / Fragment -> ViewModel -> business class -> CardRepository -> CardDao
```

- Views render state, collect input, navigate, and show transient messages. Do not put scheduling or database mutation rules in an activity or fragment.
- ViewModels own presentation state and launch lifecycle-bound work with `viewModelScope`.
- Business classes express app behavior without depending on Android views.
- `CardRepository` is the business-facing persistence contract. Keep Room entities and DAO details behind its implementation.
- `ImportExportViewModel` currently talks directly to `CardRepository`; preserve its data validation and current-set update semantics if refactoring it.
- Hilt bindings belong in `dependency_injection/AppModule.kt`. `CurrentCollectionManager` must remain a singleton unless its process-wide semantics are deliberately redesigned.

Keep blocking Room and file work off the main thread. Update Android views and show `Snackbar`s on the main thread. Prefer structured, lifecycle-aware coroutines over standalone `CoroutineScope(...)` instances when touching existing code.

## Non-negotiable data invariants

The database is not an unordered collection of independent rows. Every set is exactly one circular linked list.

1. Each set has exactly one sentinel, or **zero card**, whose stored ID is `@<set-name>`.
2. A regular stored ID is `<8-character-logical-id>@<set-name>`.
3. Every stored `previous` value includes the same `@<set-name>` suffix.
4. The zero card's `previous` points to the last card. The first card's `previous` points to the zero card.
5. Every row has exactly one successor, found by querying for the row whose `previous` equals the current row's ID.
6. All rows in the cycle have the same `coll` value and set-name suffix.
7. Regular card titles are non-empty. Current presentation code normalizes an empty title to the literal `"null"`.
8. Levels stay in `0..LEVEL_CAP`; `state = 1` means remembered/true and `state = 0` means forgotten/false.
9. The zero card's body is a positional, comma-separated metadata record. Changing its field order without a migration breaks preferences, scheduling, and interchange compatibility.
10. Exported IDs are logical IDs without the collection suffix. Imports add the suffix before persistence.

Read `docs/DATA_FORMAT.md` before changing DAO queries, identifiers, import/export, metadata, add/delete/bury operations, or collection management.

Repository mutations must leave the cycle intact even on failure. Multi-row mutations should be atomic; when improving them, prefer Room transactions instead of asynchronous cleanup or rollback work. Never bypass `CardRepository` from feature code to manipulate links directly.

## Scheduling rules to preserve

- Default insertion bookmarks are positions `20, 30, 50, 100, 200, 500, 1000, 2000, 5000, 10000`.
- A remembered card generally increases one level and is buried at that level's bookmark.
- A forgotten card decreases one level, sets `state` false, and is buried at bookmark level zero.
- Levels are clamped to `0..10`.
- In a bijective set, sufficiently learned cards can display body-to-title. The exact threshold/state transition is documented in `docs/APP_DESIGN.md`.
- Adding cards inserts them after the last card at level 2 or below.
- A day resets at 03:00 local time. The daily count increments only for `Know it!` actions.

Treat changes to these constants or transitions as product behavior changes: update tests and design documentation in the same change.

## UI and navigation conventions

- Keep the app portrait-oriented unless changing the manifest and validating every layout.
- Use View Binding; do not introduce synthetic view access.
- Put user-visible copy in `res/values/strings.xml` when modifying a screen. Some legacy hard-coded strings remain, but new code should not add more.
- Preserve edge-to-edge inset handling on activities.
- `MainActivity` is the launcher and study screen. Its floating action button opens Settings; a long press toggles audio mode.
- Settings is the navigation hub for add, edit/delete, import/export, set management, and TTS voice selection.
- Deletion of a card or set and destructive import require confirmation.
- One-time UI messages should not replay after configuration changes; the current code uses `Event<T>` for this purpose.

When adding an activity, register it in `AndroidManifest.xml`, annotate it with `@AndroidEntryPoint` if it injects a Hilt ViewModel, provide up navigation, and handle system-bar insets consistently.

## Import/export compatibility

The clipboard payload is a JSON two-element array:

```json
["set-name", [{"id":"","title":"","body":"","level":0,"previous":"","state":true}]]
```

The second element is a list of domain `Card` objects and must include the zero card. Imports overwrite rows in the named set, validate the linked structure, and switch the current set only after success. Do not silently change this envelope or field meanings. If a new format is required, add explicit versioning and maintain a compatibility path.

The Python utility currently creates a new JSON payload from `word<TAB>definition` input. Its module docstring mentions appending with `--meta`, but the implemented CLI does not provide that option; do not document or rely on it as working until it is implemented and tested.

## Change discipline

Before editing:

- Inspect the complete call path for the behavior, not just the visible activity.
- Check `git status` and preserve unrelated user/IDE changes.
- Identify whether the change touches the circular-list or zero-card contracts.

While editing:

- Keep changes scoped; avoid opportunistic architecture migrations.
- Prefer explicit names such as `collectionName` over new abbreviations.
- Preserve public repository behavior unless the task explicitly changes it.
- Add comments for invariants and non-obvious transitions, not line-by-line narration.
- Do not hand-edit generated files or build output under `.gradle/`, `build/`, or `app/build/`. Room schema snapshots under `app/schemas/` are versioned compatibility artifacts and are the exception.

Before handing off:

- Run the narrowest meaningful tests, then `assembleDebug` for Kotlin/resource changes when feasible.
- For persistence changes, cover empty, one-card, multi-card, duplicate-title, import failure, and link-integrity cases as applicable.
- For scheduling changes, cover both boolean states, levels around the reversal threshold, level bounds, and a set shorter than the bookmark positions.
- Update `docs/APP_DESIGN.md` or `docs/DATA_FORMAT.md` whenever their described behavior changes.
- Report what was verified and any environment limitation; never imply an unrun check passed.

## Current known debt

Do not mistake these legacy conditions for recommended patterns:

- Automated tests are placeholders.
- Some activities create ad-hoc coroutine scopes and touch UI from IO-launched work.
- Several multi-row Room mutations are not declared transactions.
- Dealer bookmark initialization launches asynchronous work instead of awaiting it.
- The zero-card metadata is unversioned and comma-delimited.
- Import/export uses the clipboard rather than Android's Storage Access Framework.
- Several strings are hard-coded, and some unused imports/comments remain.

It is appropriate to improve one of these when it is in scope, but preserve user-visible behavior and the data format unless the requested change explicitly includes a migration.
