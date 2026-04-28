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
    data object Watch : AppScreen
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

data class WatchUiState(
    val commute: SavedCommute,
    val origin: SavedPlace,
    val stopName: String,
    val walkingTimeMinutes: Int,
    val groups: List<LeaveWindowGroup>,
    val currentGroup: LeaveWindowGroup?,
    val currentStatus: WatchStatus?,
    val silenced: Boolean,
    val notificationStatus: NotificationPermissionStatus,
    val errorMessage: String?,
)

class TransitAppModel(
    private val transitRepository: TransitRepository,
    private val userDataRepository: UserDataRepository,
    private val notificationScheduler: NotificationScheduler,
    private val timeProvider: TimeProvider,
) {
    private val engine = WatchEngine(transitRepository)

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
        screen = when {
            userData.places.isEmpty() -> AppScreen.PlaceEditor(onboarding = true)
            restoredActiveSession -> AppScreen.Watch
            else -> AppScreen.Home
        }
    }

    fun tick() {
        updateClock()
        notificationStatus = notificationScheduler.permissionStatus()
        expireLeavingSessionIfNeeded()
        stopAutoStartedSessionAfterScheduleEnd()
        maybeAutoStartForegroundSchedule()
    }

    fun requestNotificationPermission() {
        notificationScheduler.requestPermission()
        notificationStatus = notificationScheduler.permissionStatus()
    }

    fun navigate(screen: AppScreen) {
        this.screen = screen
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

    fun updateCommuteDraft(draft: CommuteDraft) {
        commuteDraft = draft
    }

    fun selectStop(stopId: String) {
        commuteDraft = commuteDraft.copy(stopId = stopId, selections = emptySet())
    }

    fun toggleSelection(direction: LineDirection) {
        val selection = CommuteLineSelection(direction.lineId, direction.id)
        commuteDraft = if (selection in commuteDraft.selections) {
            commuteDraft.copy(selections = commuteDraft.selections - selection)
        } else {
            commuteDraft.copy(selections = commuteDraft.selections + selection)
        }
    }

    fun saveCommute() {
        val originId = commuteDraft.originPlaceId
        if (originId.isBlank() || userData.places.none { it.id == originId }) {
            errorMessage = "Choose an origin place."
            return
        }
        if (commuteDraft.selections.isEmpty()) {
            errorMessage = "Select at least one line and direction."
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

        val commute = SavedCommute(
            id = newId("commute"),
            originPlaceId = originId,
            stopId = commuteDraft.stopId,
            selections = commuteDraft.selections.sortedWith(compareBy<CommuteLineSelection> { it.lineId }.thenBy { it.directionId }),
            arrivalBufferOverride = arrivalBuffer,
            schedule = schedule,
            autoStartEnabled = schedule != null,
        )

        val nextData = userData.copy(commutes = userData.commutes + commute)
        val validation = engine.validateSchedules(nextData.commutes)
        if (validation is ScheduleValidationResult.Overlap) {
            errorMessage = "That schedule overlaps another saved commute."
            return
        }

        updateUserData(nextData)
        requestNotificationPermission()
        screen = AppScreen.Home
        errorMessage = null
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

    fun startWatch(commuteId: String, manual: Boolean = true) {
        val active = userData.activeSession
        if (active != null && active.commuteId == commuteId && manual) {
            screen = AppScreen.Watch
            return
        }
        if (active != null && active.commuteId != commuteId && manual) {
            pendingReplacementCommuteId = commuteId
            return
        }
        if (active != null && active.commuteId != commuteId && !manual) return
        startWatchInternal(commuteId, startedAutomatically = !manual)
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

    fun stopActiveSession() {
        notificationScheduler.cancelAll()
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
    }

    fun markLeaving() {
        val session = userData.activeSession ?: return
        notificationScheduler.cancelAll()
        updateUserData(
            userData.copy(
                activeSession = session.copy(
                    silenced = true,
                    leavingAtMinutes = timeProvider.nowMinutesOfDay(),
                ),
            ),
        )
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
        val currentGroup = engine.currentGroup(activeGroups, nowMinutes, skipped)
        return WatchUiState(
            commute = commute,
            origin = origin,
            stopName = stopName(commute.stopId),
            walkingTimeMinutes = engine.walkingTimeMinutes(origin.location, commute.stopId, userData.settings.walkingSpeed),
            groups = activeGroups.filterNot { it.id in skipped },
            currentGroup = currentGroup,
            currentStatus = currentGroup?.let { engine.statusFor(it, nowMinutes) },
            silenced = session.silenced,
            notificationStatus = notificationStatus,
            errorMessage = errorMessage,
        )
    }

    private fun startWatchInternal(commuteId: String, startedAutomatically: Boolean) {
        val commute = userData.commutes.firstOrNull { it.id == commuteId } ?: return
        val origin = userData.places.firstOrNull { it.id == commute.originPlaceId } ?: return
        updateClock()

        val departures = transitRepository.departuresFor(
            stopId = commute.stopId,
            selections = commute.selections,
            fromTimeMinutes = (nowMinutes - 90).coerceAtLeast(0),
            limit = 40,
        )
        val windows = engine.leaveWindows(commute, origin, userData.settings, departures)
        val groups = engine.groupWindows(windows)

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
        engine.notificationPlansForSession(
            groups = groups,
            sessionStartMinutes = nowMinutes,
            skippedGroupIds = emptySet(),
            silenced = false,
        ).forEach(notificationScheduler::schedule)
        screen = AppScreen.Watch
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
        val departures = transitRepository.departuresFor(
            stopId = commute.stopId,
            selections = commute.selections,
            fromTimeMinutes = (nowMinutes - 90).coerceAtLeast(0),
            limit = 40,
        )
        activeGroups = engine.groupWindows(engine.leaveWindows(commute, origin, userData.settings, departures))
        return true
    }

    private fun expireLeavingSessionIfNeeded() {
        val session = userData.activeSession ?: return
        val leavingAt = session.leavingAtMinutes ?: return
        if (minutesBetween(leavingAt, nowMinutes) >= 30) {
            stopActiveSession()
        }
    }

    private fun maybeAutoStartForegroundSchedule() {
        if (userData.activeSession != null) return
        val day = timeProvider.currentWeekday()
        val commute = userData.commutes.firstOrNull { commute ->
            commute.autoStartEnabled &&
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
            stopActiveSession()
        }
    }

    private fun updateUserData(next: UserData) {
        userData = next
        userDataRepository.save(next)
    }

    private fun updateClock() {
        nowSecondsOfDay = timeProvider.nowSecondsOfDay()
        nowMinutes = nowSecondsOfDay / 60
    }

    private fun newId(prefix: String): String =
        "$prefix-${timeProvider.nowMinutesOfDay()}-${Random.nextInt(1_000_000)}"

    private fun minutesBetween(start: Int, end: Int): Int =
        if (end >= start) end - start else (MINUTES_PER_DAY - start) + end
}
