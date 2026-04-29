# AGENTS.md

This file provides guidance to coding agents (Claude Code, Cursor, Copilot, etc.) when working with code in this repository.

## Commands

```bash
# Desktop run
./gradlew :composeApp:run

# Android debug build
./gradlew :composeApp:assembleDebug

# Wear OS debug build
./gradlew :wearApp:assembleDebug

# All tests (all targets)
./gradlew :composeApp:allTests

# Shared domain/transit tests
./gradlew :shared:allTests

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

**Kotlin Multiplatform + Compose Multiplatform** targeting Android, iOS, Desktop (JVM), plus a separate Wear OS Android app.

Modules:
- `:shared` — KMP library for UI-free domain, transit data, timing engine, repository contracts, and platform service contracts.
- `:composeApp` — phone/tablet/desktop/iOS Compose Multiplatform app.
- `:wearApp` — Wear OS Android companion app using Wear Compose Material3.

Shared domain code lives in `shared/src/commonMain/`. The full-size app UI and `TransitAppModel` currently live in `composeApp/src/commonMain/`. Platform-specific code uses the `expect/actual` pattern:
- `Platform.kt` — `expect fun getPlatform(): Platform`
- `Platform.android.kt` / `Platform.ios.kt` / `Platform.jvm.kt` — `actual` implementations

Platform entry points all call the shared `App()` composable:
- Android: `MainActivity` → `setContent { App() }`
- Desktop: `main.kt` → `Window { App() }`
- iOS: `MainViewController.kt` → `ComposeUIViewController { App() }`
- Wear OS: `WearMainActivity` → `setContent { WearApp(...) }`

The full-size app UI uses **Material3** via Compose Multiplatform. The Wear OS app uses **Wear Compose Material3** and must stay watch-native rather than reusing the full-size `App()` UI.

Design-system notes:
- `design.md` captures the current Material 3 / M3 Expressive design system and how it maps to the implemented app.
- `style.md` is the day-to-day UI/content style guide. Keep implementation copy, layout rules, theme rules, and component usage aligned with it.
- `reference/tack-android` is the current local visual reference when present. Do not copy GPL source directly; use it for design and interaction inspiration.
- `composeApp/src/commonMain/kotlin/com/samex/kmt_hackathon/LeaveTheme.kt` is the shared theme entry point. It defines the selectable MVP color themes (`Sunrise`, `Lagoon`, `Grove`, `Berry`), theme-aware watch status colors, shape scale, and typography overrides.
- `App.kt` currently owns the first reusable expressive UI primitives (`ActiveWatchHero`, `LeaveWindowProgress`, `RouteChip`, `CommuteSummaryCard`). Prefer extracting them into dedicated UI files as the component set grows.
- The UI is intentionally flat: do not add shadows. Use color, borders, shape, spacing, and tonal surfaces for hierarchy.

Current first-version app architecture:

- `shared/src/commonMain/.../core/DomainModels.kt` contains persisted user data, saved places/commutes, schedule, session, leave-window, and notification-plan models.
- `shared/src/commonMain/.../core/WatchEngine.kt` owns walking-time calculation, leave-window calculation, merge behavior, schedule validation, and notification-plan generation.
- `shared/src/commonMain/.../core/PlatformContracts.kt` defines service contracts for persistence, notifications, time, haptics, and live activity surfaces.
- `core/TransitAppModel.kt` is the shared state holder for onboarding, saved commutes, settings, active watch sessions, and UI actions.
- Home is the single active-watch destination. Active sessions render as a full dashboard section on Home; there is no separate Watch screen route.
- Settings is a normal page route opened from Home and closed with a back button. It uses a horizontal slide/fade transition, not a sheet/popover.
- Commute setup and edit allow exactly one line/direction selection. The persisted `SavedCommute.selections` remains a list for compatibility, but current UI/model behavior enforces a single selected line-direction.
- The active watch card headline states:
  - `Leave at <time>` before the leave window opens.
  - `Leave now` while the window is open.
  - `Final call` at the final-call minute.
  - `Departure in <countdown>` after the user taps `I'm leaving`.
