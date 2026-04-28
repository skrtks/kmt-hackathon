# Mock Transit Data

The first development dataset lives in:

- `composeApp/src/commonMain/kotlin/com/samex/kmt_hackathon/transit/MockTransitData.kt`

It is a static, Amsterdam-inspired fixture for early app development. It is not live data and should not be treated as an accurate public transport schedule.

## Contents

- Stops with ids, names, and approximate coordinates.
- Lines with ids, short names, modes, and display colors.
- API-style directions/headsigns per line.
- Fixed weekday departure times generated from simple terminal schedules and per-stop offsets.

## Included Lines

- Tram 4 between Centraal Station and Station Zuid.
- Tram 12 between Station Sloterdijk and Amstelstation.
- Metro 52 between Noord and Station Zuid.
- Bus 15 between Station Sloterdijk and Station Zuid.

## Useful Development Scenarios

- `stop_de_pijp` is served by tram 4, tram 12, and metro 52.
- `stop_vijzelgracht` has tram 4 and metro 52 departures that align closely enough to exercise merged leave-window behavior.
- The dataset includes bus, tram, and metro modes.

## Query Helpers

`MockTransitData` includes helpers for:

- Looking up stops, lines, and directions by id.
- Listing directions serving a stop.
- Listing unique lines serving a stop.
- Getting upcoming departures for a stop and selected line/direction pairs.
- Formatting minute-of-day values as `HH:mm`.
