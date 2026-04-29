package com.samex.kmt_hackathon.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.samex.kmt_hackathon.transit.LineDirection
import com.samex.kmt_hackathon.transit.TransitStop
import kotlin.random.Random

private const val DEBUG_SKIP_LEAD_SECONDS = 5

class TransitAppModel(
    private val transitRepository: TransitRepository,
    private val userDataRepository: UserDataRepository,
    private val notificationScheduler: NotificationScheduler,
    private val timeProvider: TimeProvider,
    private val liveActivityController: LiveActivityController = NoopLiveActivityController,
) {
    private val engine = WatchEngine(transitRepository)
    private val liveActivitySync = LiveActivitySyncCoordinator(
        controller = liveActivityController,
        currentSnapshot = { currentLiveSnapshot() },
        clockSecondsOfDay = { nowSecondsOfDay },
    )
    private val autoStartSuppressedCommuteIds = mutableSetOf<String>()
    private var debugClockOffsetSeconds: Int = 0

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
        liveActivitySync.reconcileOnLoad(restored = userData.activeSession != null)
    }

    fun tick() {
        updateClock()
        notificationStatus = notificationScheduler.permissionStatus()
        expireLeavingSessionIfNeeded()
        stopAutoStartedSessionAfterScheduleEnd()
        maybeAutoStartForegroundSchedule()
        liveActivitySync.sync()
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
        commuteDraft = newCommuteDraft(firstStopId = firstStop, originPlaceId = origin)
        screen = AppScreen.CommuteSetup
        errorMessage = null
    }

    fun beginCommuteEdit(commuteId: String) {
        val commute = userData.commutes.firstOrNull { it.id == commuteId } ?: return
        commuteDraft = commute.toDraft(userData.settings.defaultArrivalBuffer)
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
        val buildResult = commuteDraft.toSavedCommute(
            userData = userData,
            newCommuteId = { newId("commute") },
        )
        val commute: SavedCommute
        val existingCommute: SavedCommute?
        when (buildResult) {
            is CommuteDraftBuildResult.Error -> {
                errorMessage = buildResult.message
                return
            }
            is CommuteDraftBuildResult.Success -> {
                commute = buildResult.commute
                existingCommute = buildResult.existingCommute
            }
        }

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
            liveActivitySync.requestClockSync()
        }
        updateUserData(
            userData.copy(
                settings = userData.settings.copy(debugModeEnabled = enabled),
            ),
        )
        liveActivitySync.sync(forceClockSync = !enabled)
    }

    fun debugSkipToNextWatchTransition() {
        if (!userData.settings.debugModeEnabled) return
        if (userData.activeSession == null) return
        val nextTransitionSeconds = nextDebugTransitionSeconds() ?: return
        val targetSeconds = (nextTransitionSeconds - DEBUG_SKIP_LEAD_SECONDS).mod(SECONDS_PER_DAY)
        val actualSeconds = timeProvider.nowSecondsOfDay().mod(SECONDS_PER_DAY)
        debugClockOffsetSeconds = secondsBetween(actualSeconds, targetSeconds)
        liveActivitySync.requestClockSync()
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
        liveActivitySync.requestClockSync()
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

        val demoTiming = demoTimingFor(scenario, nowMinutes)
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
        liveActivitySync.end(reason)
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
        liveActivitySync.sync(skippedFallbackReason = LiveActivityEndReason.Skipped)
    }

    fun markLeaving() {
        val session = userData.activeSession ?: return
        updateClock()
        val state = watchUiState() ?: return
        val group = state.currentGroup ?: return
        val departureTimeMinutes = group.primaryWindow.departureTimeMinutes
        val leavingSnapshot = engine.liveActivitySnapshot(
            commuteId = session.commuteId,
            group = group,
            status = WatchStatus.LeaveNow,
            walkingMinutes = state.walkingTimeMinutes,
        ).copy(isLeaving = true)
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
        liveActivitySync.end(LiveActivityEndReason.Leaving, snapshotOverride = leavingSnapshot)
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
        liveActivitySync.sync(forceClockSync = true)
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
        val commute = userData.commutes.firstOrNull { it.id == session.commuteId } ?: return null
        val origin = userData.places.firstOrNull { it.id == commute.originPlaceId } ?: return null
        val skipped = session.skippedGroupIds.toSet()
        if (session.silenced && session.leavingDepartureTimeMinutes != null) {
            val leavingGroup = session.leavingGroupId?.let { groupId ->
                activeGroups.firstOrNull { it.id == groupId }
            } ?: return null
            val walking = engine.walkingTimeMinutes(origin.location, commute.stopId, userData.settings.walkingSpeed)
            return engine.liveActivitySnapshot(
                commuteId = commute.id,
                group = leavingGroup,
                status = WatchStatus.LeaveNow,
                walkingMinutes = walking,
            ).copy(isLeaving = true)
        }
        if (session.silenced || session.leavingAtMinutes != null) return null
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
        )
        liveActivitySync.start(snapshot)
    }
}
