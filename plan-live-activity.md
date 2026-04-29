# watchOS Live Activity Plan

Plan version: 0.2.1
Target app version: post-MVP iOS/watchOS companion surface
Status: Refresh hardening implemented; physical-device verification pending
Last updated: 2026-04-29

## Versioning

Plan versions use `major.minor.patch`.

- Increment `major` when the plan target or release scope changes substantially.
- Increment `minor` when implementation approach, product behavior, interfaces, or acceptance criteria change.
- Increment `patch` for clarifications, typo fixes, formatting, or non-behavioral edits.

## Version History

- `0.2.1` - Reapplied refresh hardening after rebase: Boolean ActivityKit acknowledgements, retryable failed starts/ends, serialized Swift bridge operations, stale-date fallback, watch-stopped final copy, widget wall-clock display, and regression coverage.
- `0.2.0` - Added refresh hardening scope for suspended-app transitions, failed ActivityKit requests, lifecycle races, stale Live Activity content, and active-commute edit refreshes.
- `0.1.2` - Implementation: shared `WatchCopy` + `LiveActivitySnapshot` + `LiveActivityController`, `TransitAppModel` wiring, Android/JVM no-ops, iOS bridge + Swift widget extension sources. Remaining manual step: add `iosApp/TransitLiveActivity/` as a Widget Extension target inside Xcode.
- `0.1.1` - Locked defaults: Smart Stack mirror only (no standalone watchOS target), `Text(timerInterval:)` countdowns, 2-minute lingering dismissal for watch-stopped, Live Activity surfaces from session start (`GetReady` onward). Removed Open Questions section accordingly. Status moved to Approved.
- `0.1.0` - Initial plan to add a Live Activity that mirrors existing notification content for the active watch session and surfaces it on watchOS via Smart Stack.

## Scope Clarification

There is no dedicated "watchOS Live Activity" API. Live Activities are an iOS feature delivered via `ActivityKit`. On a paired Apple Watch they appear automatically in the Smart Stack and as a complication-style row, mirrored from the iPhone Live Activity. This plan therefore:

- Adds a single iOS Live Activity (`ActivityKit` + `WidgetKit` widget extension) driven by the Kotlin Multiplatform shared engine.
- Verifies the Live Activity renders correctly on watchOS Smart Stack (no separate watchOS target, no `WatchKit` app).
- Defers a standalone watchOS app target (`watchOS Extension`) as a follow-up if/when independent watch features are requested.

If the user actually wants a fully native, standalone watchOS app target, that is a different scope and requires its own plan.

## Summary

Add an iOS Live Activity for the active watch session whose title, body, and per-state copy match the existing `NotificationPlan` output produced by `WatchEngine` in `commonMain`. The Live Activity starts when a watch session starts (manual or auto-start), updates as the active leave-window group transitions through `GetReady → LeaveNow → FinalCall`, and ends when the session ends, the user taps "I'm leaving," skips the active group, or the schedule window closes.

The Live Activity reuses the same shared model that drives notifications and the watch screen, so the user sees identical wording on the lock screen banner, in Dynamic Island, in the iPhone Live Activity card, and on the paired Apple Watch Smart Stack.

## Key Changes

### Shared (`commonMain`)

- Add a `LiveActivitySnapshot` data class that captures the current rendering state of the active session:
  - `groupId`, `status: WatchStatus`, `title`, `body`, `headline` (matches notification copy + watch screen headline), `departureTimeMinutes`, `windowOpenMinutes`, `finalCallMinutes`, `stopName`, `lineLabel`, `directionHeadsign`, `walkingMinutes`.
  - Title/body fields are produced by the same helpers used for notifications so wording cannot drift.
- Refactor the existing notification copy helpers in `WatchEngine` so the strings used for `NotificationPlan.title`/`body` and for the Live Activity come from one shared function. Concretely:
  - Extract `windowOpenTitle()`, `finalCallTitle()`, `notificationBody(group)` as the single source of truth.
  - `LiveActivitySnapshot` picks the right title/body for the current `WatchStatus`.
- Add a `LiveActivityController` interface in `core/PlatformContracts.kt`:
  - `start(snapshot: LiveActivitySnapshot): Boolean`
  - `update(snapshot: LiveActivitySnapshot): Boolean`
  - `end(snapshot: LiveActivitySnapshot?, reason: LiveActivityEndReason): Boolean` where reason is one of `SessionEnded`, `Skipped`, `Leaving`, `WatchStopped`, `ScheduleEnded`.
  - `isSupported(): Boolean` and `isActivityRunning(): Boolean`.
