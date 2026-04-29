# App Implementation Plans

Plan version: 1.0.3  
Target app version: MVP plus Wear OS companion MVP  
Status: First app version implemented; Wear OS foundation in progress  
Last updated: 2026-04-29

## Versioning

Plan versions use `major.minor.patch`.

- Increment `major` when the plan target or release scope changes substantially.
- Increment `minor` when implementation approach, product behavior, interfaces, or acceptance criteria change.
- Increment `patch` for clarifications, typo fixes, formatting, or non-behavioral edits.

## Version History

- `1.0.3` - Added Wear-app-owned Ongoing Activity posting from active snapshot sync.
- `1.0.2` - Removed placeholder Wear action buttons; current Wear active session screen is read-only until command sync is implemented.
- `1.0.1` - Recorded the initial Wear OS foundation work: shared KMP module, separate Wear app module, manifest split, and active-snapshot Data Layer reading.
- `1.0.0` - Added the Wear OS companion MVP port plan based on Android Wear OS packaging, Compose, Data Layer, Ongoing Activity, Tile, and quality guidance.
- `0.1.3` - Documented the remaining hardening gap for true OS-level auto-start scheduling.
- `0.1.2` - Second-pass review fixes for active-session restore, schedule-end handling, exact window-open notification suppression, and regression coverage.
- `0.1.1` - Marked the first-pass implementation complete.
- `0.1.0` - Initial implementation plan for the first full MVP using mock transit data.

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
