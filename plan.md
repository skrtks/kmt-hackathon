# App Implementation Plans

Plan version: 1.3.0
Target app version: MVP plus Wear OS companion MVP
Status: First app version implemented; Wear OS foundation in progress; Android/Wear notification/live activity alignment implemented; app naming surfaces renamed to Leave; UI componentization plan drafted
Last updated: 2026-04-29

## Versioning

Plan versions use `major.minor.patch`.

- Increment `major` when the plan target or release scope changes substantially.
- Increment `minor` when implementation approach, product behavior, interfaces, or acceptance criteria change.
- Increment `patch` for clarifications, typo fixes, formatting, or non-behavioral edits.

## Version History

- `1.3.0` - Added the UI file split and reusable component extraction plan for phone, Wear OS, and watchOS surfaces.
- `1.2.0` - Added and implemented the app naming cleanup plan so user-facing product surfaces use `Leave`.
- `1.1.5` - Restored concise `Leave now` ongoing status copy after trying explicit countdown text.
- `1.1.4` - Added live leave-window countdown copy to the `Leave now` ongoing status text.
- `1.1.3` - Adjusted the Wear Ongoing Activity Recents entry so route title and status text are not duplicated.
- `1.1.2` - Removed the Android ongoing notification progress bar so notifications use copy plus native countdown only.
- `1.1.1` - Implemented the Android notification, Android ongoing status, Wear Ongoing Activity, and shared active-surface copy alignment.
- `1.1.0` - Added the Android notification, Android ongoing status, Wear Ongoing Activity, and live-activity alignment plan.
- `1.0.6` - Added a final-call pulsing red ring on the Wear active-session screen.
- `1.0.5` - Smoothed the Wear water countdown at the `Leave at` to `Leave now` boundary.
- `1.0.4` - Added a Wear active-session water-level countdown background for time remaining until final call.
- `1.0.3` - Added Wear-app-owned Ongoing Activity posting from active snapshot sync.
- `1.0.2` - Removed placeholder Wear action buttons; current Wear active session screen is read-only until command sync is implemented.
- `1.0.1` - Recorded the initial Wear OS foundation work: shared KMP module, separate Wear app module, manifest split, and active-snapshot Data Layer reading.
- `1.0.0` - Added the Wear OS companion MVP port plan based on Android Wear OS packaging, Compose, Data Layer, Ongoing Activity, Tile, and quality guidance.
- `0.1.3` - Documented the remaining hardening gap for true OS-level auto-start scheduling.
- `0.1.2` - Second-pass review fixes for active-session restore, schedule-end handling, exact window-open notification suppression, and regression coverage.
- `0.1.1` - Marked the first-pass implementation complete.
- `0.1.0` - Initial implementation plan for the first full MVP using mock transit data.

## UI File Split And Componentization Plan

Split the full-size Compose app into screen files, reusable UI components, and presentation helpers without changing product behavior. The primary code pressure point is `composeApp/src/commonMain/kotlin/com/samex/kmt_hackathon/App.kt`, which currently owns app setup, navigation, Home, active-watch UI, place setup, commute setup/edit, settings, reusable layout primitives, field groups, haptic wrappers, and UI formatting helpers.

Reusable extraction should be driven by usage and behavior:

- If a pattern has two or more usages across the phone Compose UI, Wear OS Compose UI, or watchOS SwiftUI surfaces, it is a candidate for a reusable component, helper, or shared presentation contract.
- UI-free timing, copy, state, and formatting decisions should move toward shared Kotlin domain/presentation helpers when they can serve both phone and Wear OS.
- Platform-native visual implementations should stay platform-local when the same idea needs different UI technology, such as SwiftUI watchOS views versus Compose Wear views.
- Extract behavior slices gradually and compile after each slice so the refactor stays reviewable.

### Current Investigation Findings

- `App.kt` is about 2,900 lines and contains more than 60 composables/helpers.
- Existing reusable primitives already exist inside `App.kt`, including `CompactAware`, `ActionButtons`, `ButtonLabel`, `NavButton`, `Stepper`, `SetupChoice`, `RouteChip`, `RouteChipColumn`, `SettingsPanel`, `SettingStepperRow`, `SettingSwitchRow`, `ErrorCard`, and `EmptyCard`.
- Repeated visual patterns include flat bordered cards/surfaces, responsive action rows, selected/unselected choices, label/value metric pills, paired text fields, switch rows, stepper rows, schedule/status pills, and route chips.
- Active-watch copy and timing logic appears in several places:
  - Phone Compose active watch card in `App.kt`.
  - Shared Kotlin `ActiveWatchPresentation`.
  - Wear OS active screen and ongoing notification code.
  - watchOS `WatchSnapshot` / `WatchContentView`.
  - iOS ActivityKit widget helpers.
- `style.md` and `design.md` already define the design rules that extracted components must preserve: flat surfaces, no shadows, no nested cards, role-based colors, compact responsive layouts, route chips as text plus accent, and active-watch copy consistency.

### Target Package Shape

Use `com.samex.kmt_hackathon.ui.*` for new full-size Compose UI files.

Proposed package layout:

- `com.samex.kmt_hackathon.ui.app`
  - App shell, navigation transition, header, top-level error/replacement surfaces.
- `com.samex.kmt_hackathon.ui.components`
  - Cross-screen Compose primitives used at least twice.
- `com.samex.kmt_hackathon.ui.presentation`
  - UI-facing formatters and adapters around domain models.
- `com.samex.kmt_hackathon.ui.home`
  - Home dashboard, saved commute cards, Home action group.
