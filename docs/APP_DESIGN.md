# Flash Card app design

## Document purpose

This document describes the application implemented in this repository: its user experience, components, data flow, scheduling behavior, and present design constraints. It is a current-state specification, not a promise that every desirable feature is complete. The separate [data-format reference](DATA_FORMAT.md) defines the persistence and interchange contracts in detail.

## Product summary

Flash Card is an offline, single-user Android study app. A user organizes cards into named sets, selects one current set, and studies its cards in a queue. Rating a card changes its learning level and moves it deeper or shallower in that queue. All sets remain on the device unless the user copies a JSON export elsewhere.

The main product goals inferred from the implementation are:

- Make the study loop require very few taps.
- Reintroduce difficult cards sooner and familiar cards later.
- Support two-way recall for sets where either side can be the prompt.
- Support hands-free study through text-to-speech and media buttons.
- Keep set management and backup possible without an account or network service.

The app does not currently provide cloud sync, user accounts, rich media cards, tags, statistics beyond a daily known-card count, or a standard spaced-repetition due-date model.

## User model and core concepts

- **Set / collection:** A named deck. The code uses both words; `coll` means collection.
- **Current set:** The only set affected by study, add, search/edit/delete, voice settings, and export actions.
- **Previous set:** The last valid set selected before the current one. It enables quick switching.
- **Top card:** The first regular card after a set's sentinel in the circular queue.
- **Level:** An integer from 0 through 10 used to select how deeply a card is reinserted.
- **State:** Whether the card's latest learning state is remembered (`true`) or forgotten (`false`). It also participates in two-way prompt selection.
- **Bijective set:** A set allowed to prompt from either card side after the configured learning threshold.
- **Zero card:** An invisible sentinel that anchors a set's queue and stores set-level metadata.

## Navigation and screen responsibilities

```text
Main / Study
└── Settings
    ├── Add cards
    ├── Edit cards
    │   ├── Edit one card
    │   └── Add cards
    ├── Import / Export
    ├── Create or delete a set
    ├── Switch current or previous set
    └── Select title/body TTS voices
```

All screens are activities except the result list inside `DeleteActivity`, which is a fragment with a `RecyclerView`.

### Main / Study

`MainActivity` is the launcher screen. On entry, it initializes the dealer and renders one of these conditions:

- No current set: `No set selected`.
- Current set has only its zero card: `'<set>' is empty`.
- Set is ready: first show the set name; a tap loads the first prompt.
- Card loaded: show the prompt, keep the answer hidden, and enable rating actions.

The main interactions are:

- Tap the study surface to load the first card or reveal the current answer.
- Tap **Know it!** to rate the card remembered immediately.
- Tap **Show hint…** to reveal the answer. The same button becomes **I don't know…**; tap again to rate the card forgotten.
- Tap the counter toggle to show or hide today's known-card count.
- Tap the floating action button to open Settings.
- Long-press the floating action button to toggle audio mode.

During a load or queue mutation, `Freeze` state blurs the text and disables the rating buttons. Custom title and body font sizes are read from set metadata; absent or invalid sizes fall back to 26sp and 18sp.

### Audio mode

Audio mode hides the system bars, disables the on-screen rating buttons, and speaks each newly loaded prompt. Hardware/media key behavior is:

| Key | Action |
| --- | --- |
| Previous | Mark the pending rating as remembered and speak the prompt side. |
| Play/Pause | Mark the pending rating as forgotten and speak the answer side. Parenthesized segments are removed before speech. |
| Next | Submit the pending rating, bury the card, and load the next one. |

TTS language choices are stored independently for the title and body sides. The stored string is interpreted as `language-country-variant`; malformed or blank values fall back to the device locale.

### Settings

`SettingsActivity` is both configuration and feature navigation. It shows the current and previous sets, provides a set selector, and links to add, edit/delete, and import/export.

Set operations:

- **Create New Set** creates a zero-card-only set and makes it current. Duplicate names fail.
- **Delete Current Set** permanently removes all its rows after confirmation, then clears the current selection.
- **Switch** selects an item from the list, including `(none)` to clear selection.
- **Previous** switches to the last still-existing selection.

Voice operations list Android `TextToSpeech.availableLanguages` and store separate selections for the two card sides. Set metadata already has reserved positions for bijective mode and font sizes, but this screen does not currently expose controls for them.

### Add cards

`AddActivity` inserts a new card into the current set. It calculates an insertion anchor once when the screen starts and then advances that anchor to each newly added card, so a batch entered in one visit remains ordered.

