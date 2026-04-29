package com.samex.kmt_hackathon.core

import com.samex.kmt_hackathon.transit.Departure
import com.samex.kmt_hackathon.transit.LineDirection
import com.samex.kmt_hackathon.transit.ServiceDay
import com.samex.kmt_hackathon.transit.TransitLine
import com.samex.kmt_hackathon.transit.TransitMode
import com.samex.kmt_hackathon.transit.TransitStop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class TransitAppModelTest {
    @Test
    fun loadRestoresManualActiveSessionToHome() {
        val store = FakeModelKeyValueStore()
        val repository = UserDataRepository(store)
        repository.save(testUserData(startedAutomatically = false))
        val model = model(repository, now = 8 * 60)

        model.load()

        assertIs<AppScreen.Home>(model.screen)
        assertNotNull(model.watchUiState()?.currentGroup)
    }

    @Test
    fun scheduleEndDoesNotStopManualSession() {
        val store = FakeModelKeyValueStore()
        val repository = UserDataRepository(store)
        repository.save(testUserData(startedAutomatically = false))
        val timeProvider = FakeModelTimeProvider(now = 10 * 60, weekday = Weekday.Monday)
        val model = model(repository, timeProvider)

        model.load()
        model.tick()

        assertNotNull(model.userData.activeSession)
        assertIs<AppScreen.Home>(model.screen)
    }

    @Test
    fun startingSameCommuteKeepsExistingSessionOnHome() {
        val store = FakeModelKeyValueStore()
        val repository = UserDataRepository(store)
        repository.save(testUserData(startedAutomatically = false))
        val model = model(repository, now = 9 * 60)

        model.load()
        model.navigate(AppScreen.Home)
        model.startWatch("commute")

        assertIs<AppScreen.Home>(model.screen)
        assertEquals(8 * 60, model.userData.activeSession?.startedAtMinutes)
    }

    @Test
    fun startWatchSchedulesUpcomingNotifications() {
        val store = FakeModelKeyValueStore()
        val repository = UserDataRepository(store)
        val scheduler = FakeModelNotificationScheduler()
        repository.save(testUserData(activeSession = null))
        val model = model(repository, now = 8 * 60 + 20, notificationScheduler = scheduler)

        model.load()
        model.startWatch("commute")

        assertEquals(listOf(NotificationKind.WindowOpen, NotificationKind.FinalCall), scheduler.scheduled.map { it.kind })
        assertEquals(listOf(8 * 60 + 26, 8 * 60 + 28), scheduler.scheduled.map { it.fireAtMinutes })
    }

    @Test
    fun leavingSessionDoesNotRestartLiveActivityOnTick() {
        val store = FakeModelKeyValueStore()
        val repository = UserDataRepository(store)
        val liveActivityController = RecordingLiveActivityController()
        repository.save(testUserData(activeSession = null))
        val model = model(
            repository = repository,
            now = 8 * 60 + 20,
            liveActivityController = liveActivityController,
        )

        model.load()
        model.updateColorTheme(AppColorTheme.Berry)

        assertEquals(AppColorTheme.Berry, model.userData.settings.colorTheme)
        assertEquals(AppColorTheme.Berry, repository.load().settings.colorTheme)
    }

    @Test
    fun settingsBackReturnsToPreviousScreen() {
        val store = FakeModelKeyValueStore()
        val repository = UserDataRepository(store)
        repository.save(testUserData(activeSession = null))
        val model = model(repository, now = 8 * 60)

        model.load()
        model.beginCommuteEdit("commute")
        model.openSettings()

        assertIs<AppScreen.Settings>(model.screen)

        model.closeSettings()

        assertIs<AppScreen.CommuteEdit>(model.screen)
    }

    @Test
    fun selectingLineDirectionReplacesPreviousSelection() {
        val store = FakeModelKeyValueStore()
        val repository = UserDataRepository(store)
        repository.save(testUserData(activeSession = null))
        val model = model(repository, now = 8 * 60)
        val firstDirection = LineDirection("direction-1", "line-1", "Central", listOf("stop"))
        val secondDirection = LineDirection("direction-2", "line-2", "South", listOf("stop"))

        model.load()
        model.beginCommuteSetup()
        model.selectLineDirection(firstDirection)
        model.selectLineDirection(secondDirection)

        assertEquals(setOf(CommuteLineSelection("line-2", "direction-2")), model.commuteDraft.selections)
    }

    @Test
    fun editingCommuteUpdatesSavedCommuteInPlace() {
        val store = FakeModelKeyValueStore()
        val repository = UserDataRepository(store)
        repository.save(testUserData(activeSession = null))
        val model = model(repository, now = 8 * 60)

        model.load()
        model.beginCommuteEdit("commute")

        assertIs<AppScreen.CommuteEdit>(model.screen)
        assertEquals("commute", model.commuteDraft.editingCommuteId)
        assertEquals(setOf(CommuteLineSelection("line", "direction")), model.commuteDraft.selections)

        model.updateCommuteDraft(
            model.commuteDraft.copy(
                overrideArrivalBuffer = true,
                minEarlyMinutes = "2",
                maxEarlyMinutes = "5",
                scheduleEnabled = false,
            ),
        )
        model.saveCommute()

        val commute = model.userData.commutes.single()
        assertIs<AppScreen.Home>(model.screen)
        assertEquals("commute", commute.id)
        assertEquals(ArrivalBuffer(minEarlyMinutes = 2, maxEarlyMinutes = 5), commute.arrivalBufferOverride)
        assertEquals(null, commute.schedule)
        assertEquals(false, commute.autoStartEnabled)
        assertEquals(1, repository.load().commutes.size)
    }

    @Test
    fun editingActiveCommuteRefreshesActiveWindows() {
        val store = FakeModelKeyValueStore()
        val repository = UserDataRepository(store)
        val scheduler = FakeModelNotificationScheduler()
        repository.save(testUserData(startedAutomatically = false))
        val model = model(repository, now = 8 * 60 + 20, notificationScheduler = scheduler)

        model.load()
        model.beginCommuteEdit("commute")
        model.updateCommuteDraft(
            model.commuteDraft.copy(
                overrideArrivalBuffer = true,
                minEarlyMinutes = "2",
                maxEarlyMinutes = "5",
            ),
        )
        model.saveCommute()

        assertEquals(8 * 60 + 24, model.activeGroups.first().windowOpenMinutes)
        assertEquals(8 * 60 + 27, model.activeGroups.first().finalCallMinutes)
        assertEquals(listOf(NotificationKind.WindowOpen, NotificationKind.FinalCall), scheduler.scheduled.map { it.kind })
        assertEquals(listOf(8 * 60 + 24, 8 * 60 + 27), scheduler.scheduled.map { it.fireAtMinutes })
    }

    @Test
    fun scheduleEndStopsAutoStartedSessionAfterActiveWindowFinishes() {
        val store = FakeModelKeyValueStore()
        val repository = UserDataRepository(store)
        repository.save(testUserData(startedAutomatically = true))
        val timeProvider = FakeModelTimeProvider(now = 10 * 60, weekday = Weekday.Monday)
        val model = model(repository, timeProvider)

        model.load()
        model.tick()

        assertEquals(null, model.userData.activeSession)
        assertIs<AppScreen.Home>(model.screen)
    }

    @Test
    fun leavingSessionEndsAtSelectedDeparture() {
        val store = FakeModelKeyValueStore()
        val repository = UserDataRepository(store)
        val timeProvider = MutableModelTimeProvider(now = 8 * 60 + 26, weekday = Weekday.Monday)
        repository.save(testUserData(startedAutomatically = false))
        val model = model(repository, timeProvider)

        model.load()
        model.markLeaving()

        assertEquals(true, model.userData.activeSession?.silenced)
        assertEquals(8 * 60 + 30, model.userData.activeSession?.leavingDepartureTimeMinutes)
        assertEquals(model.activeGroups.first().id, model.userData.activeSession?.leavingGroupId)

        timeProvider.now = 8 * 60 + 29
        model.tick()

        assertNotNull(model.userData.activeSession)

        timeProvider.now = 8 * 60 + 30
        model.tick()

        assertEquals(null, model.userData.activeSession)
    }

    private fun model(
        repository: UserDataRepository,
        now: Int,
        notificationScheduler: NotificationScheduler = FakeModelNotificationScheduler(),
        liveActivityController: LiveActivityController = NoopLiveActivityController,
    ): TransitAppModel =
        model(repository, FakeModelTimeProvider(now = now, weekday = Weekday.Monday), notificationScheduler, liveActivityController)

    private fun model(
        repository: UserDataRepository,
        timeProvider: TimeProvider,
        notificationScheduler: NotificationScheduler = FakeModelNotificationScheduler(),
        liveActivityController: LiveActivityController = NoopLiveActivityController,
    ): TransitAppModel =
        TransitAppModel(
            transitRepository = ModelFakeTransitRepository,
            userDataRepository = repository,
            notificationScheduler = notificationScheduler,
            timeProvider = timeProvider,
            liveActivityController = liveActivityController,
        )

    private fun testUserData(startedAutomatically: Boolean): UserData =
        testUserData(
            activeSession = PersistedWatchSession(
                commuteId = "commute",
                startedAtMinutes = 8 * 60,
                silenced = false,
                skippedGroupIds = emptyList(),
                startedAutomatically = startedAutomatically,
            ),
        )

    private fun testUserData(activeSession: PersistedWatchSession?): UserData =
        UserData(
            places = listOf(SavedPlace("place", "Home", GeoPoint(52.0, 4.0))),
            commutes = listOf(
                SavedCommute(
                    id = "commute",
                    originPlaceId = "place",
                    stopId = "stop",
                    selections = listOf(CommuteLineSelection("line", "direction")),
                    schedule = AutoStartSchedule(
                        days = listOf(Weekday.Monday),
                        startMinutes = 7 * 60,
                        endMinutes = 9 * 60,
                    ),
                ),
            ),
            activeSession = activeSession,
        )
}