- `com.samex.kmt_hackathon.ui.activewatch`
  - Active watch hero, countdown panel, upcoming-window rows, route chips if they remain active-watch specific.
- `com.samex.kmt_hackathon.ui.places`
  - Place editor and saved places list.
- `com.samex.kmt_hackathon.ui.commute`
  - Commute setup/edit screens, steps, draft validation/display helpers.
- `com.samex.kmt_hackathon.ui.settings`
  - Settings screen, settings panels, theme picker, debug controls.

Wear OS should keep watch-native implementation under `wearApp/src/main/java/com/samex/kmt_hackathon/wear`, but can split into files such as:

- `WearMainActivity.kt` for activity and Data Layer wiring.
- `WearApp.kt` for top-level Wear UI routing.
- `WearActiveWatchScreen.kt` for the active glance surface.
- `WearEmptyState.kt` for no-active-watch states.
- `WearCountdownVisuals.kt` for water countdown and final-call ring.
- `WearSnapshotMapping.kt` for DataMap parsing helpers if parsing continues to grow.

watchOS should keep SwiftUI-native files under `iosApp/WatchApp`, but can split into:

- `WatchContentView.swift` for top-level routing.
- `WatchActiveView.swift` for active-session layout.
- `WatchEmptyView.swift` for empty/sync states.
- `WatchCountdownVisuals.swift` for water background and final-call ring.
- `WatchSnapshotPresentation.swift` for headline/status/countdown helpers if Swift-side duplication remains necessary.

### Reusable Component Candidates

Extract these first because they have clear 2+ usage or already represent repeated patterns:

- `Adaptive.kt`
  - `CompactAware`
  - `ActionButtons`
  - `responsiveButtonModifier`
  - `BottomNavigationScrollSpacer`
- `Buttons.kt`
  - `ButtonLabel`
  - `NavButton`
  - `hapticClick`
  - `hapticResultClick`
- `Surfaces.kt`
  - `flatCardElevation`
  - reusable flat bordered card/panel wrappers
  - `ErrorCard`
  - `EmptyCard`
  - replacement/permission alert surfaces if they remain generic enough.
- `Pills.kt`
  - generic label/value metric pill used by settings/watch metrics.
  - status/schedule pill variants.
- `Choices.kt`
  - selected/unselected full-width choice used by origin, stop, line, and weekday controls.
- `Fields.kt`
  - paired field row pattern for place coordinates, arrival buffer, and schedule times.
- `Stepper.kt`
  - `Stepper`, `StepperMark`, and setting stepper row support.
- `RouteChip.kt`
  - `RouteChip`, `RouteChipColumn`, route accent selection.
- `SettingsRows.kt`
  - setting switch row, setting stepper row, notification status row.

Do not extract one-off UI just to reduce line count. Keep components close to their feature package until there are at least two real usages or a clear cross-platform presentation reason.

### Shared Presentation Candidates

Move duplicated copy/timing decisions out of screen files before splitting too deeply:

- Active-watch headline and countdown:
  - Prefer using or extending `shared/src/commonMain/kotlin/com/samex/kmt_hackathon/core/ActiveWatchPresentation.kt`.
  - Cover `GetReady`, `LeaveNow`, `FinalCall`, `Missed`, and `Leaving`.
  - Include timer target and whether final-call/window countdown UI should be shown.
- Leave-window progress:
  - Share a Kotlin helper for phone and Wear OS where possible.
  - Keep Swift implementation mirrored unless a shared/generated bridge is introduced later.
- Route and commute display:
  - Move `commuteOriginName`, `commuteRouteLabels`, `groupRouteLabels`, `lineDirectionLabel`, and draft summary helpers into `ui.presentation`.
  - Keep helpers UI-facing if they depend on `TransitAppModel`; move to shared domain only when they become model-independent.
- Time formatting:
  - Reuse `formatMinutesOfDay` and `formatCountdownToMinutesOfDay` where possible.
  - Avoid adding new local countdown formatters unless a platform-native API requires it.

### Implementation Plan

1. Prepare package structure.
   - Create `ui/app`, `ui/components`, `ui/presentation`, `ui/home`, `ui/activewatch`, `ui/places`, `ui/commute`, and `ui/settings`.
   - Keep `App()` public in the root package so platform entry points do not change.
2. Extract foundation components.
   - Move adaptive layout, button labels, haptic wrappers, flat card elevation, basic alert cards, and scroll spacer first.
   - Compile after this step to catch visibility/import mistakes early.
3. Extract route and active-watch presentation helpers.
   - Move route chip UI and active-watch UI text/progress helpers.
   - Reconcile duplicate phone/Wear Kotlin helpers with `ActiveWatchPresentation` where possible.
   - Add focused tests if shared helper behavior changes.
4. Extract active-watch UI.
   - Move `ActiveWatchSection`, `ActiveWatchHero`, `LeaveWindowCountdown`, `WatchMetric`, and `UpcomingWindowRow`.
   - Preserve current Home behavior: active watch remains on Home, no separate Watch route.
5. Extract Home and Places.
   - Move `HomeScreen`, `HomeManagementActions`, `CommuteSummaryCard`, `CommuteCardActions`, `PlacesScreen`, `PlaceEditor`, and place summary/field helpers.
   - Keep saved commute behavior and notification permission warning unchanged.
6. Extract commute setup/edit.
   - Move setup step enum, step screens, edit sections, draft validation, and draft summaries.
   - Preserve single line/direction behavior and copy from `style.md`.
   - Consider a reusable `ExpandableEditSection` only because edit sections use the same pattern repeatedly.