- Extend the `expect object PlatformServices` to expose `liveActivityController(): LiveActivityController`.
- Hook `TransitAppModel` into the controller:
  - On session start: build snapshot from current `LeaveWindowGroup` and call `start`.
  - On any state transition that changes the active group, status, skip set, leaving flag, or recomputed windows: call `update`.
  - On session end / skip-of-active-group / "I'm leaving" / schedule end / watch-stopped: call `end` with the matching reason.
  - Only remember a Live Activity snapshot after the controller reports success. Keep failed starts and ends retryable on later ticks.
- All snapshot construction lives in shared code so Android, iOS, and JVM see the same logic; only the platform controllers differ.

### iOS (`iosMain` + `iosApp/`)

- Add a Swift Widget Extension target `TransitLiveActivity` to `iosApp.xcodeproj`:
  - Declares an `ActivityAttributes` struct (`TransitWatchAttributes`) plus `ContentState` matching the `LiveActivitySnapshot` fields.
  - Provides a `Widget` with `ActivityConfiguration` covering:
    - Lock screen / banner presentation (compact + expanded).
    - Dynamic Island compact, minimal, and expanded regions.
    - Smart Stack rendering on watchOS (uses the same `ActivityConfiguration`; Apple mirrors automatically).
- Add the widget extension target to the Xcode project and configure `NSSupportsLiveActivities` and `NSSupportsLiveActivitiesFrequentUpdates` in the iOS app's `Info.plist`.
- Implement `IosLiveActivityController` in `composeApp/src/iosMain/kotlin/com/samex/kmt_hackathon/core/IosLiveActivityController.kt`:
  - Bridges `LiveActivitySnapshot` to `TransitWatchAttributes.ContentState` via Kotlin/Native interop or a thin Swift bridge exposed through the framework.
  - Calls `Activity.request(...)`, `activity.update(...)`, and `activity.end(...)` from `ActivityKit` (Swift bridge). Kotlin code calls into a small `@objc` helper class because `ActivityKit` types are Swift-only.
  - Tracks the currently running activity via `Activity<TransitWatchAttributes>.activities` so it can recover after relaunch.
  - On `isSupported`, checks `ActivityAuthorizationInfo().areActivitiesEnabled` and iOS version (>= 16.2).
  - Serializes ActivityKit update/end/request work so replacement, skip, and relaunch paths cannot overlap older operations.
  - Sets a stale date past the departure and renders expired copy when the widget is stale or the departure has passed.
- Wire `PlatformServices.liveActivityController()` to return the iOS controller.
- On app launch, reconcile state: if `userData.activeSession` exists but no live activity is running, restart one from the current snapshot. If a live activity is running but no active session exists, end it.

### Android and JVM

- Add `AndroidLiveActivityController` and `JvmLiveActivityController` no-op implementations that report `isSupported() = false`. Live Activities are an iOS-only platform concept; Android's roughly equivalent surface (ongoing notification with `setOngoing` + `MediaStyle`-like progress) is out of scope for this plan.

### Notification copy parity

- Update existing `NotificationPlan` construction to call the shared title/body helpers, so notification + Live Activity wording remain identical without duplicate strings.
- Cover the `WatchStopped` end-state copy in the Live Activity end transition (matches the existing watch-stopped notification text).

## UI And Behavior

Live Activity surfaces, in order of state:

- **Pre-window (`GetReady`)**: Title = "Get ready" (matches watch screen headline), body = `notificationBody(group)`. Shows departure time, leave-by minute, line + direction, walking minutes.
- **Window open (`LeaveNow`)**: Title = "Leave now" (matches existing `WindowOpen` notification title), body identical.
- **Final call (`FinalCall`)**: Title = "Final call" (matches existing `FinalCall` notification title), body identical.
- **Missed**: Live Activity ends silently (matches existing "no missed-departure notification" rule).
- **Watch stopped**: Live Activity ends with the watch-stopped reason; ends with a final content state showing the same body used by the watch-stopped notification.

Interaction:

- Tapping the Live Activity opens the app on the watch screen for the active session (same deep-link target as tapping a notification).
- No action buttons inside the Live Activity (consistent with existing notification rule "no notification action buttons").
- Skip and "I'm leaving" remain in-app actions; they update or end the activity but do not appear as activity buttons.

Smart Stack on watchOS:

- The Live Activity mirrors automatically; verify compact and expanded layouts render within Apple's watchOS dimension guidance (small leading/trailing region, two-line body cap).
- Rely only on system fonts and SF Symbols to avoid extra asset wiring in v1.

