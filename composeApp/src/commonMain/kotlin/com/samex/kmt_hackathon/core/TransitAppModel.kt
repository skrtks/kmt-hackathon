package com.samex.kmt_hackathon.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.samex.kmt_hackathon.transit.LineDirection
import com.samex.kmt_hackathon.transit.TransitStop
import kotlin.random.Random

sealed interface AppScreen {
    data object Home : AppScreen
    data class PlaceEditor(val onboarding: Boolean) : AppScreen
    data object CommuteSetup : AppScreen
    data object CommuteEdit : AppScreen
    data object Settings : AppScreen
    data object Places : AppScreen
}

data class PresetPlace(
    val name: String,
    val location: GeoPoint,
)

data class PlaceDraft(
    val name: String = "Home",
    val latitude: String = "52.3678",
    val longitude: String = "4.8933",
    val onboarding: Boolean = false,
)

data class CommuteDraft(
    val editingCommuteId: String? = null,
    val stopId: String = "",
    val originPlaceId: String = "",
    val selections: Set<CommuteLineSelection> = emptySet(),
    val minEarlyMinutes: String = "1",
    val maxEarlyMinutes: String = "3",
    val overrideArrivalBuffer: Boolean = false,
    val scheduleEnabled: Boolean = false,
    val scheduleDays: Set<Weekday> = setOf(Weekday.Monday, Weekday.Tuesday, Weekday.Wednesday, Weekday.Thursday, Weekday.Friday),
    val scheduleStart: String = "07:30",
    val scheduleEnd: String = "09:00",
)

private fun CommuteDraft.withSingleSelection(): CommuteDraft =
    if (selections.size <= 1) {
        this
    } else {
        copy(selections = selections.take(1).toSet())
    }

data class WatchUiState(
    val commute: SavedCommute,
    val origin: SavedPlace,
    val stopName: String,
    val walkingTimeMinutes: Int,
    val groups: List<LeaveWindowGroup>,
    val currentGroup: LeaveWindowGroup?,
    val currentStatus: WatchStatus?,
    val silenced: Boolean,
    val leavingDepartureTimeMinutes: Int?,
    val leavingGroupId: String?,
    val notificationStatus: NotificationPermissionStatus,
    val errorMessage: String?,
)

enum class DemoWatchScenario {
    GetReady,
    LeaveNow,
    FinalCall,
}

private const val SECONDS_PER_DAY = MINUTES_PER_DAY * 60
private const val DEBUG_SKIP_LEAD_SECONDS = 5