7. Extract settings.
   - Move settings overview, panels, rows, notification panel, theme picker, and debug controls.
   - Keep theme picker last in Settings.
8. Split Wear OS files.
   - Separate activity/Data Layer wiring from Wear UI.
   - Reuse shared Kotlin active-watch presentation helper for Wear headline/status where practical.
   - Keep Wear Material3, black background, water countdown, and final-call ring local to `:wearApp`.
9. Split watchOS SwiftUI files.
   - Separate top-level routing, active view, empty view, visuals, and snapshot presentation.
   - Mirror shared presentation semantics but keep SwiftUI visuals native.
10. Clean up and verify.
   - Remove dead helpers from old files after each migration.
   - Run targeted searches for duplicate helpers and stale imports.
   - Compile phone/common/Wear targets after the main slices.

### Acceptance Criteria

- Root `App.kt` is reduced to app construction, ticking, top-level theme, shell, header, and screen routing.
- Full-size Compose screens live in feature packages under `com.samex.kmt_hackathon.ui.*`.
- Reusable components are extracted when they have at least two usages across phone, Wear OS, or watchOS equivalents.
- Extracted components preserve the documented flat style: no shadows, no nested cards, no decorative gradients/orbs, role-based colors, compact-safe text.
- Active-watch headline/copy semantics stay consistent across phone, Android ongoing/Wear surfaces, and watchOS:
  - `Leave at <time>`
  - `Leave now`
  - `Final call`
  - `Departure in <countdown>`
- Commute setup/edit still enforce exactly one selected line/direction.
- Home remains the active-watch dashboard; no new Watch route is introduced.
- Settings remains a page with a back button and theme picker last.
- Wear OS remains watch-native and does not reuse mobile Material3 UI.
- watchOS remains SwiftUI-native and mirrors shared state semantics.
- Targeted builds pass after implementation:
  - `GRADLE_USER_HOME=/tmp/kmt-hackathon-gradle ./gradlew :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinIosSimulatorArm64`
  - `GRADLE_USER_HOME=/tmp/kmt-hackathon-gradle ./gradlew :wearApp:assembleDebug`
  - watchOS/iOS build via Xcode when Swift files are split.

### Open Decisions

- Whether to create generic flat card/panel wrappers now, or keep style helpers smaller until a third panel/card family appears.
- Whether Swift watchOS presentation should keep mirrored helpers or receive richer preformatted payload fields from the phone.
- Whether iOS ActivityKit widget presentation should be included in this refactor pass or handled in the existing notification/live-activity alignment plan.
- Whether tests should be added before or after the active presentation helper consolidation.

## App Naming Cleanup Plan

Rename the app to `Leave` everywhere a user, installer, launcher, or product document would see the app name. Keep technical identities stable unless a separate migration is planned, including package names, bundle identifiers, Gradle module names, source-set paths, and transit/live-activity type names.

### Current Investigation Findings

- Android phone and Wear launchers read `@string/app_name`; both needed to resolve to `Leave`.
- The desktop app window title still used `kmt_hackathon`, and the desktop native distribution name was tied to the same old identifier.
- The Compose app header used the prior two-word product name.
- The watch companion empty/error copy told users to open the prior product name on the phone/iPhone.
- iOS/watchOS display-name settings used older component names, while the iOS product name still inherited `kmt_hackathon`.
- `design.md` and `style.md` still read as prior-name product documents, and `design.md` still carried the old open question about whether to keep that name.

### Implementation Plan

1. Update launcher, desktop, iOS, watchOS, and extension display names to `Leave`.
2. Update in-app and companion-app product copy to `Leave`.
3. Update product-facing documentation titles and close the obsolete product-name question.
4. Re-run a naming search for stale display-name strings, app-name keys, and iOS display-name keys, then run a targeted Gradle check.

### Acceptance Criteria

- Android phone launcher label is `Leave`.
- Wear launcher label is `Leave`.
- Desktop window title and native distribution app name are `Leave`.
- iOS app, watchOS app, and Live Activity extension display names resolve to `Leave`.
- The main app header says `Leave`.
- Companion empty/error copy says `Open Leave...`.
- Remaining `kmt_hackathon`, `TransitLiveActivity`, and `LeaveWindow` references are technical identifiers or domain terminology rather than app-name copy.

## Notification And Live Activity Alignment Plan

Align the time-critical out-of-app surfaces with the phone active-watch card and the Wear app. The primary scope is Android phone notifications plus Wear OS app/Ongoing Activity behavior. The repo also contains an iOS ActivityKit widget, so the plan calls out iOS parity as a follow-up surface rather than ignoring it.

### Current Investigation Findings

- Scheduled Android alerts are produced by `WatchEngine.notificationPlansForSession` and delivered by `AndroidNotificationScheduler` / `NotificationReceiver`.
  - Alert titles are shared through `WatchCopy.title`: `Leave now` and `Final call`.
  - Alert body copy comes from `WatchEngine.notificationBody`, currently shaped like `tram 4 at 13:44 from 13:29-13:35`.
  - The notification itself uses the launcher icon, `leave_window_alerts` high-importance channel, app-default sound/vibration, auto-cancel, and no actions.
  - This satisfies the MVP no-actions rule, but the body does not match current app language because it omits headsign/stop context and uses `from <open>-<final>` instead of `Window closes`, `Departure`, or `Leave by`.
