# Leave Window Design System

Version: 0.3.1
Status: Implemented baseline
Last updated: 2026-04-29

## Purpose

This document describes the current design system for the transit leave-window companion app. It supersedes the earlier research-only pass and should be treated as the design source of truth alongside `style.md`.

The product should feel like a calm, playful departure cockpit: expressive enough to be memorable, but always centered on one decision: when should the user leave?

## Current Product Shape

The app is a timing companion, not a route planner.

Core flow:

- The user saves places.
- The user creates a commute by choosing an origin place, one stop, one line/direction, timing defaults, and optional auto-start schedule.
- Home is the primary dashboard.
- If a watch is active, Home shows the active watch card, actions, next windows, and saved commutes in one view.
- There is no separate active-watch screen.
- Settings is a normal page with a back button and horizontal slide/fade transition.
- Wear OS is a companion surface focused on active-session glanceability and quick actions.

## Current Design Principles

- Make the timing state impossible to miss.
- Keep route and stop context visible, but secondary to the leave decision.
- Use expressive color, shape, and motion without visual clutter.
- Use flat surfaces: no shadows.
- Keep actions reachable on narrow screens.
- Keep content edge-to-edge where appropriate, but preserve scroll clearance above Android navigation controls.
- Prefer simple, literal copy over clever copy in time-critical states.

## Material 3 Direction

The app uses Compose Multiplatform with Material 3 as the component and token base.

Adopted Material 3 ideas:

- Role-based color via `MaterialTheme.colorScheme`.
- Custom theme specs through `LeaveTheme`.
- Expressive shape scale.
- Flat cards with borders and tonal surfaces instead of elevation.
- Large type only for primary state and timing.
- Component states expressed with color, shape, border weight, and animation.

Avoid:

- Generic white Material defaults.
- Decorative gradients/orbs.
- Heavy shadows.
- Nested cards.
- Route colors as large full-surface backgrounds unless contrast is guaranteed.

## Wear OS Direction

The Wear OS app uses Wear Compose Material3 and is a separate Android artifact, not a responsive variant of the full app.

Wear design principles:

- Show the active leave decision first.
- Use black app and tile backgrounds.
- Use the flat blue draining water-level background only on the active watch screen as a countdown to final call.
- Use a thin pulsing red edge ring as the final-call visual cue.
- Keep screens vertical, shallow, and glanceable.
- Keep setup and configuration on the phone.
- Use Tile and Ongoing Activity surfaces for re-entry into the active watch state.
- Treat route/status colors as small accents over black, not as full-screen decorative themes.

## Theme System

Theme implementation lives in:

- `composeApp/src/commonMain/kotlin/com/samex/kmt_hackathon/LeaveTheme.kt`

Selectable themes:

- `Sunrise`: warm default with yellow signal energy.
- `Lagoon`: cool blue/teal.
- `Grove`: green and grounded.
- `Berry`: soft red/pink.

Removed themes:

- The high-contrast IntelliJ theme has been removed.

Theme responsibilities:

- App `ColorScheme`.
- Theme-aware watch status colors.
- Status backgrounds for app-level state shifts.
- Shape scale.
- Typography overrides.
- Theme picker metadata.

## Color Roles

### App Base

- Background: soft, theme-specific neutral.
- Surface: quiet cards and panels.
- Surface variant: inactive chips, grouped controls, helper surfaces.
- Outline variant: borders and dividers.
- On-surface: primary text.
- On-surface variant: metadata and secondary text.

### Watch Status

- Get ready: route/secondary tone.
- Leave now: signal/primary tone.
- Final call: warm urgent tone.
- Missed: error tone.

The app background animates with active watch state, but content surfaces remain readable and stable.

## Typography

Use the M3 type scale.

Current hierarchy:

- App title: `headlineMedium`.
- Active watch headline: `headlineLarge` on compact screens, `displaySmall` on wider screens.
- Card titles: `titleLarge` / `titleMedium`.
- Context and route text: `bodyLarge` / `bodyMedium`.
- Metrics and helper labels: `labelLarge` / `labelMedium`.

Rules:

- Hero-scale type is reserved for the active watch state.
- Compact panels should not use hero-scale type.
- Text must wrap or truncate cleanly on narrow screens.
- Letter spacing should remain default/zero unless the Material type style defines otherwise.

