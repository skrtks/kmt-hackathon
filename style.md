# Leave Window Style Guide

Version: 0.2.0
Status: Current implementation guide
Last updated: 2026-04-29

## Purpose

This file captures practical UI, copy, and implementation style rules for the current app. Use `design.md` for design-system intent and this file for day-to-day decisions while editing screens and components.

## Product Voice

Tone:

- Clear.
- Calm.
- Direct.
- Slightly playful through shape and color, not through confusing wording.

Avoid:

- Cute filler copy.
- Long explanations inside the app.
- Technical transport-planning language.
- Marketing-style hero copy.

## Core Copy

Active watch headline copy:

- `Leave at <time>` before the leave window opens.
- `Leave now` while the leave window is open.
- `Final call` at the final-call minute.
- `Departure in <countdown>` after the user taps `I'm leaving`.

Primary actions:

- `I'm leaving`
- `Skip this departure`
- `Stop`
- `Start`
- `Edit`
- `Delete`
- `Add commute`
- `Add place`

Setup copy:

- Use `Line`, not `Lines`, because the current app supports exactly one line/direction per commute.
- Empty line state: `Choose one line and direction`.
- Save validation: `Select one line and direction.`

Do not show:

- `Notifications silenced after leaving.`

## Layout Rules

General:

- Home is the main dashboard.
- Active watch content belongs on Home.
- Settings is a page with a back button, not a sheet.
- Keep the theme picker last in Settings.
- Do not add a Home button. Use page navigation and back where needed.

Edge-to-edge:

- Root content may extend visually to the bottom.
- Keep root bottom padding at `0.dp` unless the product direction changes.
- Scrollable screens need bottom scroll clearance for Android navigation controls.
- Use a trailing scroll spacer with `WindowInsets.navigationBars` for scroll clearance.

Narrow screens:

- Use single-column layouts.
- Use full-width primary buttons.
- Let route chips wrap or stack.
- Keep button text from clipping.
- Prefer smaller, tighter headings inside panels and cards.

Wide screens:

- Action buttons may sit in rows.
- Header title and Settings button should stay on one line until they truly cannot fit.
- Avoid adding large empty margins just because the viewport is wider.

## Surface Style

Use:

- Flat cards.
- 1dp to 2dp borders.
- Tonal containers.
- Expressive rounded shapes.
- Clear selected states.

Do not use:

- Shadows.
- Nested cards.
- Decorative blobs/orbs.
- Large gradients.
- Purely decorative illustration.

Cards:

- `CommuteSummaryCard` should stay quiet unless active.
- `ActiveWatchHero` can be visually dominant.
- Settings panels should be calmer than Home.

## Color And Themes

Current selectable themes:

- Sunrise.
- Lagoon.
- Grove.
- Berry.

Removed:

- IntelliJ.

Rules:

- Use `MaterialTheme.colorScheme` and `leaveStatusColors()`.
- Do not hardcode new colors in components unless adding a theme token.
- Status colors should be semantic: get-ready, leave-now, final-call, missed.
- Route colors are accents only; they should not overwhelm text surfaces.

## Typography

Use:

- Hero scale only for active watch headline/time state.
- `titleMedium` / `titleLarge` for card titles.
- `bodyMedium` / `bodyLarge` for context.
- `labelMedium` / `labelLarge` for metrics and chips.

Rules:

- No negative letter spacing.
- Do not scale font size from viewport width.
- Use `maxLines` and ellipsis where route/stop names can be long.
- Prefer exact times and countdowns over relative phrases when timing matters.

## Motion

Allowed:

- Screen slide/fade for page transitions.
- Content-size animation for expanding surfaces.
- Color transitions for watch state.
- Per-second progress and countdown updates.
- Small progress wave in active leave states.

Avoid:

- Decorative looping animations outside the progress indicator.
- Motion that hides or delays critical actions.
- Large layout jumps when status changes.

## Interaction Rules

Commute setup:

- Origin: choose one.
- Stop: choose one.
- Line: choose exactly one line/direction.
- Timing: defaults first, custom overrides optional.
- Review: summarize before saving.

Commute edit:

- Use expandable sections.
- Do not force the user through the setup flow backward.
- Editing old multi-line data should coerce to the first line/direction.

Active watch:

- `I'm leaving` requires a current group.
- After `I'm leaving`, the selected departure is pinned.
- Watch ends when that selected departure time is reached.
- Auto-start should not immediately restart the same commute after a leaving completion inside the same schedule window.

## Implementation Notes

- Keep full-size shared UI in `composeApp/src/commonMain`.
- Keep UI-free domain and transit logic in `shared/src/commonMain`.
- Keep platform services behind `expect/actual` in `core/PlatformServices.kt`.
- Prefer existing model methods in `TransitAppModel` over manipulating state directly from UI.
- Keep validation rules in shared model/helper functions when they affect product behavior.
- Add tests for model behavior changes, especially watch-session lifecycle, scheduling, and persistence.
- For docs-only changes, compile/tests are optional unless the docs describe code that was changed in the same turn.

## Wear OS Rules

- Build Wear OS as a watch-native companion in `:wearApp`; do not shrink the full-size `App()` UI.
- Use Wear Compose Material3 components, not mobile Material3 components.
- Use a black background on watch surfaces.
- Keep hierarchy shallow: active session, no-active state, and simple commute-start entry points.
- Keep essential text at 12sp or larger and touch targets at least 48dp.
- Use short, literal watch copy: `Leave now`, `Final call`, `Leave by <time>`, `No active watch`.
- Hide watch actions until the corresponding phone command path is wired.
- Do not put commute setup, place setup, schedule editing, or theme selection on the watch MVP.