- Android ongoing status is implemented in `AndroidLiveActivityController`.
  - It posts a silent low-importance ongoing notification on `active_watch_status`.
  - It syncs `LiveActivitySnapshot` to the Wear Data Layer path `/transit-live-activity`, even when Android notification permission is not granted.
  - It uses the shared active-surface presentation for headline, detail text, tone, and countdown target.
  - It keeps the native countdown/chronometer where supported, but intentionally does not show a notification progress bar.
  - The phone Android app owns the phone ongoing notification and Data Layer sync; the Wear app owns the Wear Ongoing Activity.
- Wear in-app active screen is implemented in `WearMainActivity`.
  - It uses a black full-screen surface, one large headline, route/headsign, stop, optional `Leave by`, and `Departure`.
  - It shows the blue water background only while the leave window is active and shows a pulsing red edge ring at final call.
  - It now supports `Departure in <countdown>` after `I'm leaving`.
  - The screen is visually closer to the phone app than the notifications are.
- Wear Ongoing Activity is implemented in `WearOngoingActivity`.
  - It posts a local silent notification with `OngoingActivity` metadata and a short status text.
  - It uses the same shared active-surface presentation as the Wear screen.
  - It has no progress surface, which is intentional for Wear Ongoing Activity.
- iOS ActivityKit exists in `iosApp/TransitLiveActivity`.
  - It renders a black lock-screen/Dynamic Island activity with status headline, route, final-call countdown, and a linear progress view.
  - It does not currently model `isLeaving` in `TransitWatchContentState`.
  - On `LiveActivityEndReason.Leaving`, the iOS bridge ends immediately instead of lingering until departure. That is currently different from Android/Wear.

### Alignment Principles

- One state contract should drive every out-of-app surface:
  - `GetReady`: headline `Leave at <time>`, target `windowOpenMinutes`.
  - `LeaveNow`: headline `Leave now`, target `finalCallMinutes`, keep supporting text route/departure focused unless a surface has a native countdown treatment.
  - `FinalCall`: headline `Final call`, urgent visual treatment, no separate `Leave by` pill/line.
  - `Leaving`: headline `Departure in <countdown>`, target `departureTimeMinutes`, hide window-progress language.
  - `Missed`: do not alert; move to the next viable group or clear the live/ongoing surface.
- Scheduled alert notifications should be event alerts, not miniature live activities.
  - They may use default sound/vibration and high importance.
  - They should remain action-free for MVP.
  - They should open the app when tapped.
- Ongoing/live surfaces should be quiet return paths and glanceable status.
  - They should be silent, low importance, ongoing, and update without re-alerting.
  - They should prioritize the same headline as the app.
  - They should use short route and departure context, not long instructional copy.
- Wear surfaces should stay watch-native.
  - Use `OngoingActivity` status text for the watch-face/recent-apps chip.
  - Keep the local Wear notification short; use the Wear app screen for the richer visual treatment.
  - Do not add watch notification action buttons until watch-to-phone command sync exists.
- The out-of-app copy should follow `style.md`: clear, calm, direct, no cute filler, no technical transport-planning language.

### Target Copy Matrix

Scheduled alert notifications:

- Window open:
  - Title: `Leave now`
  - Compact body: `<line> to <headsign> - departure <time>`
  - Expanded body: `<stopName>\nWindow closes <finalCallTime>\nDeparture <departureTime>`
- Final call:
  - Title: `Final call`
  - Compact body: `<line> to <headsign> - departure <time>`
  - Expanded body: `<stopName>\nLast safe leave time\nDeparture <departureTime>`
- Merged group:
  - Title remains state-based.
  - Compact body uses the primary departure.
  - Expanded body lists up to two options, for example `tram 4 13:44 or metro 52 13:45`; add `+N more` if needed.

Android ongoing status:

- Get ready:
  - Title: `Leave at <windowOpenTime>`
  - Text: `<line> to <headsign> - departure <time>`
  - Native countdown target: window open.
- Leave now:
  - Title: `Leave now`
  - Text: `<line> to <headsign> - departure <time>`
  - No progress bar; use the native countdown target instead of repeating countdown copy.
- Final call:
  - Title: `Final call`
  - Text: `<line> to <headsign> - departure <time>`
  - No progress bar; accent should be urgent.
- Leaving:
  - Title: `Departure in <countdown>`
  - Text: `<line> to <headsign> - departure <time>`
  - No progress bar; use the departure countdown.

Wear Ongoing Activity:

- Status text should be even shorter than phone notification text:
  - `Leave at <time>`
  - `Leave now`
  - `Final call`
  - `Departs in <countdown>` or `Departure in <countdown>` depending what fits on device.
- Notification title should use the same headline as the Wear in-app active screen.
- Notification text should be route-first: `<line> to <headsign>`.
- Expanded text can add stop and `Departure <time>`, but should not include the legacy `from <open>-<final>` body.

iOS ActivityKit follow-up:

- Add `isLeaving` or a derived presentation state to `TransitWatchContentState`.
- Switch the lock-screen/Dynamic Island target date based on state: window open, final call, or departure.
- Replace the generic progress view with the same window semantics used on phone/Wear: show window countdown during `LeaveNow`, hide it for `FinalCall` and `Leaving`.
- Decide whether iOS should linger after `I'm leaving` until departure for parity with Android/Wear, or continue ending immediately as an intentional platform difference.

### Implementation Plan

1. Create a shared active-surface presentation helper.
   - Add a UI-free helper in `shared/src/commonMain/kotlin/com/samex/kmt_hackathon/core`, for example `WatchSurfaceCopy` or `ActiveWatchPresentation`.
   - Inputs: `LiveActivitySnapshot`, current seconds/minutes when needed, and surface kind if the surface has tight copy limits.
   - Outputs:
     - `headline`
     - `compactText`
     - `expandedLines`
     - `statusTone`
     - `timerTargetMinutes`
     - `showsFinalCallCue`
   - Keep actual Android/Wear notification APIs platform-local; only share the copy/state decisions.
