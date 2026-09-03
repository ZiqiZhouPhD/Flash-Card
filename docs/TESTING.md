# Testing guide

## Strategy

Flash Card uses a lightweight test pyramid. Tests are separated by the boundary they exercise, and each test class mirrors the package of its production subject.

| Layer | Source set | Purpose | Typical frequency |
| --- | --- | --- | --- |
| Unit | `app/src/test` | Pure Kotlin business rules | Every change |
| Integration | `app/src/androidTest` | Android components such as Room using isolated resources | Data-layer changes and device checks |
| UI smoke | `app/src/androidTest` | A few critical application entry points | Before release and during device checks |

Avoid replacing focused tests with a large end-to-end suite. TTS voice quality, hardware media controls, and visual appearance still require a short manual check on physical hardware.

## Layout

```text
app/src/test/java/com/ziqiphyzhou/flashcard/
└── card_main/business/
    └── CardReviewPolicyTest.kt

app/src/androidTest/java/com/ziqiphyzhou/flashcard/
├── card_database/data/repository/database/
│   └── CardRepositoryDatabaseIntegrationTest.kt
└── card_main/presentation/
    └── MainActivitySmokeTest.kt

scripts/
└── device-smoke-test.ps1
```

`CardReviewPolicyTest` is a fast JVM test. `CardRepositoryDatabaseIntegrationTest` creates a fresh in-memory database for every test, injects SQLite trigger failures to verify transaction rollback, and never opens the installed application's database. `MainActivitySmokeTest` verifies that the real entry activity can launch and render its primary controls without changing study data.

## Naming and isolation

- Mirror the production package so a test and its subject are easy to find together.
- Use `*Test` for JVM tests, `*IntegrationTest` for Android component tests, and `*SmokeTest` for critical UI paths.
- Name methods as behavior plus expected outcome, such as `collectionOperationsRemainIsolated`.
- Give each test its own state. Do not depend on test order, sleeps, the network, or another test's cleanup.
- Prefer fakes or pure functions for business rules. Use Android instrumentation only when the Android framework is part of the contract.
- Never point repository integration tests at the user's on-device database.

## Local commands

Run the smallest relevant target while iterating:

```powershell
.\gradlew.bat :app:testDebugUnitTest `
    --tests com.ziqiphyzhou.flashcard.card_main.business.CardReviewPolicyTest
```

Run all JVM tests and compile the Android tests:

```powershell
.\gradlew.bat test
.\gradlew.bat :app:assembleDebugAndroidTest
```

Run lint and build the APK before handoff:

```powershell
.\gradlew.bat :app:lintDebug :app:assembleDebug
```

## Physical-device smoke test

With exactly one authorized device connected:

```powershell
.\scripts\device-smoke-test.ps1
```

The script deliberately avoids `adb uninstall` and `pm clear` for the Flash Card package. It:

1. Builds and runs JVM tests.
2. Updates the app with `adb install -r`.
3. Verifies that `firstInstallTime` did not change.
4. Installs the separate test APK and runs the integration/UI tests.
5. Launches the app and checks Android's crash buffer.
6. Removes only the temporary test package.

If the installed application has a different signing certificate, the update fails without uninstalling it. Preserve this behavior; never work around a signature mismatch by deleting the installed package when user data must survive.

## Where new tests belong

- Pure scheduling, parsing, counters, or metadata rules: `src/test` beside the business class.
- DAO, Room, Android preferences, or framework integration: `src/androidTest` beside the implementation, with isolated storage.
- Critical user journey or application startup: a narrowly scoped `*SmokeTest` under the presentation package.
- Cross-device visual, TTS, and hardware-control behavior: the documented manual release checklist, not a brittle automated test.