## Implementation Order

1. **Shared copy refactor**
   - Extract title/body helpers in `WatchEngine` so notifications and the Live Activity share them.
   - Add `LiveActivitySnapshot` and `LiveActivityController` in `commonMain`.
   - Unit-test snapshot construction for each `WatchStatus` and for skip/leaving/watch-stopped end reasons.
2. **State-holder integration**
   - Wire `TransitAppModel` start/update/end calls.
   - Ensure idempotent updates (no-op when snapshot unchanged) to avoid hammering ActivityKit.
   - Add reconciliation on launch.
3. **iOS widget extension**
   - Add `TransitLiveActivity` widget extension target in `iosApp.xcodeproj`.
   - Define `TransitWatchAttributes` and `ActivityConfiguration` views (lock screen, Dynamic Island, watch Smart Stack).
   - Add `Info.plist` keys.
4. **iOS controller bridge**
   - Implement Swift helper class exposed to Kotlin via the shared framework.
   - Implement `IosLiveActivityController`; back it with `Activity<TransitWatchAttributes>` lifecycle.
   - Handle authorization and unsupported-OS fallbacks.
5. **Android/JVM no-ops**
   - Add `isSupported = false` controllers; document the gap in `AGENTS.md`.
6. **Polish**
   - Verify on a paired Apple Watch in the Smart Stack.
   - Verify Dynamic Island states.
   - Verify Live Activity recovers after force-quitting and relaunching the app.
   - Update `AGENTS.md` Architecture section to note the new widget extension target and Live Activity controller.

## Test Plan

- **Unit tests (`commonTest`)**
  - `LiveActivitySnapshot` derived from a `LeaveWindowGroup` matches the corresponding `NotificationPlan` title/body for `WindowOpen` and `FinalCall`.
  - Snapshot recomputation when the user skips the active group advances to the next group, or signals end if no further groups remain.
  - "I'm leaving" produces an end snapshot with reason `Leaving`.
  - Watch-stopped path produces end snapshot with reason `WatchStopped` and uses the watch-stopped body.
- **State-holder tests**
  - Starting a session triggers exactly one `start` call.
  - Group/status transitions trigger `update` only when fields change.
  - Session end triggers `end` exactly once.
  - Launch reconciliation: pre-existing `activeSession` rebuilds the activity if none is running.
- **Manual iOS verification (Xcode)**
  - Build and run on iPhone simulator (iOS 17+) with Live Activities enabled; confirm lock screen + Dynamic Island layouts.
  - Run on a physical iPhone paired with an Apple Watch; verify Smart Stack rendering and tap-through.
  - Force-quit the app mid-session; reopen and confirm the activity is restored.
  - Toggle Live Activities off in Settings; verify the app continues to function and notifications still fire.
- **Platform checks**
  - `GRADLE_USER_HOME=/tmp/kmt-hackathon-gradle ./gradlew :composeApp:allTests`.
  - `./gradlew :composeApp:assembleDebug` (Android still compiles with no-op controller).
  - Open `iosApp/iosApp.xcodeproj`, build app + widget extension targets, run on device.

## Assumptions

- Live Activities target iOS 16.2+ and watchOS 10+ (Smart Stack mirroring requires watchOS 10).
- The Apple Watch surface is Smart Stack only; no standalone watchOS app target is added.
- Live Activity content updates are driven from the foreground app and from existing scheduled wake-ups; APNs push updates are out of scope for v1.
- Apple's 8-hour Live Activity lifetime cap is acceptable; sessions naturally end before that limit (`WatchSession` ends within minutes of `FinalCall`).
- All notification copy already in `WatchEngine` is the canonical wording; the Live Activity must follow it, not the other way around.
- `product-spec.md` remains the product source of truth; this plan adds a presentation surface for already-specified behavior and does not change watch-session semantics.

## Locked Defaults

- **Countdown rendering**: Use `Text(timerInterval:)` for the time-to-leave and time-to-final-call counters. System-driven, no per-second update calls from the app side.
- **Watch-stopped dismissal**: `.dismissalPolicy(.after(now + 2 minutes))` so the reason stays readable on the lock screen / Smart Stack briefly, then auto-clears.
- **Visibility**: Live Activity starts when the watch session starts (`GetReady`) and surfaces all states through to end. Earlier visibility chosen so the watch face has context before the leave window opens.
- **Smart Stack**: iOS Live Activity mirrored automatically by Apple. No standalone watchOS app target.