2. Replace legacy notification body generation.
   - Keep `NotificationPlan` small, but generate display copy through the new helper or a dedicated alert-copy helper.
   - Preserve current scheduling behavior and no-action MVP rule.
   - Switch scheduled alert notifications to `ic_transit_ongoing`, explicit category/visibility/priority, and consistent status color.
   - Keep channel IDs stable unless there is a concrete reason to migrate; channel names can be clarified if needed.
3. Simplify Android ongoing notification ownership.
   - Make phone Android ongoing notification a phone return path only.
   - Remove or isolate Wear `OngoingActivity` metadata from the phone module if the local Wear app owns the Wear Ongoing Activity.
   - Keep Data Layer sync in the phone module because it is the source of truth for active snapshots.
   - Ensure notification permission denial does not block Wear Data Layer sync.
4. Align Android ongoing notification rendering.
   - Use the shared headline instead of raw `snapshot.title`.
   - Use target minutes from the shared presentation helper.
   - Do not show a progress bar; use clear state copy plus the native countdown/chronometer where supported.
   - Use urgent accent only for `FinalCall`.
   - Keep the ongoing notification silent and `setOnlyAlertOnce(true)`.
5. Align Wear Ongoing Activity rendering.
   - Use the same presentation helper as the Wear screen.
   - Replace legacy `snapshot.body` in `ongoingBigText` with explicit stop, route, departure, and optional window-close details.
   - Keep the status part short enough for the watch-face chip.
   - Keep the Wear notification local-only and silent.
6. Align Wear in-app screen with the same presentation helper.
   - Keep the current visual direction: black surface, blue water background for `LeaveNow`, red pulsing edge for `FinalCall`, departure countdown after leaving.
   - Replace local duplicate countdown/headline helpers only after the shared helper is in place.
   - Preserve readable text sizes and round-screen-safe layout.
7. Add regression tests.
   - Unit-test the shared presentation helper for `GetReady`, `LeaveNow`, `FinalCall`, `Leaving`, and merged-window cases.
   - Add Android DataMap round-trip tests for all live snapshot fields, including `isLeaving`.
   - Add model tests that `I'm leaving` produces a departure-target ongoing snapshot and that scheduled notifications remain canceled.
   - Keep existing `WatchEngine` notification-copy tests updated to the new body contract.
8. Manual QA pass.
   - Use demo mode to produce each state.
   - Check Android notification shade compact/expanded scheduled alerts.
   - Check Android ongoing notification compact/expanded state transitions.
   - Check Wear in-app screen and Wear Ongoing Activity chip/notification.
   - Check notification permission denied on Android phone and Wear: Wear sync should still update where possible, but local notifications should not post.
   - Check final-call transition does not produce duplicate alerting or a stale `Leave by` line.
   - Check `I'm leaving` updates all active surfaces to `Departure in <countdown>` and clears them at departure.

### Acceptance Criteria

- Copy on phone active card, Android ongoing notification, Wear app screen, and Wear Ongoing Activity uses the same state language.
- Scheduled alerts remain high-importance, action-free, default sound/vibration notifications.
- Ongoing/live surfaces remain silent, low-importance return paths.
- Ongoing notifications do not show a progress bar.
- `Final call` has a clear urgent clue on watch surfaces and does not show stale `Leave by` wording.
- `I'm leaving` changes ongoing/watch surfaces to `Departure in <countdown>` and keeps them alive until the selected departure time.
- Wear Data Layer sync continues when phone notification permission is denied.
- No duplicate watch ongoing surfaces are produced by phone and Wear code fighting for ownership.
- `./gradlew :wearApp:assembleDebug`, `./gradlew :composeApp:compileDebugKotlinAndroid`, and `./gradlew :composeApp:allTests` pass after implementation.

### Open Decisions

- Whether final-call Android ongoing notification should use only urgent copy/color, or also suppress the native chronometer.
- Whether the Wear Ongoing Activity status should use `Departure in` or shorter `Departs in` for small chip fit.
- Whether iOS ActivityKit should match Android/Wear by lingering after `I'm leaving` until departure.

## Wear OS Port Plan

Build a Wear OS companion app that shares the transit timing domain model with the existing app, but uses a watch-native UI and a separate Wear OS Android artifact. The first Wear release should not try to shrink the current `App()` UI. It should focus on the watch jobs that matter: glance at the active leave window, act on it quickly, and get back to the active session from watch surfaces.

### Investigation Findings

- The current project is a single `:composeApp` Kotlin Multiplatform application module. Its shared UI in `composeApp/src/commonMain/kotlin/com/samex/kmt_hackathon/App.kt` uses mobile Material3 components and multi-screen setup/edit flows that are not appropriate for a watch.
- The Android manifest currently declares `android.hardware.type.watch` with `android:required="false"`. That is not a valid Wear distribution strategy: Wear OS APKs are separate from mobile APKs, and the Wear APK must declare the watch feature without setting `required` to `false`.
- The current Android implementation already has useful watch groundwork:
  - `AndroidLiveActivityController` serializes `LiveActivitySnapshot` data to the Wear Data Layer path `/transit-live-activity`.
  - `WearLiveActivitySyncService` listens for that data and can update a local Android ongoing activity notification.
  - `PlatformServices.isWearDevice()` exists, but it only gates small parts of the current mobile UI; it is not enough for a real Wear app.
