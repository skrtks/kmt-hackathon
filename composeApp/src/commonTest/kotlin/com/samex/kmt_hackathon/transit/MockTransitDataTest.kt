package com.samex.kmt_hackathon.transit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MockTransitDataTest {
    @Test
    fun datasetReferencesExistingStopsLinesAndDirections() {
        val stopIds = MockTransitData.stops.map { it.id }.toSet()
        val lineIds = MockTransitData.lines.map { it.id }.toSet()
        val directionIds = MockTransitData.directions.map { it.id }.toSet()

        MockTransitData.directions.forEach { direction ->
            assertTrue(direction.lineId in lineIds, "Unknown line '${direction.lineId}'")
            assertTrue(direction.stopIds.all { it in stopIds }, "Unknown stop in '${direction.id}'")
        }

        MockTransitData.departures.forEach { departure ->
            val direction = assertNotNull(MockTransitData.directionById(departure.directionId))

            assertTrue(departure.stopId in stopIds, "Unknown stop '${departure.stopId}'")
            assertTrue(departure.lineId in lineIds, "Unknown line '${departure.lineId}'")
            assertTrue(departure.directionId in directionIds, "Unknown direction '${departure.directionId}'")
            assertTrue(departure.stopId in direction.stopIds, "Departure stop is not served by its direction")
            assertEquals(direction.lineId, departure.lineId)
            assertEquals(direction.headsign, departure.headsign)
        }
    }

    @Test
    fun linesServingStopReturnsUniqueLinesForSharedStop() {
        val lines = MockTransitData.linesServingStop("stop_de_pijp").map { it.shortName }

        assertEquals(listOf("4", "12", "52"), lines)
    }

    @Test
    fun departuresForReturnsSortedDeparturesForSelectedDirections() {
        val departures = MockTransitData.departuresFor(
            stopId = "stop_vijzelgracht",
            selections = setOf(
                LineSelection("line_tram_4", "dir_tram_4_zuid"),
                LineSelection("line_metro_52", "dir_metro_52_zuid"),
            ),
            fromTimeMinutes = minutes("08:20"),
            limit = 4,
        )

        assertEquals(
            listOf("08:21", "08:21", "08:27", "08:31"),
            departures.map { MockTransitData.formatTime(it.scheduledTimeMinutes) },
        )
        assertEquals(
            listOf("52", "4", "52", "4"),
            departures.map { MockTransitData.lineById(it.lineId)?.shortName },
        )
    }

    private fun minutes(value: String): Int {
        val parts = value.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }
}
