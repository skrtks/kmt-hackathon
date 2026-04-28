package com.samex.kmt_hackathon.core

import com.samex.kmt_hackathon.transit.Departure
import com.samex.kmt_hackathon.transit.LineDirection
import com.samex.kmt_hackathon.transit.TransitLine
import com.samex.kmt_hackathon.transit.TransitMode
import com.samex.kmt_hackathon.transit.TransitStop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WatchEngineTest {
    private val repository = FakeTransitRepository()
    private val engine = WatchEngine(repository)
    private val origin = SavedPlace("place-home", "Home", GeoPoint(52.0, 4.0))
    private val commute = SavedCommute(
        id = "commute",
        originPlaceId = origin.id,
        stopId = "stop",
        selections = listOf(CommuteLineSelection("line", "direction")),
    )
    private val settings = UserSettings(
        defaultArrivalBuffer = ArrivalBuffer(minEarlyMinutes = 1, maxEarlyMinutes = 3),
        walkingSpeed = WalkingSpeed(metersPerMinute = 80.0),
    )

    @Test
    fun computesLeaveWindowFromDepartureWalkingTimeAndArrivalBuffer() {
        val windows = engine.leaveWindows(
            commute = commute,
            origin = origin,
            settings = settings,
            departures = listOf(departure("dep-1", 8 * 60 + 30)),
        )

        assertEquals(1, windows.size)
        assertEquals(8 * 60 + 26, windows.first().windowOpenMinutes)
        assertEquals(8 * 60 + 28, windows.first().finalCallMinutes)
    }

    @Test
    fun mergesWindowsWithinTolerance() {
        val windows = engine.leaveWindows(
            commute = commute,
            origin = origin,
            settings = settings,
            departures = listOf(
                departure("dep-1", 8 * 60 + 30),
                departure("dep-2", 8 * 60 + 31),
            ),
        )

        val groups = engine.groupWindows(windows, toleranceMinutes = 1)

        assertEquals(1, groups.size)
        assertEquals(2, groups.first().windows.size)
        assertEquals("dep-1", groups.first().primaryWindow.departureId)
    }

    @Test
    fun suppressesNotificationsForWindowAlreadyOpenAtSessionStart() {
        val groups = engine.groupWindows(
            engine.leaveWindows(
                commute = commute,
                origin = origin,
                settings = settings,
                departures = listOf(departure("dep-1", 8 * 60 + 30)),
            ),
        )

        val plans = engine.notificationPlansForSession(
            groups = groups,
            sessionStartMinutes = 8 * 60 + 27,
            skippedGroupIds = emptySet(),
            silenced = false,
        )

        assertTrue(plans.isEmpty())
    }

    @Test
    fun suppressesNotificationsWhenSessionStartsExactlyAtWindowOpen() {
        val groups = engine.groupWindows(
            engine.leaveWindows(
                commute = commute,
                origin = origin,
                settings = settings,
                departures = listOf(departure("dep-1", 8 * 60 + 30)),
            ),
        )

        val plans = engine.notificationPlansForSession(
            groups = groups,
            sessionStartMinutes = 8 * 60 + 26,
            skippedGroupIds = emptySet(),
            silenced = false,
        )

        assertTrue(plans.isEmpty())
    }

    @Test
    fun schedulesOpenAndFinalCallWhenWindowIsAhead() {
        val groups = engine.groupWindows(
            engine.leaveWindows(
                commute = commute,
                origin = origin,
                settings = settings,
                departures = listOf(departure("dep-1", 8 * 60 + 30)),
            ),
        )

        val plans = engine.notificationPlansForSession(
            groups = groups,
            sessionStartMinutes = 8 * 60 + 20,
            skippedGroupIds = emptySet(),
            silenced = false,
        )

        assertEquals(listOf(NotificationKind.WindowOpen, NotificationKind.FinalCall), plans.map { it.kind })
    }

    @Test
    fun validatesOverlappingSchedules() {
        val mondayMorning = AutoStartSchedule(
            days = listOf(Weekday.Monday),
            startMinutes = 7 * 60,
            endMinutes = 9 * 60,
        )
        val overlap = AutoStartSchedule(
            days = listOf(Weekday.Monday),
            startMinutes = 8 * 60,
            endMinutes = 10 * 60,
        )

        val result = engine.validateSchedules(
            listOf(
                commute.copy(id = "a", schedule = mondayMorning),
                commute.copy(id = "b", schedule = overlap),
            ),
        )

        assertIs<ScheduleValidationResult.Overlap>(result)
    }

    @Test
    fun notificationTitlesUseSharedWatchCopy() {
        val groups = engine.groupWindows(
            engine.leaveWindows(
                commute = commute,
                origin = origin,
                settings = settings,
                departures = listOf(departure("dep-1", 8 * 60 + 30)),
            ),
        )

        val plans = engine.notificationPlansForSession(
            groups = groups,
            sessionStartMinutes = 8 * 60 + 20,
            skippedGroupIds = emptySet(),
            silenced = false,
        )

        val open = plans.first { it.kind == NotificationKind.WindowOpen }
        val final = plans.first { it.kind == NotificationKind.FinalCall }
        assertEquals(WatchCopy.LEAVE_NOW, open.title)
        assertEquals(WatchCopy.FINAL_CALL, final.title)
    }

    @Test
    fun watchCopyHeadlineCoversEveryStatus() {
        assertEquals("Get ready", WatchCopy.headline(WatchStatus.GetReady))
        assertEquals("Leave now", WatchCopy.headline(WatchStatus.LeaveNow))
        assertEquals("Final call", WatchCopy.headline(WatchStatus.FinalCall))
        assertEquals("Next chance", WatchCopy.headline(WatchStatus.Missed))
        assertEquals("Watching", WatchCopy.headline(null))
    }

    @Test
    fun watchCopyTitleMatchesNotificationKind() {
        assertEquals("Leave now", WatchCopy.title(NotificationKind.WindowOpen))
        assertEquals("Final call", WatchCopy.title(NotificationKind.FinalCall))
        assertEquals("Watch stopped", WatchCopy.title(NotificationKind.WatchStopped))
    }

    @Test
    fun liveActivitySnapshotMatchesNotificationBody() {
        val groups = engine.groupWindows(
            engine.leaveWindows(
                commute = commute,
                origin = origin,
                settings = settings,
                departures = listOf(departure("dep-1", 8 * 60 + 30)),
            ),
        )
        val group = groups.single()

        val snapshot = engine.liveActivitySnapshot(
            commuteId = commute.id,
            group = group,
            status = WatchStatus.LeaveNow,
            walkingMinutes = 1,
        )

        assertEquals(WatchCopy.LEAVE_NOW, snapshot.title)
        assertEquals(engine.notificationBody(group), snapshot.body)
        assertEquals(group.id, snapshot.groupId)
        assertEquals(group.windowOpenMinutes, snapshot.windowOpenMinutes)
        assertEquals(group.finalCallMinutes, snapshot.finalCallMinutes)
        assertEquals("Test Stop", snapshot.stopName)
        assertEquals("tram 4", snapshot.lineLabel)
        assertEquals("Central", snapshot.directionHeadsign)
        assertEquals(1, snapshot.walkingMinutes)
    }

    @Test
    fun liveActivitySnapshotHeadlineFollowsStatus() {
        val groups = engine.groupWindows(
            engine.leaveWindows(
                commute = commute,
                origin = origin,
                settings = settings,
                departures = listOf(departure("dep-1", 8 * 60 + 30)),
            ),
        )
        val group = groups.single()

        val getReady = engine.liveActivitySnapshot(commute.id, group, WatchStatus.GetReady, walkingMinutes = 1)
        val finalCall = engine.liveActivitySnapshot(commute.id, group, WatchStatus.FinalCall, walkingMinutes = 1)

        assertEquals(WatchCopy.GET_READY, getReady.title)
        assertEquals(WatchCopy.FINAL_CALL, finalCall.title)
    }

    private fun departure(id: String, scheduledTimeMinutes: Int): Departure =
        Departure(
            id = id,
            stopId = "stop",
            lineId = "line",
            directionId = "direction",
            headsign = "Central",
            serviceDay = com.samex.kmt_hackathon.transit.ServiceDay.Weekday,
            scheduledTimeMinutes = scheduledTimeMinutes,
        )
}

private class FakeTransitRepository : TransitRepository {
    private val stop = TransitStop("stop", "Test Stop", 52.0, 4.0)
    private val line = TransitLine("line", "4", TransitMode.Tram, "#000000")
    private val direction = LineDirection("direction", "line", "Central", listOf("stop"))

    override fun stops(): List<TransitStop> = listOf(stop)

    override fun stopById(stopId: String): TransitStop? = stop.takeIf { it.id == stopId }

    override fun lineById(lineId: String): TransitLine? = line.takeIf { it.id == lineId }

    override fun directionById(directionId: String): LineDirection? = direction.takeIf { it.id == directionId }

    override fun directionsServingStop(stopId: String): List<LineDirection> = listOf(direction)

    override fun departuresFor(
        stopId: String,
        selections: List<CommuteLineSelection>,
        fromTimeMinutes: Int,
        limit: Int,
    ): List<Departure> = emptyList()
}
