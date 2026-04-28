# Leave Window Design System

Version: 0.1.0  
Status: Research direction  
Last updated: 2026-04-28

## Purpose

This document captures the first design research pass for improving the app UI. It is not an implementation plan yet. Its job is to define the desired product feeling, Material 3 principles to adopt, and the first design-system concepts for the app.

The app should feel like a calm, playful departure cockpit: focused on the one useful decision, "When should I leave?", while still feeling warm, expressive, and enjoyable to use.

## Research Sources

- Material 3 in Compose: https://developer.android.com/develop/ui/compose/designsystems/material3
- Material 3 Expressive design language: https://developer.android.com/design/ui/wear/guides/get-started/design-language
- Google Android and Wear OS Material 3 Expressive launch: https://blog.google/products-and-platforms/platforms/android/material-3-expressive-android-wearos-launch/
- Material 3 Expressive discussion and research summary: https://9to5google.com/2025/05/05/material-3-expressive-leak/
- Material Design home and M3 Expressive blog pages: https://m3.material.io/ and https://m3.material.io/blog/building-with-m3-expressive

Note: `m3.material.io` is a JavaScript-rendered site. Static fetches expose metadata, theme tokens, fonts, and page structure, but not full page text. The findings below therefore combine the Material site, its reachable page metadata/styles, and official Android/Google pages that expose the M3 Expressive principles in crawlable form.

## Material 3 Takeaways

Material 3 should be treated as a design system, not only as a component library.

Useful principles for this app:

- Use role-based color instead of ad hoc color values.
- Let `MaterialTheme.colorScheme`, typography, shapes, and elevation drive consistency.
- Use color roles for state and emphasis: primary, secondary, tertiary, surface, surface variant, error, and matching on-colors.
- Use tonal elevation and surface layering instead of heavy shadows.
- Use the M3 type scale deliberately. Reserve large expressive type for the main decision, not every section title.
- Use shape as a product voice. Shape should communicate containment, importance, and interaction.
- Use dynamic color on Android where appropriate, but preserve a strong fallback brand theme.
- Use Material components, but customize their roles, shapes, and hierarchy so the product does not feel generic.

## Material 3 Expressive Takeaways

M3 Expressive expands Material You with more personality, stronger visual hierarchy, and more fluid interaction.

Relevant ideas:

- Interfaces should be modern, distinct, and emotionally warmer than plain "clean" UIs.
- Expressiveness comes from color, shape, size, motion, and containment.
- Key actions should be easier to spot, not merely prettier.
- Grouped containers should organize related controls and make flows easier to parse.
- Typography can be more expressive through weight, size, and hierarchy.
- Components can feel more responsive through shape changes, springy motion, and adaptive button groups.
- Important information should be glanceable and front-and-center.

For this app, expressiveness should never compete with timing clarity. The UI can be playful in surfaces, route chips, progress bands, and motion, but the leave decision must remain unambiguous.

## Product Mood

Target mood:

- Calm
- Timely
- Friendly
- Transit-aware
- Slightly kinetic
- Personal but not cute
- Expressive but still practical

Avoid:

- Generic admin dashboard styling
- Plain white Material defaults
- Overly decorative gradients
- Dense lists without visual grouping
- Tiny action buttons for important moments
- Route colors taking over full cards
- Animation that delays decision-making

## Core Visual Metaphor

Use the leave window as a signal or runway.

The product is about moving from "getting ready" to "leave now" to "final call." The main visual system should make this timing arc visible:

- A capsule-shaped leave-window band.
- A moving or filled progress region.
- State colors that shift from calm to active to urgent.
- Route chips attached to departures.
- A large active watch surface that feels like the app's cockpit.

## Color Direction

Use a warm neutral base with expressive but controlled accents.

### Base Roles

