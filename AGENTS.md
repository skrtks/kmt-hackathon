# AGENTS.md

This file provides guidance to coding agents (Claude Code, Cursor, Copilot, etc.) when working with code in this repository.

## Commands

```bash
# Desktop run
./gradlew :composeApp:run

# Android debug build
./gradlew :composeApp:assembleDebug

# All tests (all targets)
./gradlew :composeApp:allTests

# All tests with a workspace-local Gradle cache
GRADLE_USER_HOME=/tmp/kmt-hackathon-gradle ./gradlew :composeApp:allTests

# Single test class — JVM/Desktop target (primary dev target)
./gradlew :composeApp:jvmTest --tests "com.samex.kmt_hackathon.ComposeAppCommonTest"

# Single test class — Android target
./gradlew :composeApp:testDebugUnitTest --tests "com.samex.kmt_hackathon.ComposeAppCommonTest"

# iOS — open iosApp/iosApp.xcodeproj in Xcode and run from there
```

No lint tooling configured (no Detekt/Ktlint). Kotlin official code style enforced via `kotlin.code.style=official` in `gradle.properties`.

## Architecture

**Kotlin Multiplatform + Compose Multiplatform** targeting Android, iOS, Desktop (JVM). Single module: `:composeApp`.

All shared code lives in `composeApp/src/commonMain/`. Platform-specific code uses the `expect/actual` pattern:
- `Platform.kt` — `expect fun getPlatform(): Platform`
- `Platform.android.kt` / `Platform.ios.kt` / `Platform.jvm.kt` — `actual` implementations

Platform entry points all call the shared `App()` composable:
- Android: `MainActivity` → `setContent { App() }`
- Desktop: `main.kt` → `Window { App() }`
- iOS: `MainViewController.kt` → `ComposeUIViewController { App() }`

UI uses **Material3** via Compose Multiplatform. Lifecycle/ViewModel from `androidx.lifecycle` works cross-platform via KMP-compatible artifacts.

Current first-version app architecture:

- `core/DomainModels.kt` contains persisted user data, saved places/commutes, schedule, session, leave-window, and notification-plan models.
- `core/WatchEngine.kt` owns walking-time calculation, leave-window calculation, merge behavior, schedule validation, and notification-plan generation.
- `core/TransitAppModel.kt` is the shared state holder for onboarding, saved commutes, settings, active watch sessions, and UI actions.
- `core/PlatformServices.kt` defines `expect` platform hooks for key-value persistence, notifications, and time.
- Android/iOS/JVM actual implementations live under the matching platform source sets.
- Android notifications use `AlarmManager` in `AndroidPlatformServices.kt`. When exact pending-intent alarms are allowed, the app uses them for process-independent delivery. On newer Android installs where `SCHEDULE_EXACT_ALARM` is denied by default, it also schedules a permission-free in-process exact alarm plus an inexact broadcast fallback so near-term smoke tests still fire while preserving a fallback if the process is gone.

### Mock Transit Data

Early development uses a static in-memory fixture:

- `composeApp/src/commonMain/kotlin/com/samex/kmt_hackathon/transit/MockTransitData.kt`
- `composeApp/src/commonTest/kotlin/com/samex/kmt_hackathon/transit/MockTransitDataTest.kt`
- `docs/mock-transit-data.md`

The mock dataset includes stops, bus/tram/metro lines, API-style directions/headsigns, and fixed weekday departure times. It deliberately includes shared stops and closely aligned departures so leave-window merging behavior can be developed without live transit data.

## Product Context

Transit leave-window companion app. Core concept: given a transit departure time, walking time, and desired early arrival buffer, compute when the user should leave and notify them. Key domain models to implement: **SavedPlace**, **SavedCommute**, **WatchSession**. Platform-specific adapters needed for notifications, background scheduling, and location — these go in `androidMain`/`iosMain`/`jvmMain` with `expect/actual` interfaces in `commonMain`.

See `product-spec.md` for full MVP scope and timing formula. Treat `product-spec.md` as the source of truth for product decisions and expected behaviour of the app. When implementation details are underspecified, prefer conservative choices that match the spec and existing architecture; ask the user only when the ambiguity changes product behavior or creates meaningful implementation risk.

## Plan Versioning

Implementation plans live in `plan.md` unless a more specific plan file is requested. Plans must include a `major.minor.patch` version, target app version, status, last-updated date, and a version history. Increment the plan version as follows:

- `major`: target or release scope changes substantially.
- `minor`: implementation approach, product behavior, interfaces, or acceptance criteria change.
- `patch`: clarifications, typo fixes, formatting, or non-behavioral edits.
