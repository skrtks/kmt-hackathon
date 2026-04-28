package com.samex.kmt_hackathon.transit

enum class TransitMode {
    Bus,
    Tram,
    Metro,
}

enum class ServiceDay {
    Weekday,
    Saturday,
    Sunday,
}

data class TransitStop(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

data class TransitLine(
    val id: String,
    val shortName: String,
    val mode: TransitMode,
    val colorHex: String,
)

data class LineDirection(
    val id: String,
    val lineId: String,
    val headsign: String,
    val stopIds: List<String>,
)

data class Departure(
    val id: String,
    val stopId: String,
    val lineId: String,
    val directionId: String,
    val headsign: String,
    val serviceDay: ServiceDay,
    val scheduledTimeMinutes: Int,
)

data class LineSelection(
    val lineId: String,
    val directionId: String,
)

data class TransitDataset(
    val stops: List<TransitStop>,
    val lines: List<TransitLine>,
    val directions: List<LineDirection>,
    val departures: List<Departure>,
)

object MockTransitData {
    val stops: List<TransitStop> = listOf(
        TransitStop("stop_centraal", "Centraal Station", 52.3791, 4.8994),
        TransitStop("stop_dam", "Dam", 52.3731, 4.8936),
        TransitStop("stop_rokin", "Rokin", 52.3678, 4.8933),
        TransitStop("stop_vijzelgracht", "Vijzelgracht", 52.3599, 4.8917),
        TransitStop("stop_de_pijp", "De Pijp", 52.3546, 4.8910),
        TransitStop("stop_zuid", "Station Zuid", 52.3392, 4.8731),
        TransitStop("stop_sloterdijk", "Station Sloterdijk", 52.3889, 4.8372),
        TransitStop("stop_westergas", "Westergasfabriek", 52.3862, 4.8691),
        TransitStop("stop_leidseplein", "Leidseplein", 52.3640, 4.8838),
        TransitStop("stop_museumplein", "Museumplein", 52.3584, 4.8811),
        TransitStop("stop_amstelstation", "Amstelstation", 52.3465, 4.9179),
        TransitStop("stop_noord", "Noord", 52.4022, 4.9318),
    )

    val lines: List<TransitLine> = listOf(
        TransitLine("line_tram_4", "4", TransitMode.Tram, "#D62828"),
        TransitLine("line_tram_12", "12", TransitMode.Tram, "#0077B6"),
        TransitLine("line_metro_52", "52", TransitMode.Metro, "#6A4C93"),
        TransitLine("line_bus_15", "15", TransitMode.Bus, "#2A9D8F"),
    )

    val directions: List<LineDirection> = listOf(
        LineDirection(
            id = "dir_tram_4_zuid",
            lineId = "line_tram_4",
            headsign = "Station Zuid",
            stopIds = listOf(
                "stop_centraal",
                "stop_dam",
                "stop_rokin",
                "stop_vijzelgracht",
                "stop_de_pijp",
                "stop_zuid",
            ),
        ),
        LineDirection(
            id = "dir_tram_4_centraal",
            lineId = "line_tram_4",
            headsign = "Centraal Station",
            stopIds = listOf(
                "stop_zuid",
                "stop_de_pijp",
                "stop_vijzelgracht",
                "stop_rokin",
                "stop_dam",
                "stop_centraal",
            ),
        ),
        LineDirection(
            id = "dir_tram_12_amstelstation",
            lineId = "line_tram_12",
            headsign = "Amstelstation",
            stopIds = listOf(
                "stop_sloterdijk",
                "stop_westergas",
                "stop_leidseplein",
                "stop_museumplein",
                "stop_de_pijp",
                "stop_amstelstation",
            ),
        ),
        LineDirection(
            id = "dir_tram_12_sloterdijk",
            lineId = "line_tram_12",
            headsign = "Station Sloterdijk",
            stopIds = listOf(
                "stop_amstelstation",
                "stop_de_pijp",
                "stop_museumplein",
                "stop_leidseplein",
                "stop_westergas",
                "stop_sloterdijk",
            ),
        ),
        LineDirection(
            id = "dir_metro_52_zuid",
            lineId = "line_metro_52",
            headsign = "Station Zuid",
            stopIds = listOf(
                "stop_noord",
                "stop_centraal",
                "stop_rokin",
                "stop_vijzelgracht",
                "stop_de_pijp",
                "stop_zuid",
            ),
        ),
        LineDirection(
            id = "dir_metro_52_noord",
            lineId = "line_metro_52",
            headsign = "Noord",
            stopIds = listOf(
                "stop_zuid",
                "stop_de_pijp",
                "stop_vijzelgracht",
                "stop_rokin",
                "stop_centraal",
                "stop_noord",
            ),
        ),
        LineDirection(
            id = "dir_bus_15_zuid",
            lineId = "line_bus_15",
            headsign = "Station Zuid",
            stopIds = listOf(
                "stop_sloterdijk",
                "stop_westergas",
                "stop_museumplein",
                "stop_zuid",
            ),
        ),
        LineDirection(
            id = "dir_bus_15_sloterdijk",
            lineId = "line_bus_15",
            headsign = "Station Sloterdijk",
            stopIds = listOf(
                "stop_zuid",
                "stop_museumplein",
                "stop_westergas",
                "stop_sloterdijk",
            ),
        ),
    )

    val departures: List<Departure> = buildList {
        addDirectionDepartures(
            directionId = "dir_tram_4_zuid",
            serviceDay = ServiceDay.Weekday,
            firstTerminalDeparture = time("07:00"),
            lastTerminalDeparture = time("09:30"),
            intervalMinutes = 10,
            stopOffsets = listOf(0, 4, 7, 11, 15, 22),
        )
        addDirectionDepartures(
            directionId = "dir_tram_4_centraal",
            serviceDay = ServiceDay.Weekday,
            firstTerminalDeparture = time("07:04"),
            lastTerminalDeparture = time("09:34"),
            intervalMinutes = 10,
            stopOffsets = listOf(0, 7, 11, 15, 18, 23),
        )
        addDirectionDepartures(
            directionId = "dir_tram_12_amstelstation",
            serviceDay = ServiceDay.Weekday,
            firstTerminalDeparture = time("07:03"),
            lastTerminalDeparture = time("09:33"),
            intervalMinutes = 12,
            stopOffsets = listOf(0, 6, 15, 18, 23, 31),
        )
        addDirectionDepartures(
            directionId = "dir_tram_12_sloterdijk",
            serviceDay = ServiceDay.Weekday,
            firstTerminalDeparture = time("07:06"),
            lastTerminalDeparture = time("09:30"),
            intervalMinutes = 12,
            stopOffsets = listOf(0, 8, 13, 16, 25, 31),
        )
        addDirectionDepartures(
            directionId = "dir_metro_52_zuid",
            serviceDay = ServiceDay.Weekday,
            firstTerminalDeparture = time("07:02"),
            lastTerminalDeparture = time("09:32"),
            intervalMinutes = 6,
            stopOffsets = listOf(0, 7, 10, 13, 15, 20),
        )
        addDirectionDepartures(
            directionId = "dir_metro_52_noord",
            serviceDay = ServiceDay.Weekday,
            firstTerminalDeparture = time("07:01"),
            lastTerminalDeparture = time("09:31"),
            intervalMinutes = 6,
            stopOffsets = listOf(0, 5, 7, 10, 13, 20),
        )
        addDirectionDepartures(
            directionId = "dir_bus_15_zuid",
            serviceDay = ServiceDay.Weekday,
            firstTerminalDeparture = time("07:05"),
            lastTerminalDeparture = time("09:35"),
            intervalMinutes = 15,
            stopOffsets = listOf(0, 8, 17, 28),
        )
        addDirectionDepartures(
            directionId = "dir_bus_15_sloterdijk",
            serviceDay = ServiceDay.Weekday,
            firstTerminalDeparture = time("07:02"),
            lastTerminalDeparture = time("09:32"),
            intervalMinutes = 15,
            stopOffsets = listOf(0, 11, 20, 28),
        )
    }.sortedWith(compareBy<Departure> { it.scheduledTimeMinutes }.thenBy { it.lineId }.thenBy { it.stopId })

    val dataset = TransitDataset(
        stops = stops,
        lines = lines,
        directions = directions,
        departures = departures,
    )

    fun stopById(stopId: String): TransitStop? = stops.firstOrNull { it.id == stopId }

    fun lineById(lineId: String): TransitLine? = lines.firstOrNull { it.id == lineId }

    fun directionById(directionId: String): LineDirection? = directions.firstOrNull { it.id == directionId }

    fun directionsServingStop(stopId: String): List<LineDirection> =
        directions.filter { stopId in it.stopIds }

    fun linesServingStop(stopId: String): List<TransitLine> {
        val lineIds = directionsServingStop(stopId).map { it.lineId }.toSet()
        return lines.filter { it.id in lineIds }
    }

    fun departuresFor(
        stopId: String,
        selections: Set<LineSelection>,
        serviceDay: ServiceDay = ServiceDay.Weekday,
        fromTimeMinutes: Int = 0,
        limit: Int = 12,
    ): List<Departure> =
        departures
            .asSequence()
            .filter { it.stopId == stopId }
            .filter { it.serviceDay == serviceDay }
            .filter { it.scheduledTimeMinutes >= fromTimeMinutes }
            .filter { departure ->
                LineSelection(departure.lineId, departure.directionId) in selections
            }
            .sortedBy { it.scheduledTimeMinutes }
            .take(limit)
            .toList()

    fun formatTime(minutesAfterMidnight: Int): String {
        val normalizedMinutes = minutesAfterMidnight.mod(24 * 60)
        val hours = normalizedMinutes / 60
        val minutes = normalizedMinutes % 60
        return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
    }

    private fun MutableList<Departure>.addDirectionDepartures(
        directionId: String,
        serviceDay: ServiceDay,
        firstTerminalDeparture: Int,
        lastTerminalDeparture: Int,
        intervalMinutes: Int,
        stopOffsets: List<Int>,
    ) {
        val direction = requireNotNull(directionById(directionId)) {
            "Unknown direction '$directionId'"
        }
        require(direction.stopIds.size == stopOffsets.size) {
            "Direction '$directionId' has ${direction.stopIds.size} stops but ${stopOffsets.size} offsets"
        }

        var terminalDeparture = firstTerminalDeparture
        while (terminalDeparture <= lastTerminalDeparture) {
            direction.stopIds.forEachIndexed { stopIndex, stopId ->
                val scheduledTime = terminalDeparture + stopOffsets[stopIndex]
                add(
                    Departure(
                        id = buildDepartureId(directionId, stopId, serviceDay, scheduledTime),
                        stopId = stopId,
                        lineId = direction.lineId,
                        directionId = direction.id,
                        headsign = direction.headsign,
                        serviceDay = serviceDay,
                        scheduledTimeMinutes = scheduledTime,
                    ),
                )
            }
            terminalDeparture += intervalMinutes
        }
    }

    private fun buildDepartureId(
        directionId: String,
        stopId: String,
        serviceDay: ServiceDay,
        scheduledTimeMinutes: Int,
    ): String = "$directionId-$stopId-${serviceDay.name.lowercase()}-$scheduledTimeMinutes"

    private fun time(value: String): Int {
        val parts = value.split(":")
        require(parts.size == 2) { "Time must use HH:mm format" }
        val hours = parts[0].toInt()
        val minutes = parts[1].toInt()
        require(hours in 0..23) { "Hour out of range in '$value'" }
        require(minutes in 0..59) { "Minute out of range in '$value'" }
        return hours * 60 + minutes
    }
}
