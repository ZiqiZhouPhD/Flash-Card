# Changelog

All notable changes to Flash Card are documented here. The format follows Keep a Changelog categories; the project has not assigned a new release version yet, so these changes remain under `Unreleased`.

## Unreleased

### Added

- Repository-wide contributor guidance in `AGENTS.md`.
- A root `TODO.md` as the authoritative, prioritized project backlog.
- Architecture, data-format, and testing documentation under `docs/`.
- Focused JVM coverage for card-review scheduling behavior.
- In-memory Room integration coverage for circular-list integrity, collection isolation, export/import round trips, and transaction rollback under injected SQLite failures.
- Focused ViewModel coroutine tests for duplicate review suppression, initialization ordering, stale-search cancellation, and duplicate deletion suppression.
- A UI launch smoke test for the main activity.
- A reusable PowerShell device-smoke runner that updates the app in place, verifies that installation data was preserved, runs Android tests, checks the crash buffer, and removes only its temporary test package.
- Versioned Room schema output for future migration validation.

### Changed

- Upgraded the build to Gradle 9.6.1 and Android Gradle Plugin 9.4.0.
- Adopted AGP's built-in Kotlin support and migrated Room and Hilt code generation from KAPT to KSP.
- Updated AndroidX, Material, Room, Hilt, Gson, and Android test dependencies and centralized their versions in the Gradle catalog.
- Raised compile and target SDK to 36 while retaining minimum SDK 34.
- Raised Java source and bytecode compatibility from 8 to 17.
- Updated the Foojay resolver and standardized the Gradle daemon on JetBrains JDK 21.
- Simplified application dependencies by removing unused Cronet and Compose artifacts, duplicate declarations, and obsolete annotation processors.
- Replaced asynchronous bookmark initialization with an awaited setup step before review actions can proceed.
- Made add, delete, empty, import, and review/bury repository mutations atomic with Room transactions.
- Replaced import's detached deletion coroutine and manual snapshot restoration with transactional collection replacement and automatic rollback.
- Replaced ownerless presentation coroutines with `viewModelScope` or `lifecycleScope`, keeping Android view and clipboard access on the main thread.
- Treated the completed coroutine-ownership work (`CONC-001`) as P0 release-safety work.
- Serialized study, delete/search, Settings mutation, and current-collection state changes; replaceable searches now cancel their predecessor.
- Added testable business boundaries for card editing and review counting.
- Reorganized tests by production package and scope using `Test`, `IntegrationTest`, and `SmokeTest` naming.
- Expanded the README with product behavior, setup, documentation, testing, and roadmap information.

### Fixed

- Prevented one-card collections from calculating a negative insertion-bookmark index during reversed-prompt scheduling.
- Fixed emptying a populated collection so it reliably leaves one self-linked zero card.
- Prevented failed review writes from advancing the dealer's in-memory bookmarks.
- Prevented duplicate ratings or deletions from starting overlapping mutations and stale searches from replacing newer results.
- Preserved coroutine cancellation through import error handling and released Text-to-Speech resources with their activities.
- Removed unused Compose imports left in XML/View Binding activities.

### Verification

- JVM tests, Android test APK compilation, lint, and debug assembly pass.
- Room and launch instrumentation tests pass on a Samsung SM-A146U1.
- Settings navigation and lifecycle-owned initialization render without a crash on the Samsung device.
- An in-place device update preserved the original app installation and existing collection data.
