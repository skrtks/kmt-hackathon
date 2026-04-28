# AGENTS.md

This file provides guidance to coding agents (Claude Code, Cursor, Copilot, etc.) when working with code in this repository.

## Commands

```bash
# Desktop run
./gradlew :composeApp:run

# Android debug build
./gradlew :composeApp:assembleDebug

# All tests (all targets)
./gradlew :composeApp:test

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

## Product Context

Transit leave-window companion app. Core concept: given a transit departure time, walking time, and desired early arrival buffer, compute when the user should leave and notify them. Key domain models to implement: **SavedPlace**, **SavedCommute**, **WatchSession**. Platform-specific adapters needed for notifications, background scheduling, and location — these go in `androidMain`/`iosMain`/`jvmMain` with `expect/actual` interfaces in `commonMain`.

See `product-spec.md` for full MVP scope and timing formula. Treat `product-spec.md` as the only source of truth for product decisions and expected behaviour of the app. When in doubt, always prompt the user with questions and wait for their clarifications.