- Compose for Wear OS should use `androidx.wear.compose` Material3/Foundation/Navigation APIs. The mobile Material3 theme and components can remain in the phone/tablet/desktop/iOS UI, but the Wear app should have its own Wear Material theme and components.
- A companion-first MVP is the pragmatic release target because the current app stores saved commutes and active sessions locally on the phone. A fully standalone watch app is a later product step that needs direct transit networking, account/cloud sync, or a watch setup flow.

### Product Scope

- Ship the first Wear OS version as a non-standalone Android companion app.
- Keep phone app as the source of truth for saved commutes, active watch sessions, auto-start scheduling, transit refresh, and alert notification scheduling.
- Let the watch display the active watch state and send commands to the phone:
  - Start a synced saved commute.
  - Mark `I'm leaving`.
  - Skip the current departure group.
  - Stop the active watch.
- Keep commute setup, place setup, schedule editing, theme settings, and notification permission explanation on the phone for the Wear MVP.
- Show clear watch empty/offline states:
  - No phone companion found.
  - Phone companion found but no saved commutes.
  - Saved commutes available but no active watch.
  - Active watch data is stale or the phone is unreachable.
- Do not support Wear OS watches paired to iOS for this companion MVP. The Wear Data Layer does not work for Wear OS devices paired with iOS; iOS users already have the iOS Live Activity / watchOS Smart Stack path.

### Target Wear Surfaces

- Wear app: the primary interactive surface. Use a shallow vertical flow with no hierarchy deeper than two levels.
  - Active session screen first when a watch is active.
  - Synced saved commutes list when no watch is active.
  - Details/action screen for the current group only when needed.
- Ongoing Activity: keep the active watch return path visible on the watch face and in recent apps while a session is active.
- Tile: show the current active session at a glance and deep-link back into the Wear app. If there is no active session, show a compact "Start on phone" or "Start commute" entry depending on synced data availability.
- Complication: defer until after the Tile. A short-text complication for `Leave by HH:MM` is useful, but the Tile and Ongoing Activity cover the MVP better.

### Architecture Plan

1. Split shared, UI-free logic out of `:composeApp`.
   - Add a shared KMP library module, for example `:shared`.
   - Move domain and model code from `composeApp/src/commonMain/kotlin/com/samex/kmt_hackathon/core` and mock transit data from `transit` into `:shared`.
   - Keep the existing phone/desktop/iOS Compose UI in `:composeApp`.
   - Keep platform service implementations in app-specific modules, or extract reusable Android service code only after the split is stable.
2. Add a new `:wearApp` Android application module.
   - Use the same application ID/package for Play association with the phone app, with a version-code scheme that is unique across form factors.
   - Use the current compile/target SDK line from the Android app. Default the Wear minimum SDK to Wear OS 3 / API 30 unless Wear OS 2 support is explicitly required.
   - Declare `<uses-feature android:name="android.hardware.type.watch" />` in the Wear manifest.
   - Declare `com.google.android.wearable.standalone=false` for the companion MVP.
   - Remove the watch feature declaration from the phone manifest.
3. Add Wear dependencies.
   - `androidx.wear.compose:compose-material3`
   - `androidx.wear.compose:compose-foundation`
   - `androidx.wear.compose:compose-ui-tooling`
   - `androidx.wear:wear-ongoing`
   - `com.google.android.gms:play-services-wearable`
   - Tile/ProtoLayout libraries when implementing the Tile.
4. Build a Wear-specific presentation model.
   - Reuse `LiveActivitySnapshot` as the seed for active-session display.
   - Add a small serialized phone-to-watch state model for saved commute summaries and companion availability.
   - Keep watch state intentionally smaller than `UserData`; do not sync full setup drafts or settings.
5. Implement Data Layer communication.
   - Use `DataClient`/`DataItem` for persisted synced state: active watch snapshot, saved commute summaries, and stale timestamps.
   - Use `MessageClient` for one-off commands: start commute, mark leaving, skip, stop, and request resync.
   - Use `CapabilityClient` so the watch can tell whether a reachable phone companion exists before enabling command buttons.
6. Build the Wear app UI.
   - Use a black background and Wear Material3 components.
   - Use `TimeText`, `PositionIndicator`, rotary-friendly scrolling, and watch-sized 48dp minimum touch targets.
   - Prioritize one large status headline, line/stop/direction, final-call/departure time, then primary actions.
   - Keep actions inline and labeled; avoid trying to reproduce phone cards, setup forms, or settings panels.
7. Rework notifications and ongoing status.
   - Phone remains authoritative for alert scheduling so the watch does not duplicate window-open/final-call alerts.
   - Wear app posts/updates a local Ongoing Activity from the active snapshot so the user can return from the watch face and recent apps.
   - End the Ongoing Activity when the phone clears the active snapshot, the command succeeds, or the synced state is stale beyond a defined threshold.
8. Add the Tile after the app screen is working.
   - Render only a small amount of cached data.
   - Link the Tile to the active watch screen.
   - Reference the ongoing activity while a session is active.
   - Do not fetch transit/network data from the Tile service.

### Implementation Order

1. Project structure:
   - Create `:shared`.
   - Move shared domain/model/transit code.
   - Update `:composeApp` imports and build files.
   - Confirm existing common tests still run.
2. Wear shell:
   - Create `:wearApp`.
   - Add manifest, launcher activity, Wear theme, preview setup, and a static fake active-session screen.
   - Build with `./gradlew :wearApp:assembleDebug`.
3. Data sync:
   - Move/generalize existing active snapshot Data Layer code.
   - Add saved commute summaries and companion capability detection.
   - Add stale-state handling.
