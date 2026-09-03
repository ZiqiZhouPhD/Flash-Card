# Flash Card

Flash Card is an offline Android study app with multiple named card sets, level-based queue scheduling, optional two-way prompts, text-to-speech study, a daily counter, and clipboard JSON backup/restore.

## Current features

- Study a selected set one card at a time.
- Reveal an answer, then rate the card remembered or forgotten.
- Reinsert cards at increasing queue depths as their learning level changes.
- Create, select, revisit, and delete named sets.
- Add cards and search by title prefix to edit or delete them.
- Use separate Android TTS voices for the two card sides.
- Study with hardware media controls in audio mode.
- Import and export a complete set as clipboard JSON.

## Project documentation

- [App design](docs/APP_DESIGN.md) — product behavior, screens, architecture, scheduling, and known constraints
- [Data format](docs/DATA_FORMAT.md) — circular-list invariants, zero-card metadata, Room representation, and JSON interchange
- [Testing guide](docs/TESTING.md) — test layers, package layout, commands, and safe device checks
- [Agent and contributor guide](AGENTS.md) — repository map, build checks, architectural rules, and change discipline
- [Project backlog](TODO.md) — authoritative priorities and completion criteria
- [Changelog](CHANGELOG.md) — notable unreleased changes and verification status

## Development setup

The project is a single Kotlin/Android module using XML layouts, View Binding, AGP's built-in Kotlin support, KSP, Hilt, Room, LiveData/ViewModel, coroutines, Material Components, and Gson.

Requirements:

- Android Studio with Android SDK 36 and Build Tools 36.0.0 installed
- JetBrains JDK 21 (the current Gradle JVM criteria use it)
- An API 34+ emulator/device for instrumented tests

From Windows PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
.\gradlew.bat lint
```

Use `./gradlew` instead of `.\gradlew.bat` on macOS or Linux. The test suite includes focused scheduling tests plus isolated Room and launch checks. With one authorized Android device connected, run the non-destructive in-place device check:

```powershell
.\scripts\device-smoke-test.ps1
```

The script never uninstalls or clears the application package. It updates with `adb install -r`, verifies that the original install timestamp is unchanged, runs instrumented tests, relaunches the app, checks Android's crash buffer, and removes only its temporary test package.

## Creating an importable set

Sets can be created in the app, or generated from a UTF-8 text file containing one `word<TAB>definition` pair per line:

```powershell
python .\raw-vocab-to-meta-tool.py .\vocab.txt --output .\vocab.json
```

The generated JSON follows the app's clipboard import format. Copy the JSON text, then open **Settings → Import / Export → Import**. See [the data-format reference](docs/DATA_FORMAT.md) before generating or editing payloads outside the app.

## Roadmap

See [TODO.md](TODO.md) for the prioritized project backlog. It is the authoritative location for open work; completed changes are recorded in [CHANGELOG.md](CHANGELOG.md).
