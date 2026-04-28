# Transit Leave-Window Companion

## Product Intent

The app helps a user time when to leave for a bus, tram, or metro. It is not a full public transport planning app. The user already knows the stop and the useful line or lines. The app acts as a timing companion: it watches upcoming departures and tells the user when their personal leave window opens and when the final safe leave moment arrives.

Core promise: "Tell me when I can leave now and still arrive at my stop within my preferred early-arrival buffer."

Example:

- Departure: 08:30
- Walking time to stop: 8 minutes
- User wants to arrive at least 1 minute early and at most 3 minutes early
- Leave window: 08:19-08:21
- Window open notification: 08:19
- Final call notification: 08:21

## Platform

- Cross-platform mobile app using Compose Multiplatform.
- MVP targets real mobile behavior, not just a foreground prototype.
- MVP must support local/background notifications on the target mobile platforms.
- Platform-specific adapters are expected for notifications, permissions, background scheduling, location/address handling, and any walking-time provider integration.
- If notification permission is denied, setup and foreground watching still work, but the app must clearly show that notifications are disabled.

## MVP Users And Use Cases

Primary use cases:

- Daily repeat commute.
- Live helper while the user is getting ready.

MVP assumptions:

- All watch sessions come from saved commutes.
- Ad-hoc temporary watches are not part of MVP.
- The user does not choose a specific target departure. The app watches every upcoming viable departure until the session ends or notifications are silenced.
- Departure times are treated as fixed for MVP. Real-time delay recalculation is a later feature.
- The app fetches departures from the transit API when a watch session starts and refreshes during the session only as needed to keep future departures available.
- Once a notification has been scheduled for a fetched departure, MVP does not attempt to move that alert because of later delay changes.

## Core Concepts

### Saved Place

A saved place is a reusable origin, such as "Home" or "Office."

- Users name saved places.
- Saved places are origins only.
- Transit stops are not favorited separately in MVP.

### Saved Commute

A saved commute is the reusable setup that can be watched manually or automatically.

It contains:

- One saved origin place.
- One selected stop.
- One or more selected line and API-provided direction/headsign pairs serving that stop.
- Arrival buffer settings, using the global default unless overridden.
- Optional auto-start schedule.
- Enabled/disabled state for auto-start behavior.

MVP does not require:

- A destination field.
- A custom commute name.
- Morning/return pairing.

Saved commutes are displayed by stop and selected line/direction details.

### Watch Session

A watch session is an active monitoring run for one saved commute.

- Only one watch session can be active at a time.
- Manual start is always available, even if the commute's auto-start toggle is disabled.
- If the user manually starts another commute while one is active, the app asks for confirmation before stopping the current session and starting the new one.
- If an auto-start schedule begins while a manual session is active, that auto-start occurrence is skipped.
- Overlapping enabled auto-start schedules are prevented during setup/editing.

## Timing Model

Each departure produces a leave window.

Definitions:

- `departureTime`: scheduled/fetched departure time.
- `walkingTime`: calculated walking duration from saved place to stop, using the global walking-speed preference.
- `minEarlyArrival`: minimum acceptable early arrival, for example 1 minute before departure.
- `maxEarlyArrival`: maximum acceptable early arrival, for example 3 minutes before departure.

Formula:

- `windowOpen = departureTime - walkingTime - maxEarlyArrival`
- `finalCall = departureTime - walkingTime - minEarlyArrival`

A departure is viable for alerting when its leave window has not already been consumed by the current session-start rules.

Default timing behavior:

- Arrival buffer has a global default.
- Each saved commute can override the global arrival buffer.
- Walking speed is configured globally.
- Walking time is calculated per saved commute from selected origin to selected stop.
- MVP does not support per-commute manual walking-time overrides.

Working default values:

- Initial arrival buffer can use the example default of arriving 1-3 minutes early.
- Initial walking speed should use a normal walking pace until user testing suggests better defaults.

## Notification Behavior

Each viable departure window produces two notification moments:

- Window open: calm and direct. The user can now leave and arrive inside the configured buffer.
- Final call: more urgent. This is the latest safe leave time for that departure.

MVP notification rules:

- Notifications use platform default sound/vibration behavior.
- Notifications have no action buttons.
- Tapping a notification opens the app.
- There is no separate "missed departure" notification in MVP.
- Missed departures are reflected in the in-app state and the app moves on to the next departure/window.
- If notification permission is denied, background notification behavior is unavailable and the app shows that degraded state clearly.

Example copy direction:

- Window open: "Leave now for tram 4 at 08:30."
- Final call: "Final call for tram 4 at 08:30."
- Merged alternatives: "Leave now for tram 4 at 08:30, or metro 52 at 08:31."

## Multi-Line And Merge Rules

A saved commute can watch multiple selected line/direction pairs from the same stop.

Default rule:

- Treat each departure separately.

Merge rule:

- If multiple selected departures from the same stop have leave-window start and end times within a small tolerance, such as 1 minute, group them into one notification/window group.
- For merged alternatives, lead with the earliest departure.
- Include other alternatives as supporting details where space allows.
- Sorting inside a group is by departure time, earliest first.

Skip behavior:

- Home provides an in-app "skip this departure" action while a watch is active.
- Skipping suppresses all remaining alerts for the current departure.
- If the current item is a merged window group, skipping suppresses all remaining alerts for that merged group. This keeps the action aligned with what the user sees and avoids duplicate alerts for the same practical leaving opportunity.

## Watch Session Lifecycle