4. Commands:
   - Add phone-side command receiver.
   - Add watch-side command sender and disabled/offline states.
   - Add acknowledgements or state refresh after commands.
5. Ongoing Activity:
   - Move the current ongoing-notification logic into Wear-safe code.
   - Verify active indicator, recent-app chip, tap target, and end behavior.
6. Tile:
   - Add a Tile service backed by cached state.
   - Add tile preview metadata.
7. Polish and docs:
   - Update `AGENTS.md` commands for Wear builds/tests.
   - Update `design.md` and `style.md` with watch-specific UI rules.
   - Add screenshots and Play listing notes before release.

### Current Progress

- Completed the first project-structure slice:
  - Added `:shared`.
  - Moved UI-free domain, transit, watch-engine, and persistence repository code into `:shared`.
  - Moved watch-engine, mock-transit, and persistence tests into `:shared`.
  - Updated `:composeApp` to depend on `:shared`.
- Started the Wear shell:
  - Added `:wearApp` as a separate Android app module.
  - Added the Wear manifest with `android.hardware.type.watch` and `com.google.android.wearable.standalone=false`.
  - Removed the invalid phone-manifest watch feature declaration.
  - Added a Wear Compose Material3 activity that reads the current `/transit-live-activity` Data Layer item and renders active/no-active/phone-unavailable states.
  - Removed placeholder action buttons from the active session screen; the Wear surface is currently read-only.
  - Added a simple blue water-level background that drains downward as the active leave window approaches final call.
  - Added a pulsing red edge ring during final call.
- Added Wear Ongoing Activity support:
  - The Wear app requests notification permission when needed.
  - Active snapshots post/update a local ongoing notification with `OngoingActivity` metadata.
  - A Wear-side Data Layer listener service keeps the Ongoing Activity in sync even when the Wear app UI is not open.
  - Inactive, deleted, or unavailable snapshots cancel the Wear ongoing notification.
- Not yet implemented:
  - Watch-to-phone command messages.
  - Saved-commute summary sync.
  - Tile service.

### Acceptance Criteria

- The phone app still builds and tests after the shared-module extraction.
- `./gradlew :wearApp:assembleDebug` produces a Wear OS APK with the watch feature manifest entry and no `required=false` watch feature.
- On a paired Android phone plus Wear OS emulator/device:
  - The watch receives active session updates from the phone.
  - The watch shows the correct headline state: `Leave at <time>`, `Leave now`, `Final call`, or `Departure in <countdown>`.
  - `I'm leaving`, `Skip`, `Stop`, and `Start` commands reach the phone and the watch updates after the phone state changes.
  - The Ongoing Activity appears while a watch session is active and disappears when it ends.
  - The Tile shows cached active-state data and opens the Wear app.
- UI passes Wear shape checks on at least small round 192dp and large round 227dp emulators.
- Essential text is at least 12sp, touch targets are at least 48dp, scrollable views show position, and no text/control is clipped on round screens.

### Open Decisions

- Whether to build active-session display/actions as the first internal milestone before adding watch-side `Start commute`; the final Wear MVP should include watch-side start from synced saved commutes.
- Whether the Wear app should be usable when the phone is disconnected but cached commutes exist. The recommended MVP answer is "read-only cached state plus clear offline messaging"; standalone session ownership should wait for direct transit networking.
- Whether to preserve the existing `/transit-live-activity` Data Layer path for compatibility or migrate to clearer paths such as `/leave-window/active-watch` and `/leave-window/commutes`.
- Whether the Wear app should use the same theme names as the phone app or a fixed black Wear theme with only route/status accent colors.

### References

- Wear OS packaging requires separate Wear APKs and warns against `android.hardware.type.watch` with `required=false`: https://developer.android.com/training/wearables/packaging
- Standalone vs non-standalone Wear OS apps: https://developer.android.com/training/wearables/apps/standalone-apps
- Compose for Wear OS setup and Wear-specific Material3/Foundation guidance: https://developer.android.com/training/wearables/compose
- Wear app UX principles: https://developer.android.com/training/wearables/apps
- Wear Data Layer sync, DataClient, and iOS-paired limitation: https://developer.android.com/training/wearables/data/sync
- Wear Data Layer client choice and MessageClient command tradeoffs: https://developer.android.com/training/wearables/data/client-types
- Ongoing Activity guidance: https://developer.android.com/training/wearables/notifications/ongoing-activity
- Tiles guidance: https://developer.android.com/training/wearables/tiles
- Wear OS app quality requirements and test sizes: https://developer.android.com/docs/quality-guidelines/wear-app-quality

## First App Version Summary

Build the first full MVP of the transit leave-window companion app using the existing mock transit dataset instead of live transit APIs. The app should let a user create saved origin places, create saved commutes from mock stops/lines/directions, configure timing defaults, manually or automatically watch a commute, calculate leave windows, show the active watch screen, and schedule local notifications for window-open/final-call events.

The first version will be a real Compose Multiplatform app across Android, iOS, and JVM/Desktop, but Android/iOS are the primary targets for notification behavior. Desktop may use an in-app/fake notification adapter for development.

## First App Version Key Changes

- Replace the starter `App()` sample UI with a Material3 app shell driven by shared `commonMain` state.
- Add shared domain types for `SavedPlace`, `SavedCommute`, `ArrivalBuffer`, `WalkingSpeed`, `AutoStartSchedule`, `WatchSession`, `LeaveWindow`, `LeaveWindowGroup`, and notification planning.
- Add repository interfaces in common code:
  - `TransitRepository`, backed first by `MockTransitData`.
  - `UserDataRepository`, backed by platform key-value persistence.
  - `NotificationScheduler`, with Android/iOS real local notification implementations and a JVM fake implementation.
  - `TimeProvider`, with fake/test and platform implementations.