- App background: warm off-white or soft neutral, not pure white.
- Surface: slightly lighter than background.
- Surface variant: used for grouped controls, inactive cards, and permission/status panels.
- Outline variant: subtle dividers, card separators, timeline ticks.
- On surface: deep neutral text.
- On surface variant: secondary text and metadata.

### Brand Roles

- Primary: transit green or teal. Used for active leave state, primary action, selected setup choices.
- Secondary: route blue. Used for stop/line context, inactive timing structure, selected route groups.
- Tertiary: warm amber. Used for final-call urgency and near-departure emphasis.
- Error: coral/red. Used for missed, denied permissions, or invalid setup state.

### Route Accent Colors

Transit line colors should be used as accents:

- Small chips
- Leading rails
- Dots
- Badges
- Timeline markers

They should not become full-card backgrounds unless the card is very small and text contrast is guaranteed.

## Typography Direction

Use the M3 type scale, but make numerals and current state the hero.

Suggested hierarchy:

- Current status: `headlineLarge` or `displaySmall` depending on screen width.
- Main time/departure: large numeric treatment, preferably `displaySmall` or custom numeral style.
- Section/card titles: `titleMedium` or `titleLarge`.
- Route and stop context: `bodyLarge`.
- Metadata and counters: `labelMedium` / `bodySmall`.

Rules:

- Large text is reserved for active watch state and primary time information.
- Compact cards should not use hero-scale text.
- Letter spacing should stay 0 unless the type scale explicitly requires otherwise.
- Numerals should be highly legible and steady in layout.

## Shape Direction

Shape should create personality and relationship.

Initial shape scale:

- Extra small: 6dp for small internal controls.
- Small: 10dp for chips and compact fields.
- Medium: 14dp for list rows and setup choices.
- Large: 20dp for commute cards and grouped panels.
- Extra large: 28dp for active watch hero and major setup surfaces.
- Full: pill buttons, route chips, progress bands.

Rules:

- More important and more interactive surfaces can be rounder.
- Cards should not be nested inside cards.
- Grouped controls should use shared container shape.
- Active watch can use a distinctive rounded surface to become recognizable.

## Elevation And Surfaces

Prefer tonal layering over obvious shadows.

Surface layers:

- Level 0: app background.
- Level 1: normal cards and panels.
- Level 2: selected commute card, active setup step.
- Level 3: active watch hero, urgent current-state panel.

Use elevation sparingly:

- Active watch gets strongest container emphasis.
- Normal commute cards stay quiet.
- Permission warnings and errors use color/state, not heavy shadow.

## Motion Direction

Motion is a later implementation step, but the design system should anticipate it.

Good motion candidates:

- Leave-window progress band fills over time.
- Active state shifts color when the window opens.
- Final call uses a short pulse or emphasis transition.
- Setup step transitions slide/fade between stages.
- Pressed route chips subtly scale or morph.
- "I'm leaving" action resolves the current watch with a satisfying but short transition.

Motion rules:

- Motion should never delay a time-critical action.
- Keep frequent transitions short.
- Use springy motion for state changes, not constant decoration.
- Respect reduced-motion settings when available.

## Component Inventory

### `LeaveTheme`

Central theme entry point.

Responsibilities:

- Selectable app color themes inspired by Tack's red/yellow/green/blue palette picker, plus a high-contrast IntelliJ-inspired theme.
- Theme-aware active-watch status colors.
- Optional Android dynamic color later.
- App typography.
- Shape scale.
- Surface defaults.

### `ActiveWatchHero`

Main watch surface.

Content:

- Current status.
- Stop name.
- Route chips.
- Departure time.
- Leave window range.
- Walking time.
- Primary action.

Behavior:

- Dominant active-watch surface on Home.
- Replaces the old separate active-watch screen so the user always returns to one dashboard.
- State color changes by timing status.

### `LeaveWindowProgress`

Capsule timeline for the current departure group.

Segments:

- Get ready.
- Leave now.
- Final call.
- Missed/expired.

It should make the user's current position in the timing window immediately visible.