- The insertion anchor is the last queue card whose level is at most `ADD_NEW_CARD_AFTER_LEVEL` (2).
- A duplicate title prompts for confirmation but is allowed.
- An empty title is persisted as the literal `null`.
- A successful save clears both fields and focuses the title for fast entry.

### Search, edit, and delete cards

`DeleteActivity` is the card-management browser despite its name. Search is a case-insensitive SQL `LIKE '<input>%'` prefix query, excludes zero cards, sorts by title, and returns at most ten matches. An empty query returns no results.

Each result can:

- Open `CardEditActivity` with its logical ID, title, and body.
- Be deleted after confirmation. The repository reconnects the deleted card's successor to its predecessor so the queue remains circular.

The edit screen changes title and body but preserves ID, level, state, and queue position. Its toolbar prompts before leaving if no successful save has occurred. Empty edited titles are normalized to `null`.

### Import / Export

`ImportExportActivity` uses the system clipboard:

- Export serializes the current set as JSON and copies it as plain text.
- Import reads plain text, asks for destructive confirmation, replaces the named set represented by the payload, validates the linked structure, and selects that set on success.

The warning says the current database will be overwritten, but the repository actually targets the collection named in the payload rather than deleting every collection. The exact format and failure semantics are specified in [DATA_FORMAT.md](DATA_FORMAT.md).

## Architecture

### Component flow

```text
XML + View Binding
       │
Activity / Fragment
       │ observes LiveData, sends user intents
       ▼
ViewModel
       │ calls suspend functions
       ▼
Business service
       │ uses domain Card objects
       ▼
CardRepository interface
       │ maps domain objects and maintains queue links
       ▼
Room CardDao ──────────────> SQLite card table

SharedPreferences ─────────> current/previous set, bookmark positions
Android TextToSpeech ──────> spoken prompt and answer
```

The app is a single Gradle module and organizes source by feature. It resembles MVVM with a repository layer, though boundaries are pragmatic rather than strict: for example, import/export accesses the repository directly, while its activity owns clipboard and confirmation UI.

### Presentation state

The study screen uses a sealed `CardViewState`:

| State | Meaning |
| --- | --- |
| `Init` | Dealer is ready; show the selected set before study starts. |
| `ShowTitleOnly` | A card is ready with prompt/answer text and sizes. |
| `Freeze` | An asynchronous transition is running; prevent another rating. |
| `CollectionEmpty` | The selected set contains no regular cards. |
| `CollectionMissing` | There is no valid current set. |

The edit/delete list has `Loading` and `Content` states. Snackbar messages, count changes, voices, and save completion use `Event<T>` to avoid re-consuming a one-shot value after observation restarts.

### Dependency injection

`AppApplication` is annotated with `@HiltAndroidApp`. `AppModule` provides:

- `CardDatabase` and `CardDao`
- `CardRepositoryDatabase` bound as `CardRepository`
- `CardDealerImpl` bound as `CardDealer`
- `CardEditor` bound as `CardEditorActions`
- `DailyCounter` bound as `ReviewCounter`
- A singleton `CurrentCollectionManager`

Activities/fragments with injected ViewModels are `@AndroidEntryPoint`; ViewModels are `@HiltViewModel` and receive collaborators via constructor injection.

### Concurrency

Room and business APIs are main-safe and move blocking database work to `Dispatchers.IO`. ViewModels use `viewModelScope`; short UI-bound suspend calls use the owning activity's `lifecycleScope`. There are no ownerless presentation scopes, so destroying the owner cancels its work, and cancellation is rethrown rather than converted into an ordinary import failure. LiveData observers and lifecycle scopes update Android views, clipboard, and `Snackbar`s on the main thread.

Stateful work is explicitly coordinated. The study ViewModel serializes initialization, loading, and review/reload operations and ignores a second rating while the first is frozen. Delete search cancels its preceding query and serializes searches with deletion. Settings mutations and current/previous collection changes use mutexes so concurrent requests cannot interleave their state writes. Dealer setup awaits bookmark initialization before exposing the initialized state. Activities also shut down their Text-to-Speech instances on destruction.

## Queue and scheduling design

### Physical queue model

Each collection is one circular linked list represented by `previous` pointers. To move forward from a card, the DAO finds the row whose `previous` equals the current ID. The zero card is the sentinel:

```text
zero ──next──> top ──next──> ... ──next──> last ──next──> zero
  ^                                                     │
  └──────────── zero.previous = last.id ────────────────┘
```

This makes the top card `getNextById(zero.id)`. Burying the top card rewires three `previous` pointers: the old second card follows zero, the insertion successor follows the top card, and the top card follows the selected insertion anchor.

### Bookmarks