## Shape And Surfaces

Current shape scale:

- Extra small: 6dp.
- Small: 10dp.
- Medium: 14dp.
- Large: 20dp.
- Extra large: 28dp.
- Full pill: route chips, badges, primary compact controls.

Surface rules:

- No shadows.
- Use `flatCardElevation()` for cards.
- Use borders for separation.
- Use tonal color and shape for hierarchy.
- Do not put UI cards inside other cards.

## Motion

Implemented motion:

- Screen transition into/out of Settings uses horizontal slide plus fade.
- Active watch card animates content size.
- Watch colors animate between states.
- Leave-window countdown updates as time changes.
- Progress updates from `nowSecondsOfDay`, so it changes per second.
- The phone active card keeps the leave-window detail as a simple countdown panel; avoid generic loading bars for the leave window.

Motion rules:

- Keep motion short and functional.
- Do not animate in ways that delay time-critical actions.
- Do not add decorative motion without a product reason.

## Core Components

### `ActiveWatchHero`

Primary active-watch surface on Home.

Current headline copy:

- Before window opens: `Leave at <time>`.
- Window open: `Leave now`.
- Final-call minute: `Final call`.
- After tapping `I'm leaving`: `Departure in <countdown>`.

Behavior:

- Shows stop, route chips, walking/departure metrics, the window countdown, and actions.
- After `I'm leaving`, it pins the selected departure/group and counts down to that departure.
- After `I'm leaving`, the window countdown is hidden because the headline owns the departure countdown.
- At final call, the window countdown is hidden because the headline owns the urgent state.
- The watch ends when the selected departure happens.
- The previous "Notifications silenced after leaving" banner has been removed.

### `LeaveWindowCountdown`

Simple per-second countdown panel for the current leave window.

Rules:

- Show the primary state as `Window closes in <countdown>` while the window is open.
- Before the window opens, show the window-open time instead of a countdown.
- Keep secondary timing context to one compact line for leave and final-call times.
- Final-call and missed states use urgent status color.
- Must remain legible in all themes.

### `RouteChip`

Compact route/direction indicator.

Rules:

- Use label text, not color alone.
- Use route accent color as a small visual cue.
- Keep chips compact and wrap-safe.

### `CommuteSummaryCard`

Saved commute card on Home.

Content:

- Origin.
- Stop.
- One selected line/direction.
- Schedule/active status.
- Start, edit, delete, and auto-start controls.

### `CommuteSetup`

Staged setup flow.

Current behavior:

- One decision per stage.
- Searchable stop and line steps.
- Line step allows exactly one line/direction.
- Review step says `Line`, not `Lines`.

### `CommuteEditScreen`

Expandable section edit flow.

Current behavior:

- The user does not need to go backward through the setup flow.
- Sections expand in place: origin, stop, line, timing, schedule.
- Editing older saved data coerces line selection to one line/direction.

### Settings

Settings is a normal page, not a sheet.

Current order:

1. Overview card.
2. Arrival window.
3. Walking pace.
4. Notifications.
5. Theme picker.

The theme picker should remain the final block.

## Layout And Insets

Root layout:

- May extend visually to the bottom edge.
- Applies safe top and horizontal content insets.
- Uses zero root bottom padding.

Scrollable screens:

- Must include enough trailing scroll space for Android navigation controls.
- Prefer scroll-tail spacers using `WindowInsets.navigationBars` over adding root bottom padding.

Responsive rules:

- Narrow screens use single-column layout and full-width buttons.
- Wider screens can place action buttons in rows.
- Header buttons should stay on one line until they truly do not fit.
- Text inside buttons and compact controls must not clip.

## Accessibility

- Do not communicate timing status with color alone.
- Keep text contrast high on custom containers.
- Keep touch targets at least 48dp where practical.
- Ensure bottom actions can scroll above system navigation.
- Support readable labels for route chips and timing metrics.
- Avoid excessive animation.

## Open Design Questions

- Should the product name become `HopOn`, or should the UI remain `Leave Window` for now?
- Should Android dynamic color be introduced later, or should the fixed four-theme picker remain the main personalization model?
- Should the active watch card become more compact on very short screens?
- Should schedule/auto-start be visually separated more strongly from manual commutes?
- Should the route chip system later include real transit mode icons?