- Tapping `I'm leaving` captures the selected departure/group, silences notifications, shows the departure countdown, and ends the watch when that departure time is reached. Auto-start for that commute is suppressed until the current schedule window ends so it does not immediately restart.
- Edge-to-edge visuals are allowed at the app root, but scrollable screens need bottom scroll tail space for Android navigation controls. Prefer scroll content insets/spacers over root bottom padding when solving nav-bar overlap.
- `core/PlatformServices.kt` defines `expect` platform hooks for key-value persistence, notifications, time, live activity updates, haptics, and Wear-device detection in `:composeApp`.
- Android/iOS/JVM actual implementations live under the matching platform source sets.
- Android notifications use `AlarmManager` in `AndroidPlatformServices.kt`. When exact pending-intent alarms are allowed, the app uses them for process-independent delivery. On newer Android installs where `SCHEDULE_EXACT_ALARM` is denied by default, it also schedules a permission-free in-process exact alarm plus an inexact broadcast fallback so near-term smoke tests still fire while preserving a fallback if the process is gone.
- `core/WatchEngine.WatchCopy` is the single source of truth for notification titles and Live Activity titles. Active watch card headlines are currently formatted in `App.kt` because they include UI-specific countdown and leave-time presentation. Notification body copy is shared with the Live Activity body via `WatchEngine.notificationBody` and `liveActivitySnapshot`.

### Wear OS

- `:wearApp` is a separate Wear OS APK. Do not add `android.hardware.type.watch` with `required=false` to the phone manifest.
- The Wear manifest declares `<uses-feature android:name="android.hardware.type.watch" />` and `com.google.android.wearable.standalone=false` for the companion MVP.
- The first Wear surface reads the phone-published active watch `LiveActivitySnapshot` from the Wear Data Layer path `/transit-live-activity`.
- Keep the phone app as the source of truth for saved commutes, active sessions, notification scheduling, and transit refresh until a standalone watch product scope is explicitly planned.
- Wear UI should use black background, Wear Material3 components, `TimeText`, 48dp touch targets, and shallow vertical flows.

### Live Activity (iOS / watchOS Smart Stack)

- `core/PlatformServices.kt` exposes `LiveActivityController` (with `NoopLiveActivityController` for Android/JVM) and `LiveActivitySnapshot` / `LiveActivityEndReason` domain types.
- `TransitAppModel` calls `start` / `update` / `end` on the controller as the active watch session changes (manual start, auto-start, status transitions, skip, "I'm leaving," schedule end, session restore on launch).
- iOS implementation: `iosMain/.../IosLiveActivityController.kt` holds a Swift-registered `LiveActivityBridge`. The Swift side lives in `iosApp/`:
  - `iosApp/iosApp/TransitLiveActivityBridge.swift` adopts `LiveActivityBridge`, drives `ActivityKit`, and is registered from `iOSApp.swift` at launch.
  - `iosApp/TransitLiveActivity/` is a separate Widget Extension target containing `TransitWatchAttributes.swift`, `TransitLiveActivityWidget.swift`, and the extension `Info.plist`. **Manual Xcode step:** add this folder to `iosApp.xcodeproj` as a "Widget Extension" target and add it as an embedded content to the iOS app target. The Kotlin-side framework (`ComposeApp`) does not need to depend on the widget extension.
  - The iOS app `Info.plist` enables `NSSupportsLiveActivities` and `NSSupportsLiveActivitiesFrequentUpdates`.
- watchOS surface is iOS Live Activity mirroring via Smart Stack (watchOS 10+); there is no standalone watchOS app target.
- `Text(timerInterval:)` drives the countdown; `WatchStopped` end reason uses a 2-minute lingering dismissal, others dismiss immediately.

### Mock Transit Data

Early development uses a static in-memory fixture:

- `shared/src/commonMain/kotlin/com/samex/kmt_hackathon/transit/MockTransitData.kt`
- `shared/src/commonTest/kotlin/com/samex/kmt_hackathon/transit/MockTransitDataTest.kt`
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