private object ModelFakeTransitRepository : TransitRepository {
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
    ): List<Departure> =
        listOf(
            Departure(
                id = "dep-1",
                stopId = "stop",
                lineId = "line",
                directionId = "direction",
                headsign = "Central",
                serviceDay = ServiceDay.Weekday,
                scheduledTimeMinutes = 8 * 60 + 30,
            ),
        )
}

private class FakeModelKeyValueStore : KeyValueStore {
    private val values = mutableMapOf<String, String>()

    override fun getString(key: String): String? = values[key]

    override fun putString(key: String, value: String) {
        values[key] = value
    }
}

private class FakeModelNotificationScheduler : NotificationScheduler {
    val scheduled = mutableListOf<NotificationPlan>()

    override fun permissionStatus(): NotificationPermissionStatus = NotificationPermissionStatus.Granted

    override fun requestPermission() = Unit

    override fun schedule(plan: NotificationPlan) {
        scheduled += plan
    }

    override fun cancel(notificationIds: List<String>) = Unit

    override fun cancelAll() = Unit
}

private class RecordingLiveActivityController(
    private val supported: Boolean = true,
) : LiveActivityController {
    val starts = mutableListOf<LiveActivitySnapshot>()
    val updates = mutableListOf<LiveActivitySnapshot>()
    val ends = mutableListOf<Pair<LiveActivitySnapshot?, LiveActivityEndReason>>()
    private var running = false

    override fun isSupported(): Boolean = supported

    override fun isActivityRunning(): Boolean = running

    override fun start(snapshot: LiveActivitySnapshot) {
        starts += snapshot
        running = true
    }

    override fun update(snapshot: LiveActivitySnapshot) {
        updates += snapshot
    }

    override fun end(snapshot: LiveActivitySnapshot?, reason: LiveActivityEndReason) {
        ends += snapshot to reason
        running = false
    }
}

private class FakeModelTimeProvider(
    private val now: Int,
    private val weekday: Weekday,
) : TimeProvider {
    override fun nowMinutesOfDay(): Int = now

    override fun nowSecondsOfDay(): Int = now * 60

    override fun currentWeekday(): Weekday = weekday
}

private class MutableModelTimeProvider(
    var now: Int,
    private val weekday: Weekday,
) : TimeProvider {
    override fun nowMinutesOfDay(): Int = now

    override fun nowSecondsOfDay(): Int = now * 60

    override fun currentWeekday(): Weekday = weekday
}