### `RouteChip`

Compact line/direction indicator.

Content:

- Line short name.
- Direction/headsign.
- Optional mode icon later.
- Line color accent.

### `CommuteSummaryCard`

Saved commute card for home.

Content:

- Origin.
- Stop.
- Selected route chips.
- Auto-start status.
- Start action.

Design:

- Quiet unless active.
- Uses route accents only in small areas.
- No giant text.

### `SetupStepSurface`

Container for the staged setup flow.

Responsibilities:

- One decision per step.
- Searchable stop/line steps.
- Selected choices visually obvious.
- Review step before save.

### `PermissionStatusBanner`

Small, stateful surface for notification permission.

Tone:

- Clear but not alarming.
- Uses warning/error color only when permission is denied.
- Avoids consuming the primary visual hierarchy unless notifications are required for the current action.

## Screen Direction

### Home

Home should answer:

- Is there an active watch?
- What commutes can I start?
- Can I add or edit routine data?

Desired structure:

- Full active-watch hero if a session is active.
- Inline watch actions and the next few leave windows directly under the hero.
- Commute cards below.
- Floating or prominent "add commute" action.
- Places/settings as secondary actions.

### Active Watch Section

This is the emotional core of the app.

Desired structure:

- Large state hero.
- Leave-window progress band.
- Route chips and departure group.
- Primary "I'm leaving" action.
- Secondary "skip" and "stop" actions.
- Upcoming windows as a quieter list.

### Commute Setup

Keep the staged setup flow.

Future visual improvements:

- Step indicator as expressive pills.
- Selected choices with strong container color.
- Stop/line search as a rounded search surface.
- Review step as a polished summary card.

### Settings

Settings should be quieter than Home/Watch.

Use:

- Grouped surfaces.
- Theme picker cards with compact color swatches and a clear selected state.
- Small descriptions.
- Tighter controls.
- Clear current defaults.

## Responsive Direction

Narrow phone:

- Single-column.
- Active hero first.
- Full-width primary buttons.
- Route chips wrap.
- Setup shows one step at a time.

Wide phone / desktop:

- Consider supporting pane or list/detail layout later.
- Navigation can become a rail.
- Active watch and upcoming windows can sit side by side.
- Home can show commute list and active watch panel together.

## Accessibility Rules

- Text contrast must be checked for every custom container color.
- Touch targets should remain at least 48dp.
- Do not communicate timing status with color alone.
- Route colors need text labels.
- Large numeric time should support system text scaling.
- Important buttons must remain visible and reachable on narrow screens.
- Motion should be optional/reduced when platform support is available.

## Implementation Sequence Proposal

Do not redesign every screen at once. Build the system in layers.

1. Theme foundation:
   - `LeaveTheme`
   - color scheme
   - typography
   - shapes
   - app background

2. Shared components:
   - route chips
   - status banners
   - card/surface wrappers

3. Active watch redesign:
   - active hero
   - leave-window progress
   - primary action layout

4. Home redesign:
   - active watch summary
   - saved commute cards
   - primary add/start actions

5. Commute setup visual pass:
   - keep staged flow
   - improve step surface and choices

6. Motion pass:
   - active state transitions
   - progress band
   - setup transitions

## Open Design Questions

- Should the app support Android dynamic color by default, or preserve a fixed branded transit palette?
- Should line colors come from real transit agencies later, or remain app-owned semantic colors for MVP?
- How playful can the active watch hero be before it becomes distracting?
- Should "Leave now" use green as positive/actionable, or amber as urgency?
- Should the final call be an urgent state inside the same hero, or a distinct elevated warning surface?

## Current Recommendation

Start with a fixed fallback theme inspired by M3 Expressive, then optionally enable Android dynamic color later. The MVP needs a strong brand direction first; dynamic color can personalize it once the base hierarchy is proven.

The first implementation should focus on theme and active watch. That is where visual improvement will most directly improve the product.