Bookmark positions approximate review intervals as queue depths. Defaults are:

```text
level:     0   1   2    3    4    5     6     7     8      9
position: 20  30  50  100  200  500  1000  2000  5000  10000
```

The bookmark preference is global rather than per set. During dealer setup, each position is resolved to the logical ID at that queue position; positions beyond the end are capped with the last card. If the queue has only the sentinel, lookup resolves to its empty logical ID, though studying the set reports it as empty.

### Rating transition

Let `L` be the current level and `S` the current remembered state.

| Rating | Prior state | Level/state update | Reinsert depth |
| --- | --- | --- | --- |
| Remembered | `S = true` | `L := min(L + 1, 10)`; state stays true | Bookmark for the new level, capped by available bookmark IDs. |
| Remembered | `S = false`, normally | Level stays; `S := true` | Bookmark for the current level. |
| Remembered | `S = false` and `L = 1` | `L := 2`; state stays false for one transition | Bookmark for level 1. |
| Forgotten | Either | `L := max(L - 1, 0)`; `S := false` | Bookmark for level 0. |

The special transition at level 1 coordinates with side reversal: the card advances to the reversal threshold without immediately treating the reversed direction as remembered.

### Bijective prompt selection

For ordinary sets, title is always the prompt and body is always the answer. For a set whose zero-card metadata field 4 is exactly `bijective`, the sides are swapped when either condition is true:

```text
(state is true  and level >= 2)
or
(state is false and level >= 1)
```

When swapped, font sizes swap with the content. This makes learned cards test recall in the reverse direction while the state/level transition provides a lower threshold for a recently missed reverse prompt.

### Daily counter

The current set stores a study date and known-card count in its zero card. The effective study date is local time minus three hours, so the counter rolls over at 03:00 rather than midnight. Loading the study screen resets a stale date to zero; rating remembered increments the count. Revealing or rating forgotten does not.

## Persistence design

Room database `card-database` has one table, `card`. Domain `Card` objects omit the collection suffix; `CardEntity` values include it and add a `coll` column. This convention lets exported logical IDs remain independent of the destination set name.

Current and previous collection names are cached in `CurrentCollectionManager` and persisted in default `SharedPreferences` under `coll` and `coll_previous`. Setting a collection verifies its zero card exists; stale selections become null.

Set-level metadata is stored in the zero card's body as positional comma-separated values. This is compact but fragile and unversioned. Any future redesign needs an explicit migration because old databases and exported JSON already contain this representation.

See [DATA_FORMAT.md](DATA_FORMAT.md) for field-level definitions and examples.

## Error handling and integrity

The business layer distinguishes a missing set from an empty set with `CardDealer.CollectionMissingException` and `CollectionEmptyException`. The study ViewModel explicitly renders both cases during initialization/loading.

Import catches malformed JSON and returns failure. Repository import transactionally replaces only the destination set, validates that the replacement forms a single cycle containing a zero card, and commits only after validation succeeds. An exception or invalid cycle rolls the whole replacement back automatically. Other repository calls assume the requested IDs exist; Room lookup failures may propagate.

Add, delete, empty, import, and review/bury mutations use Room transactions so their linked rows change together or not at all. A review combines the top card's level/state write and queue move in one repository operation; `CardDealerImpl` publishes its calculated in-memory bookmarks only after that operation succeeds.

## Quality attributes and design constraints

### Offline and privacy

The app declares no network permission and uses local Room storage, preferences, clipboard, and platform TTS. Clipboard exports can contain the complete contents of a set; users should treat them as potentially sensitive.

### Compatibility

- Minimum SDK is 34, so the application is Android 14+ only; compile and target SDK are 36.
- The build uses AGP's built-in Kotlin support and KSP for Room and Hilt code generation.
- The UI is locked to portrait orientation.
- Database schema version is 1 and no migrations are defined.
- Release minification is disabled.

### Accessibility and localization

TTS and hardware media controls enable an audio-oriented workflow. However, many visible strings remain hard-coded in layouts/activities, content descriptions are sparse, and only the default resource language exists. New UI work should use string resources, meaningful accessibility labels, scalable text, and validated focus order.

## Known limitations and planned direction

The prioritized backlog is maintained in [`TODO.md`](../TODO.md). The most important remaining architectural work is versioning collection metadata without losing legacy data and defining backup/restore rules. Product work includes Storage Access Framework import/export, stronger boundary validation, collection behavior controls, and accessibility cleanup.

These items describe current debt, not permission to change compatibility silently. Scheduling or storage redesigns should include migrations, focused tests, and updates to both design documents. Completed work belongs in `CHANGELOG.md`, not in the active backlog.