class TransitAppModel(
    private val transitRepository: TransitRepository,
    private val userDataRepository: UserDataRepository,
    private val notificationScheduler: NotificationScheduler,
    private val timeProvider: TimeProvider,
    private val liveActivityController: LiveActivityController = NoopLiveActivityController,
) {
    private val engine = WatchEngine(transitRepository)
    private var lastLiveSnapshot: LiveActivitySnapshot? = null
    private val autoStartSuppressedCommuteIds = mutableSetOf<String>()
    private var debugClockOffsetSeconds: Int = 0
    private var forceLiveActivityClockSync: Boolean = false

    var screen: AppScreen by mutableStateOf<AppScreen>(AppScreen.Home)
        private set

    var userData: UserData by mutableStateOf(UserData())
        private set

    var placeDraft: PlaceDraft by mutableStateOf(PlaceDraft())
        private set

    var commuteDraft: CommuteDraft by mutableStateOf(CommuteDraft())
        private set

    var activeGroups: List<LeaveWindowGroup> by mutableStateOf(emptyList())
        private set

    var pendingReplacementCommuteId: String? by mutableStateOf(null)
        private set

    private var settingsBackScreen: AppScreen = AppScreen.Home

    var errorMessage: String? by mutableStateOf(null)
        private set

    var nowMinutes: Int by mutableStateOf(0)
        private set

    var nowSecondsOfDay: Int by mutableStateOf(0)
        private set

    var notificationStatus: NotificationPermissionStatus by mutableStateOf(NotificationPermissionStatus.NotDetermined)
        private set

    val presetPlaces: List<PresetPlace> = listOf(
        PresetPlace("Home", GeoPoint(52.3678, 4.8933)),
        PresetPlace("Office", GeoPoint(52.3546, 4.8910)),
        PresetPlace("Gym", GeoPoint(52.3862, 4.8691)),
    )

    fun load() {
        userData = userDataRepository.load()
        notificationStatus = notificationScheduler.permissionStatus()
        updateClock()
        val restoredActiveSession = restoreActiveSession()
        expireLeavingSessionIfNeeded()
        screen = when {
            userData.places.isEmpty() -> AppScreen.PlaceEditor(onboarding = true)
            restoredActiveSession && userData.activeSession != null -> AppScreen.Home
            else -> AppScreen.Home
        }
        reconcileLiveActivityOnLoad(restored = userData.activeSession != null)
    }

    fun tick() {
        updateClock()
        notificationStatus = notificationScheduler.permissionStatus()
        expireLeavingSessionIfNeeded()
        stopAutoStartedSessionAfterScheduleEnd()
        maybeAutoStartForegroundSchedule()
        syncLiveActivity()
    }

    fun requestNotificationPermission() {
        notificationScheduler.requestPermission()
        notificationStatus = notificationScheduler.permissionStatus()
    }

    fun navigate(screen: AppScreen) {
        this.screen = screen
        errorMessage = null
    }

    fun openSettings() {
        if (screen != AppScreen.Settings) {
            settingsBackScreen = screen
        }
        screen = AppScreen.Settings
        errorMessage = null
    }

    fun closeSettings() {
        screen = settingsBackScreen
        errorMessage = null
    }

    fun beginPlaceEditor(onboarding: Boolean = false) {
        placeDraft = PlaceDraft(onboarding = onboarding)
        screen = AppScreen.PlaceEditor(onboarding = onboarding)
        errorMessage = null
    }

    fun updatePlaceDraft(draft: PlaceDraft) {
        placeDraft = draft
    }

    fun applyPresetPlace(preset: PresetPlace) {
        placeDraft = placeDraft.copy(
            name = preset.name,
            latitude = preset.location.latitude.toString(),
            longitude = preset.location.longitude.toString(),
        )
    }

    fun savePlace() {
        val latitude = placeDraft.latitude.toDoubleOrNull()
        val longitude = placeDraft.longitude.toDoubleOrNull()
        val name = placeDraft.name.trim()

        if (name.isEmpty() || latitude == null || longitude == null) {
            errorMessage = "Enter a place name and valid coordinates."
            return
        }

        val place = SavedPlace(
            id = newId("place"),
            name = name,
            location = GeoPoint(latitude, longitude),
        )
        updateUserData(userData.copy(places = userData.places + place))

        if (placeDraft.onboarding) {
            beginCommuteSetup(preselectedOriginPlaceId = place.id)
        } else {
            screen = AppScreen.Places
        }
        errorMessage = null
    }

    fun deletePlace(placeId: String) {
        if (userData.commutes.any { it.originPlaceId == placeId }) {
            errorMessage = "This place is used by a saved commute."
            return
        }
        updateUserData(userData.copy(places = userData.places.filterNot { it.id == placeId }))
    }

    fun beginCommuteSetup(preselectedOriginPlaceId: String? = null) {
        val firstStop = transitRepository.stops().firstOrNull()?.id.orEmpty()
        val origin = preselectedOriginPlaceId ?: userData.places.firstOrNull()?.id.orEmpty()
        commuteDraft = CommuteDraft(stopId = firstStop, originPlaceId = origin)
        screen = AppScreen.CommuteSetup
        errorMessage = null
    }

    fun beginCommuteEdit(commuteId: String) {
        val commute = userData.commutes.firstOrNull { it.id == commuteId } ?: return
        val defaultBuffer = userData.settings.defaultArrivalBuffer
        val schedule = commute.schedule
        commuteDraft = CommuteDraft(
            editingCommuteId = commute.id,
            stopId = commute.stopId,
            originPlaceId = commute.originPlaceId,
            selections = commute.selections.take(1).toSet(),
            minEarlyMinutes = (commute.arrivalBufferOverride ?: defaultBuffer).minEarlyMinutes.toString(),
            maxEarlyMinutes = (commute.arrivalBufferOverride ?: defaultBuffer).maxEarlyMinutes.toString(),
            overrideArrivalBuffer = commute.arrivalBufferOverride != null,
            scheduleEnabled = schedule != null,
            scheduleDays = schedule?.days?.toSet() ?: CommuteDraft().scheduleDays,
            scheduleStart = schedule?.startMinutes?.let(::formatMinutesOfDay) ?: CommuteDraft().scheduleStart,
            scheduleEnd = schedule?.endMinutes?.let(::formatMinutesOfDay) ?: CommuteDraft().scheduleEnd,
        )
        screen = AppScreen.CommuteEdit
        errorMessage = null
    }

    fun updateCommuteDraft(draft: CommuteDraft) {
        commuteDraft = draft.withSingleSelection()
    }

    fun selectStop(stopId: String) {
        commuteDraft = commuteDraft.copy(stopId = stopId, selections = emptySet())
    }

    fun selectLineDirection(direction: LineDirection) {
        val selection = CommuteLineSelection(direction.lineId, direction.id)
        commuteDraft = commuteDraft.copy(selections = setOf(selection))
    }

    fun saveCommute() {
        val originId = commuteDraft.originPlaceId
        if (originId.isBlank() || userData.places.none { it.id == originId }) {
            errorMessage = "Choose an origin place."
            return
        }
        if (commuteDraft.selections.size != 1) {
            errorMessage = "Select one line and direction."
            return
        }

        val arrivalBuffer = if (commuteDraft.overrideArrivalBuffer) {
            val min = commuteDraft.minEarlyMinutes.toIntOrNull()
            val max = commuteDraft.maxEarlyMinutes.toIntOrNull()
            if (min == null || max == null || min < 0 || max < min) {
                errorMessage = "Enter a valid arrival buffer."
                return
            }
            ArrivalBuffer(min, max)
        } else {
            null
        }

        val schedule = if (commuteDraft.scheduleEnabled) {
            val start = parseMinutesOfDay(commuteDraft.scheduleStart)
            val end = parseMinutesOfDay(commuteDraft.scheduleEnd)
            if (start == null || end == null || commuteDraft.scheduleDays.isEmpty() || start >= end) {
                errorMessage = "Enter a valid schedule."
                return
            }
            AutoStartSchedule(
                days = commuteDraft.scheduleDays.sortedBy { it.ordinal },
                startMinutes = start,
                endMinutes = end,
            )
        } else {
            null
        }

        val existingCommute = commuteDraft.editingCommuteId?.let { editingId ->
            userData.commutes.firstOrNull { it.id == editingId }
        }
        val autoStartEnabled = when {
            schedule == null -> false
            existingCommute == null -> true
            existingCommute.schedule == null -> true
            else -> existingCommute.autoStartEnabled
        }
        val commute = SavedCommute(
            id = existingCommute?.id ?: newId("commute"),
            originPlaceId = originId,
            stopId = commuteDraft.stopId,
            selections = commuteDraft.selections.toList(),
            arrivalBufferOverride = arrivalBuffer,
            schedule = schedule,
            autoStartEnabled = autoStartEnabled,
        )

        val nextCommutes = if (existingCommute == null) {
            userData.commutes + commute
        } else {
            userData.commutes.map { if (it.id == existingCommute.id) commute else it }
        }
        val activeEditedSession = userData.activeSession?.takeIf { it.commuteId == commute.id }
        val nextData = userData.copy(commutes = nextCommutes)
        val validation = engine.validateSchedules(nextData.commutes)
        if (validation is ScheduleValidationResult.Overlap) {
            errorMessage = "That schedule overlaps another saved commute."
            return
        }

        updateUserData(nextData)
        if (activeEditedSession != null) {
            refreshActiveSession(activeEditedSession)
        } else {
            errorMessage = null
        }
        requestNotificationPermission()
        screen = AppScreen.Home
    }

    fun toggleCommuteAutoStart(commuteId: String) {
        val nextCommutes = userData.commutes.map { commute ->
            if (commute.id == commuteId) commute.copy(autoStartEnabled = !commute.autoStartEnabled) else commute
        }
        val validation = engine.validateSchedules(nextCommutes)
        if (validation is ScheduleValidationResult.Overlap) {
            errorMessage = "Auto-start would overlap another commute."
            return
        }
        updateUserData(userData.copy(commutes = nextCommutes))
    }

    fun deleteCommute(commuteId: String) {
        if (userData.activeSession?.commuteId == commuteId) {
            stopActiveSession()
        }
        updateUserData(userData.copy(commutes = userData.commutes.filterNot { it.id == commuteId }))
    }

    fun updateDefaultArrivalBuffer(minEarlyMinutes: Int, maxEarlyMinutes: Int) {
        if (minEarlyMinutes < 0 || maxEarlyMinutes < minEarlyMinutes) return
        updateUserData(
            userData.copy(
                settings = userData.settings.copy(
                    defaultArrivalBuffer = ArrivalBuffer(minEarlyMinutes, maxEarlyMinutes),
                ),
            ),
        )
    }

    fun updateWalkingSpeed(metersPerMinute: Double) {
        if (metersPerMinute <= 0.0) return
        updateUserData(
            userData.copy(
                settings = userData.settings.copy(walkingSpeed = WalkingSpeed(metersPerMinute)),
            ),
        )
    }

    fun updateColorTheme(theme: AppColorTheme) {
        updateUserData(
            userData.copy(
                settings = userData.settings.copy(colorTheme = theme),
            ),
        )
    }

    fun setDebugModeEnabled(enabled: Boolean) {
        if (!enabled) {
            debugClockOffsetSeconds = 0
            updateClock()
            forceLiveActivityClockSync = true
        }
        updateUserData(
            userData.copy(
                settings = userData.settings.copy(debugModeEnabled = enabled),
            ),
        )
        syncLiveActivity(forceClockSync = !enabled)
    }

    fun debugSkipToNextWatchTransition() {
        if (!userData.settings.debugModeEnabled) return
        if (userData.activeSession == null) return
        val nextTransitionSeconds = nextDebugTransitionSeconds() ?: return
        val targetSeconds = (nextTransitionSeconds - DEBUG_SKIP_LEAD_SECONDS).mod(SECONDS_PER_DAY)
        val actualSeconds = timeProvider.nowSecondsOfDay().mod(SECONDS_PER_DAY)
        debugClockOffsetSeconds = secondsBetween(actualSeconds, targetSeconds)
        forceLiveActivityClockSync = true
        tick()
    }

    fun startWatch(commuteId: String, manual: Boolean = true) {
        val active = userData.activeSession
        if (active != null && active.commuteId == commuteId && manual) {
            screen = AppScreen.Home
            return
        }
        if (active != null && active.commuteId != commuteId && manual) {
            pendingReplacementCommuteId = commuteId
            return
        }
        if (active != null && active.commuteId != commuteId && !manual) return
        startWatchInternal(commuteId, startedAutomatically = !manual)
    }

    fun startDemoLeavingWindow() {
        startDemoWatch(DemoWatchScenario.LeaveNow)
    }

    fun startDemoWatch(scenario: DemoWatchScenario) {
        debugClockOffsetSeconds = 0
        forceLiveActivityClockSync = true
        updateClock()
        val commute = userData.activeSession
            ?.let { activeSession -> userData.commutes.firstOrNull { it.id == activeSession.commuteId } }
            ?: userData.commutes.firstOrNull()
        if (commute == null) {
            errorMessage = "Create a commute before starting the demo."
            return
        }

        val origin = userData.places.firstOrNull { it.id == commute.originPlaceId }
        val selection = commute.selections.firstOrNull()
        val line = selection?.let { transitRepository.lineById(it.lineId) }
        val direction = selection?.let { transitRepository.directionById(it.directionId) }
        if (origin == null || selection == null || line == null || direction == null) {
            errorMessage = "The demo needs a valid saved commute."
            return
        }

        val demoTiming = demoTimingFor(scenario)
        val demoWindow = LeaveWindow(
            departureId = "demo-${scenario.name.lowercase()}-$nowSecondsOfDay",
            stopId = commute.stopId,
            lineId = selection.lineId,
            lineShortName = line.shortName,
            directionId = selection.directionId,
            headsign = direction.headsign,
            departureTimeMinutes = demoTiming.departureTimeMinutes,
            windowOpenMinutes = demoTiming.windowOpenMinutes,
            finalCallMinutes = demoTiming.finalCallMinutes,
        )
        val demoGroup = LeaveWindowGroup(
            id = "demo-${scenario.name.lowercase()}-$nowSecondsOfDay",
            windows = listOf(demoWindow),
            windowOpenMinutes = demoTiming.windowOpenMinutes,
            finalCallMinutes = demoTiming.finalCallMinutes,
        )
        val session = PersistedWatchSession(
            commuteId = commute.id,
            startedAtMinutes = nowMinutes,
            silenced = false,
            skippedGroupIds = emptyList(),
            startedAutomatically = false,
        )

        activeGroups = listOf(demoGroup)
        updateUserData(userData.copy(activeSession = session))
        notificationScheduler.cancelAll()
        startLiveActivityForSession(commute, origin)
        screen = AppScreen.Home
        errorMessage = null
    }

    fun confirmReplacement() {
        val commuteId = pendingReplacementCommuteId ?: return
        stopActiveSession()
        pendingReplacementCommuteId = null
        startWatchInternal(commuteId, startedAutomatically = false)
    }

    fun cancelReplacement() {
        pendingReplacementCommuteId = null
    }

    fun stopActiveSession(reason: LiveActivityEndReason = LiveActivityEndReason.SessionEnded) {
        notificationScheduler.cancelAll()
        endLiveActivity(reason)
        activeGroups = emptyList()
        updateUserData(userData.copy(activeSession = null))
        screen = AppScreen.Home
    }

    fun skipCurrentGroup() {
        val session = userData.activeSession ?: return
        val group = watchUiState()?.currentGroup ?: return
        val skipped = (session.skippedGroupIds + group.id).distinct()
        notificationScheduler.cancel(listOf("${group.id}-open", "${group.id}-final"))
        updateUserData(
            userData.copy(
                activeSession = session.copy(skippedGroupIds = skipped),
            ),
        )
        syncLiveActivity(skippedFallbackReason = LiveActivityEndReason.Skipped)
    }

    fun markLeaving() {
        val session = userData.activeSession ?: return
        updateClock()
        val group = watchUiState()?.currentGroup ?: return
        val departureTimeMinutes = group.primaryWindow.departureTimeMinutes
        notificationScheduler.cancelAll()
        updateUserData(
            userData.copy(
                activeSession = session.copy(
                    silenced = true,
                    leavingAtMinutes = nowMinutes,
                    leavingDepartureTimeMinutes = departureTimeMinutes,
                    leavingGroupId = group.id,
                ),
            ),
        )
        endLiveActivity(LiveActivityEndReason.Leaving)
    }

    fun stops(): List<TransitStop> = transitRepository.stops()

    fun directionsForDraftStop(): List<LineDirection> =
        transitRepository.directionsServingStop(commuteDraft.stopId)

    fun lineShortName(lineId: String): String =
        transitRepository.lineById(lineId)?.shortName ?: "?"

    fun directionHeadsign(directionId: String): String =
        transitRepository.directionById(directionId)?.headsign ?: "Unknown direction"

    fun stopName(stopId: String): String =
        transitRepository.stopById(stopId)?.name ?: "Unknown stop"

    fun watchUiState(): WatchUiState? {
        val session = userData.activeSession ?: return null
        val commute = userData.commutes.firstOrNull { it.id == session.commuteId } ?: return null
        val origin = userData.places.firstOrNull { it.id == commute.originPlaceId } ?: return null
        val skipped = session.skippedGroupIds.toSet()
        val leavingGroup = session.leavingGroupId?.let { groupId ->
            activeGroups.firstOrNull { it.id == groupId }
        }
        val currentGroup = leavingGroup ?: engine.currentGroup(activeGroups, nowMinutes, skipped)
        val currentStatus = currentGroup?.let { group ->
            if (session.silenced && session.leavingDepartureTimeMinutes != null) {
                WatchStatus.LeaveNow
            } else {
                engine.statusFor(group, nowMinutes)
            }
        }
        return WatchUiState(
            commute = commute,
            origin = origin,
            stopName = stopName(commute.stopId),
            walkingTimeMinutes = engine.walkingTimeMinutes(origin.location, commute.stopId, userData.settings.walkingSpeed),
            groups = activeGroups.filterNot { it.id in skipped },
            currentGroup = currentGroup,
            currentStatus = currentStatus,
            silenced = session.silenced,
            leavingDepartureTimeMinutes = session.leavingDepartureTimeMinutes,
            leavingGroupId = session.leavingGroupId,
            notificationStatus = notificationStatus,
            errorMessage = errorMessage,
        )
    }

    private fun startWatchInternal(commuteId: String, startedAutomatically: Boolean) {
        val commute = userData.commutes.firstOrNull { it.id == commuteId } ?: return
        val origin = userData.places.firstOrNull { it.id == commute.originPlaceId } ?: return
        updateClock()
        autoStartSuppressedCommuteIds -= commuteId

        val groups = groupsFor(commute, origin)

        activeGroups = groups
        val session = PersistedWatchSession(
            commuteId = commute.id,
            startedAtMinutes = nowMinutes,
            silenced = false,
            skippedGroupIds = emptyList(),
            startedAutomatically = startedAutomatically,
        )
        updateUserData(userData.copy(activeSession = session))
        notificationScheduler.cancelAll()
        scheduleNotificationsForSession(session, groups)
        startLiveActivityForSession(commute, origin)
        screen = AppScreen.Home
        errorMessage = if (groups.isEmpty()) "No upcoming departures found." else null
    }

    private fun restoreActiveSession(): Boolean {
        val session = userData.activeSession ?: return false
        val commute = userData.commutes.firstOrNull { it.id == session.commuteId }
        val origin = commute?.let { userData.places.firstOrNull { place -> place.id == it.originPlaceId } }
        if (commute == null || origin == null) {
            updateUserData(userData.copy(activeSession = null))
            return false
        }
        activeGroups = groupsFor(commute, origin)
        return true
    }

    private fun refreshActiveSession(session: PersistedWatchSession) {
        updateClock()
        val commute = userData.commutes.firstOrNull { it.id == session.commuteId }
        val origin = commute?.let { userData.places.firstOrNull { place -> place.id == it.originPlaceId } }
        if (commute == null || origin == null) {
            updateUserData(userData.copy(activeSession = null))
            activeGroups = emptyList()
            notificationScheduler.cancelAll()
            errorMessage = null
            return
        }

        val groups = groupsFor(commute, origin)
        activeGroups = groups
        notificationScheduler.cancelAll()
        scheduleNotificationsForSession(session, groups)
        errorMessage = if (groups.isEmpty()) "No upcoming departures found." else null
    }

    private fun groupsFor(commute: SavedCommute, origin: SavedPlace): List<LeaveWindowGroup> {
        val departures = transitRepository.departuresFor(
            stopId = commute.stopId,
            selections = commute.selections,
            fromTimeMinutes = (nowMinutes - 90).coerceAtLeast(0),
            limit = 40,
        )
        return engine.groupWindows(engine.leaveWindows(commute, origin, userData.settings, departures))
    }

    private fun scheduleNotificationsForSession(session: PersistedWatchSession, groups: List<LeaveWindowGroup>) {
        engine.notificationPlansForSession(
            groups = groups,
            sessionStartMinutes = nowMinutes,
            skippedGroupIds = session.skippedGroupIds.toSet(),
            silenced = session.silenced,
        ).forEach(notificationScheduler::schedule)
    }

    private fun expireLeavingSessionIfNeeded() {
        val session = userData.activeSession ?: return
        val leavingAt = session.leavingAtMinutes ?: return
        val departureTime = session.leavingDepartureTimeMinutes
        if (departureTime == null) {
            if (minutesBetween(leavingAt, nowMinutes) >= 30) {
                stopActiveSession()
            }
            return
        }
        if (minutesBetween(leavingAt, nowMinutes) >= minutesBetween(leavingAt, departureTime)) {
            autoStartSuppressedCommuteIds += session.commuteId
            stopActiveSession()
        }
    }

    private fun maybeAutoStartForegroundSchedule() {
        if (userData.activeSession != null) return
        val day = timeProvider.currentWeekday()
        autoStartSuppressedCommuteIds.removeAll { commuteId ->
            val commute = userData.commutes.firstOrNull { it.id == commuteId }
            commute?.schedule?.isActive(day, nowMinutes) != true
        }
        val commute = userData.commutes.firstOrNull { commute ->
            commute.autoStartEnabled &&
                commute.id !in autoStartSuppressedCommuteIds &&
                commute.schedule?.isActive(day, nowMinutes) == true
        } ?: return
        startWatch(commute.id, manual = false)
    }

    private fun stopAutoStartedSessionAfterScheduleEnd() {
        val session = userData.activeSession ?: return
        if (!session.startedAutomatically) return
        val commute = userData.commutes.firstOrNull { it.id == session.commuteId } ?: return
        val schedule = commute.schedule ?: return
        if (schedule.isActive(timeProvider.currentWeekday(), nowMinutes)) return

        val skipped = session.skippedGroupIds.toSet()
        val current = engine.currentGroup(activeGroups, nowMinutes, skipped)
        if (current == null || nowMinutes > current.finalCallMinutes) {
            stopActiveSession(reason = LiveActivityEndReason.ScheduleEnded)
        }
    }

    private fun updateUserData(next: UserData) {
        userData = next
        userDataRepository.save(next)
    }

    private fun updateClock() {
        nowSecondsOfDay = (timeProvider.nowSecondsOfDay() + debugClockOffsetSeconds).mod(SECONDS_PER_DAY)
        nowMinutes = nowSecondsOfDay / 60
    }

    private fun newId(prefix: String): String =
        "$prefix-${timeProvider.nowMinutesOfDay()}-${Random.nextInt(1_000_000)}"

    private fun minutesBetween(start: Int, end: Int): Int =
        if (end >= start) end - start else (MINUTES_PER_DAY - start) + end

    private fun secondsBetween(start: Int, end: Int): Int =
        if (end >= start) end - start else (SECONDS_PER_DAY - start) + end

    private data class DemoTiming(
        val windowOpenMinutes: Int,
        val finalCallMinutes: Int,
        val departureTimeMinutes: Int,
    )

    private fun demoTimingFor(scenario: DemoWatchScenario): DemoTiming =
        when (scenario) {
            DemoWatchScenario.GetReady -> DemoTiming(
                windowOpenMinutes = nowMinutes + 1,
                finalCallMinutes = nowMinutes + 2,
                departureTimeMinutes = nowMinutes + 3,
            )
            DemoWatchScenario.LeaveNow -> DemoTiming(
                windowOpenMinutes = nowMinutes - 1,
                finalCallMinutes = nowMinutes + 1,
                departureTimeMinutes = nowMinutes + 2,
            )
            DemoWatchScenario.FinalCall -> DemoTiming(
                windowOpenMinutes = nowMinutes - 1,
                finalCallMinutes = nowMinutes,
                departureTimeMinutes = nowMinutes + 1,
            )
        }

    private fun nextDebugTransitionSeconds(): Int? {
        val state = watchUiState() ?: return null
        val groupTransitions = state.groups.flatMap { group ->
            listOf(group.windowOpenMinutes * 60, group.finalCallMinutes * 60)
        }
        val leavingTransition = state.leavingDepartureTimeMinutes
            ?.takeIf { state.silenced }
            ?.let { it * 60 }
        val transitions = (groupTransitions + listOfNotNull(leavingTransition))
            .map { it.mod(SECONDS_PER_DAY) }
            .distinct()

        return transitions
            .filter { secondsBetween(nowSecondsOfDay, it) > DEBUG_SKIP_LEAD_SECONDS }
            .minByOrNull { secondsBetween(nowSecondsOfDay, it) }
    }

    private fun currentLiveSnapshot(): LiveActivitySnapshot? {
        val session = userData.activeSession ?: return null
        if (session.silenced || session.leavingAtMinutes != null) return null
        val commute = userData.commutes.firstOrNull { it.id == session.commuteId } ?: return null
        val origin = userData.places.firstOrNull { it.id == commute.originPlaceId } ?: return null
        val skipped = session.skippedGroupIds.toSet()
        val group = engine.currentGroup(activeGroups, nowMinutes, skipped) ?: return null
        val status = engine.statusFor(group, nowMinutes)
        if (status == WatchStatus.Missed) return null
        val walking = engine.walkingTimeMinutes(origin.location, commute.stopId, userData.settings.walkingSpeed)
        return engine.liveActivitySnapshot(
            commuteId = commute.id,
            group = group,
            status = status,
            walkingMinutes = walking,
        )
    }

    private fun startLiveActivityForSession(commute: SavedCommute, origin: SavedPlace) {
        if (!liveActivityController.isSupported()) return
        val skipped = userData.activeSession?.skippedGroupIds?.toSet().orEmpty()
        val group = engine.currentGroup(activeGroups, nowMinutes, skipped) ?: return
        val status = engine.statusFor(group, nowMinutes)
        if (status == WatchStatus.Missed) return
        val walking = engine.walkingTimeMinutes(origin.location, commute.stopId, userData.settings.walkingSpeed)
        val snapshot = engine.liveActivitySnapshot(
            commuteId = commute.id,
            group = group,
            status = status,
            walkingMinutes = walking,
        ).withCurrentClock()
        liveActivityController.start(snapshot)
        lastLiveSnapshot = snapshot
    }

    private fun syncLiveActivity(
        skippedFallbackReason: LiveActivityEndReason? = null,
        forceClockSync: Boolean = false,
    ) {
        if (!liveActivityController.isSupported()) return
        val snapshot = currentLiveSnapshot()
        val shouldForceClockSync = forceClockSync || forceLiveActivityClockSync
        when {
            snapshot == null && lastLiveSnapshot != null -> {
                liveActivityController.end(
                    snapshot = lastLiveSnapshot,
                    reason = skippedFallbackReason ?: LiveActivityEndReason.SessionEnded,
                )
                lastLiveSnapshot = null
            }
            snapshot != null && lastLiveSnapshot == null -> {
                val syncedSnapshot = snapshot.withCurrentClock()
                liveActivityController.start(syncedSnapshot)
                lastLiveSnapshot = syncedSnapshot
            }
            snapshot != null && (shouldForceClockSync || !snapshot.sameLiveContentAs(lastLiveSnapshot)) -> {
                val syncedSnapshot = snapshot.withCurrentClock()
                liveActivityController.update(syncedSnapshot)
                lastLiveSnapshot = syncedSnapshot
            }
        }
        forceLiveActivityClockSync = false
    }

    private fun endLiveActivity(reason: LiveActivityEndReason) {
        if (!liveActivityController.isSupported()) return
        if (lastLiveSnapshot == null && !liveActivityController.isActivityRunning()) return
        liveActivityController.end(snapshot = lastLiveSnapshot, reason = reason)
        lastLiveSnapshot = null
    }

    private fun reconcileLiveActivityOnLoad(restored: Boolean) {
        if (!liveActivityController.isSupported()) return
        if (restored) {
            val snapshot = currentLiveSnapshot()
            if (snapshot != null) {
                val syncedSnapshot = snapshot.withCurrentClock()
                if (liveActivityController.isActivityRunning()) {
                    liveActivityController.update(syncedSnapshot)
                } else {
                    liveActivityController.start(syncedSnapshot)
                }
                lastLiveSnapshot = syncedSnapshot
            } else if (liveActivityController.isActivityRunning()) {
                liveActivityController.end(snapshot = null, reason = LiveActivityEndReason.SessionEnded)
            }
        } else if (liveActivityController.isActivityRunning()) {
            liveActivityController.end(snapshot = null, reason = LiveActivityEndReason.SessionEnded)
        }
    }

    private fun LiveActivitySnapshot.withCurrentClock(): LiveActivitySnapshot =
        copy(syncedNowSecondsOfDay = nowSecondsOfDay)

    private fun LiveActivitySnapshot.sameLiveContentAs(other: LiveActivitySnapshot?): Boolean =
        copy(syncedNowSecondsOfDay = null) == other?.copy(syncedNowSecondsOfDay = null)
}
