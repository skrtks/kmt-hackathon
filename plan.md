# First App Version Plan

Plan version: 0.1.0  
Target app version: MVP / first usable version  
Status: Draft ready for implementation  
Last updated: 2026-04-28

## Versioning

Plan versions use `major.minor.patch`.

- Increment `major` when the plan target or release scope changes substantially.
- Increment `minor` when implementation approach, product behavior, interfaces, or acceptance criteria change.
- Increment `patch` for clarifications, typo fixes, formatting, or non-behavioral edits.

## Version History

- `0.1.0` - Initial implementation plan for the first full MVP using mock transit data.

## Summary

Build the first full MVP of the transit leave-window companion app using the existing mock transit dataset instead of live transit APIs. The app should let a user create saved origin places, create saved commutes from mock stops/lines/directions, configure timing defaults, manually or automatically watch a commute, calculate leave windows, show the active watch screen, and schedule local notifications for window-open/final-call events.

The first version will be a real Compose Multiplatform app across Android, iOS, and JVM/Desktop, but Android/iOS are the primary targets for notification behavior. Desktop may use an in-app/fake notification adapter for development.

## Key Changes

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

## UI And Behavior

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

## Implementation Order

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

## Test Plan

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

## Assumptions

- "First version" means the full MVP from `product-spec.md`, using mock transit data instead of live APIs.
- No external transit, geocoding, map, or routing API is introduced in v1.
- Real-time delay recalculation is out of scope.
- Address search is out of scope; saved places use preset/manual coordinates.
- Desktop does not need OS-level notifications in v1.
- Exact Android notification timing should use the best available platform mechanism; if exact alarms are unavailable, the app should still surface permission/status limitations clearly.
- `product-spec.md` remains the product source of truth; this plan is the implementation plan for that spec.