### Starting

A session can start in two ways:

- User manually starts a saved commute.
- An enabled saved commute auto-starts during its configured schedule.

When a session starts:

- Fetch departures from the transit API.
- Calculate leave windows for selected line/direction pairs.
- Schedule upcoming window-open and final-call notifications for viable windows.
- Return to Home and show the active watch dashboard.

If watching starts while a leave window is already open:

- Show that state in-app.
- Do not send late notifications for that departure or merged group.
- Do not send its final-call notification either.

If watching starts after the final-call time for a departure:

- Treat that departure as missed/too late in-app.
- Do not notify for it.
- Move to the next viable departure.

### Active Watching

While active:

- Keep showing the current/next actionable departure.
- Keep showing the next few upcoming departures and leave windows.
- Continue alerting for upcoming windows until the user taps "I'm leaving," stops/replaces the session, the schedule completes, or an error stops the session.

### User Taps "I'm Leaving"

MVP uses a manual "I'm leaving" action.

After tapping:

- Further notifications for that active commute session are silenced.
- The app keeps showing the relevant departure context in-app.
- The active session ends after a fixed timeout.
- Working timeout default: 30 minutes.

MVP does not store "caught" or "missed" history and does not use behavior learning.

### Schedule End

Auto-start schedules use selected days plus one time range, such as weekdays 07:30-09:00.

When a schedule's time range ends:

- Do not start or schedule new leave windows.
- If a leave window is currently active, let that window finish.
- Then stop the watch session.

### Errors

If the transit API becomes unavailable during an active watch:

- Stop watching.
- Cancel pending leave-window notifications for that session.
- Show an error in the app.
- If the app is backgrounded, send a notification that watching stopped because transit data is unavailable.

If the API is reachable but returns no upcoming departures:

- Keep the session active until the schedule/session rules end.
- Show a "no upcoming departures" state in-app.
- Retry on the normal refresh cadence.
- Do not send leave-window notifications until departures are available.

## Setup And Onboarding

First-run onboarding:

- Guide the user through creating a saved place.
- Then guide the user through creating the first saved commute.
- Preselect the newly created saved place as the commute origin.
- Request notification permission after the first commute is created, or when the user starts watching/enables a schedule.

Saved commute setup flow:

1. Search/select stop first.
2. Choose one or more lines and API-provided directions/headsigns serving that stop.
3. Choose origin from saved places.
4. Calculate walking time from origin to stop using the global walking-speed preference.
5. Configure arrival buffer, using global default unless overridden.
6. Optionally configure an auto-start schedule.

Auto-start schedule behavior:

- Schedule is selected days plus a time range.
- If a schedule is configured during setup, it is enabled by default.
- A saved commute's enabled/disabled toggle controls auto-start only.
- Manual start remains available when auto-start is disabled.

## Primary Screens

### Saved Commutes List

MVP uses a simple list of saved commutes.

Each row should show:

- Stop.
- Selected line/direction summary.
- Origin place.
- Schedule summary if configured.
- Auto-start enabled/disabled state.
- Manual start action.

Richer dashboard or calendar views can come later.

### Home With Active Watch

When a watch is active, Home becomes the active-watch dashboard instead of sending the user to a separate watch screen. It should show:

- Primary current/next actionable departure panel.
- Action headline, such as "Get ready," "Leave now," or "Final call."
- Exact departure time.
- Leave window times.
- Stop, line, and direction/headsign details.
- Walking time.
- "I'm leaving" action.
- "Skip this departure" action.
- List of the next few upcoming departures with leave windows.
- Clear notification-disabled or API-error state when relevant.

Use hybrid status language:

- Lead with the action.
- Include precise timing and transit details.

### Saved Places

Saved places screen or flow should support:

- Add saved place.
- Edit saved place name/location.
- Delete saved place when it is not required by an active/saved commute, or require the user to resolve affected commutes first.

### Settings

Settings should include:

- Global arrival buffer default.
- Global walking-speed preference.
- Notification permission/status entry point.

## Data And API Needs

Transit API requirements:

- Stop search.
- Lines serving a stop.
- API-provided direction/headsign values for selected lines at a stop.
- Upcoming departures for selected stop and selected line/direction pairs.

Walking-time requirements:

- Location/address selection for saved places.
- Walking-time calculation from saved place to selected stop.
- Ability to account for the global walking-speed preference.

Persistence requirements:

- Saved places.
- Saved commutes.
- Global settings.
- Schedule configuration.
- Current active watch session state enough to recover after app restart/backgrounding.

No MVP persistence for:

- Caught/missed history.
- Behavioral learning.
- Destination data.
- Favorite stops.

## Non-Goals For MVP

- Full trip planning.
- Transfer planning.
- Destination-based route search.
- Ad-hoc current-location watches.
- Real-time delay recalculation and alert rescheduling.
- User learning based on caught/missed behavior.
- Multiple active watch sessions.
- Commute pairing for outbound/return trips.
- Favorite stops independent of saved commutes.
- Notification action buttons.
- Custom notification sounds or vibration styles.
- Calendar-style schedule management.

## Later Opportunities

- Location-based detection that the user has left.
- Delay-aware recalculation with clear rules for when alerts move or lock.
- Ad-hoc watch from current location.
- Paired morning/return commutes.
- Favorite stops.
- Richer dashboard showing next scheduled commute.
- Learned walking-speed or buffer suggestions.
- Notification actions, such as "I'm leaving" or "stop watching."
- Multiple schedules per saved commute.