- Add a common watch engine that:
  - Calculates `windowOpen = departureTime - walkingTime - maxEarlyArrival`.
  - Calculates `finalCall = departureTime - walkingTime - minEarlyArrival`.
  - Merges same-stop departure windows within 1 minute.
  - Suppresses late notifications when watching starts inside an already-active window.
  - Handles skip, "I'm leaving," schedule end, and one-active-session rules.
- Use mock origin/place creation for v1: users name a saved place and choose from preset coordinates or enter coordinates manually. No external geocoder or map API yet.
- Use Haversine distance plus global walking speed to calculate walking time from saved place to stop.
- Persist saved places, saved commutes, global settings, schedules, and recoverable active-session state. Use structured JSON serialization over platform key-value storage.

## First App Version UI And Behavior

- Onboarding:
  - First launch guides the user to create a saved place.
  - Then create the first saved commute with that place preselected.
  - Request notification permission after commute creation, or when the user starts watching/enables a schedule.
- Saved commutes list:
  - Show stop, selected line/direction summary, origin place, schedule summary, auto-start toggle, and manual start.
  - Disabled auto-start does not block manual start.
- Commute setup:
  - Stop-first flow using mock stops.
  - Select one or more API-style line/direction/headsign pairs serving the stop.
  - Choose saved origin, arrival buffer override, and optional days/time-range schedule.
  - Prevent overlapping enabled auto-start schedules.
- Watch screen:
  - Primary current/next actionable panel.
  - Show action headline, departure time, leave window, walking time, stop, line, direction/headsign.
  - Show next few departures below.
  - Include "Skip this departure" and "I'm leaving."
  - Clearly show notification-disabled, no-departures, or API-error states.
- Notifications:
  - Window-open copy is calm; final-call copy is urgent.
  - No notification action buttons.
  - Tapping opens the app.
  - No missed-departure notification.
  - If API/data failure stops a background watch, schedule/show a "watch stopped" notification.

## First App Version Implementation Order

1. Domain core:
   - Add timing, walking-time, merge, schedule, and watch-session models.
   - Add unit-tested calculators before UI integration.
2. Data layer:
   - Wrap `MockTransitData` in `TransitRepository`.
   - Add platform key-value storage and JSON-backed user-data persistence.
   - Add seed/default settings: arrival buffer 1-3 minutes early and normal walking speed.
3. App state:
   - Add a shared view model/state holder with screen state, onboarding state, saved-commute management, setup flow, settings, and active watch-session actions.
4. UI:
   - Replace starter screen with onboarding, saved commute list, setup flow, watch screen, saved places, and settings.
   - Keep navigation simple with an internal sealed screen state; no navigation dependency required for v1.
5. Notifications and scheduling:
   - Add common notification planning.
   - Implement Android local notifications with runtime permission handling and manifest updates.
   - Implement iOS local notifications through `UNUserNotificationCenter`.
   - Implement JVM fake notification scheduler for desktop development/tests.
   - Reconcile scheduled notifications on app launch, settings changes, commute changes, manual start/end, and "I'm leaving."
6. Polish and integration:
   - Ensure active session recovery after app restart.
   - Add empty/error/permission-denied states.
   - Update `AGENTS.md` and docs if implementation adds commands, architecture decisions, or platform setup details.

## First App Version Test Plan

- Unit tests:
  - Leave-window formula.
  - Start-before-window, start-inside-window, start-after-final-call behavior.
  - Window-open and final-call notification planning.
  - Merge tolerance for closely aligned departures.
  - Skip suppresses all remaining alerts for a departure/group.
  - "I'm leaving" silences notifications and ends session after 30 minutes.
  - Schedule-end behavior.
  - Overlapping schedule validation.
  - Walking-time calculation from coordinates and walking speed.
  - Persistence round-trip for saved places, commutes, settings, and active session.
- Repository tests:
  - Existing mock data integrity tests stay green.
  - Mock transit repository returns stops, directions, and selected departures correctly.
- State-holder tests:
  - Onboarding path.
  - Saved commute creation.
  - Manual watch start and replacement confirmation.
  - Notification permission denied state.
  - No upcoming departures state.
- Platform checks:
  - Run `GRADLE_USER_HOME=/tmp/kmt-hackathon-gradle ./gradlew :composeApp:allTests`.
  - Build Android debug with `./gradlew :composeApp:assembleDebug`.
  - Run desktop with `./gradlew :composeApp:run` for fast UI inspection.
  - Verify iOS manually from Xcode after notification adapter work.

## First App Version Assumptions

- "First version" means the full MVP from `product-spec.md`, using mock transit data instead of live APIs.
- No external transit, geocoding, map, or routing API is introduced in v1.
- Real-time delay recalculation is out of scope.
- Address search is out of scope; saved places use preset/manual coordinates.
- Desktop does not need OS-level notifications in v1.
- Exact Android notification timing should use the best available platform mechanism; if exact alarms are unavailable, the app should still surface permission/status limitations clearly.
- `product-spec.md` remains the product source of truth; this plan is the implementation plan for that spec.

## First App Version Known Remaining Hardening

- Active watch sessions schedule real platform notifications, but scheduled auto-start currently depends on the app being active or opened during the schedule window. True OS-level background auto-start should be implemented as a follow-up with platform-specific scheduling rules.
