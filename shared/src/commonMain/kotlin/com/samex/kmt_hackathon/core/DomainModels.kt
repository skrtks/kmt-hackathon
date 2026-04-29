package com.samex.kmt_hackathon.core

import kotlinx.serialization.Serializable

@Serializable
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class SavedPlace(
    val id: String,
    val name: String,
    val location: GeoPoint,
)

@Serializable
data class ArrivalBuffer(
    val minEarlyMinutes: Int,
    val maxEarlyMinutes: Int,
) {
    init {
        require(minEarlyMinutes >= 0) { "Minimum early arrival must be zero or greater" }
        require(maxEarlyMinutes >= minEarlyMinutes) {
            "Maximum early arrival must be greater than or equal to minimum early arrival"
        }
    }
}

@Serializable
data class WalkingSpeed(
    val metersPerMinute: Double,
) {
    init {
        require(metersPerMinute > 0.0) { "Walking speed must be positive" }
    }
}

@Serializable
enum class AppColorTheme {
    Sunrise,
    Lagoon,
    Grove,
    Berry,
}

@Serializable
enum class Weekday {
    Monday,
    Tuesday,
    Wednesday,
    Thursday,
    Friday,
    Saturday,
    Sunday,
}

@Serializable
data class AutoStartSchedule(
    val days: List<Weekday>,
    val startMinutes: Int,
    val endMinutes: Int,
) {
    init {
        require(days.isNotEmpty()) { "Schedule must include at least one day" }
        require(startMinutes in 0 until MINUTES_PER_DAY) { "Schedule start time is out of range" }
        require(endMinutes in 1..MINUTES_PER_DAY) { "Schedule end time is out of range" }
        require(startMinutes < endMinutes) { "Schedule start must be before schedule end" }
    }

    fun isActive(day: Weekday, minutesOfDay: Int): Boolean =
        day in days && minutesOfDay in startMinutes until endMinutes

    fun overlaps(other: AutoStartSchedule): Boolean {
        if (days.none { it in other.days }) return false
        return startMinutes < other.endMinutes && other.startMinutes < endMinutes
    }
}

@Serializable
data class CommuteLineSelection(
    val lineId: String,
    val directionId: String,
)

@Serializable
data class SavedCommute(
    val id: String,
    val originPlaceId: String,
    val stopId: String,
    val selections: List<CommuteLineSelection>,
    val arrivalBufferOverride: ArrivalBuffer? = null,
    val schedule: AutoStartSchedule? = null,
    val autoStartEnabled: Boolean = true,
)

@Serializable
data class UserSettings(
    val defaultArrivalBuffer: ArrivalBuffer = ArrivalBuffer(minEarlyMinutes = 1, maxEarlyMinutes = 3),
    val walkingSpeed: WalkingSpeed = WalkingSpeed(metersPerMinute = 80.0),
    val colorTheme: AppColorTheme = AppColorTheme.Sunrise,
    val debugModeEnabled: Boolean = false,
)

@Serializable
data class PersistedWatchSession(
    val commuteId: String,
    val startedAtMinutes: Int,
    val silenced: Boolean,
    val skippedGroupIds: List<String>,
    val leavingAtMinutes: Int? = null,
    val leavingDepartureTimeMinutes: Int? = null,
    val leavingGroupId: String? = null,
    val startedAutomatically: Boolean = false,
)

@Serializable
data class UserData(
    val places: List<SavedPlace> = emptyList(),
    val commutes: List<SavedCommute> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val activeSession: PersistedWatchSession? = null,
)

data class LeaveWindow(
    val departureId: String,
    val stopId: String,
    val lineId: String,
    val lineShortName: String,
    val directionId: String,
    val headsign: String,
    val departureTimeMinutes: Int,
    val windowOpenMinutes: Int,
    val finalCallMinutes: Int,
)

data class LeaveWindowGroup(
    val id: String,
    val windows: List<LeaveWindow>,
    val windowOpenMinutes: Int,
    val finalCallMinutes: Int,
) {
    val primaryWindow: LeaveWindow = windows.minBy { it.departureTimeMinutes }
}

enum class WatchStatus {
    GetReady,
    LeaveNow,
    FinalCall,
    Missed,
}

data class NotificationPlan(
    val id: String,
    val groupId: String,
    val kind: NotificationKind,
    val fireAtMinutes: Int,
    val title: String,
    val body: String,
)

enum class NotificationKind {
    WindowOpen,
    FinalCall,
    WatchStopped,
}

data class LiveActivitySnapshot(
    val commuteId: String,
    val groupId: String,
    val status: WatchStatus,
    val title: String,
    val body: String,
    val stopName: String,
    val lineLabel: String,
    val directionHeadsign: String,
    val departureTimeMinutes: Int,
    val windowOpenMinutes: Int,
    val finalCallMinutes: Int,
    val walkingMinutes: Int,
    val isLeaving: Boolean = false,
    val syncedNowSecondsOfDay: Int? = null,
)

enum class LiveActivityEndReason {
    SessionEnded,
    Skipped,
    Leaving,
    WatchStopped,
    ScheduleEnded,
}

const val MINUTES_PER_DAY = 24 * 60

fun formatMinutesOfDay(minutesOfDay: Int): String {
    val normalized = minutesOfDay.mod(MINUTES_PER_DAY)
    val hours = normalized / 60
    val minutes = normalized % 60
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}

fun parseMinutesOfDay(value: String): Int? {
    val parts = value.trim().split(":")
    if (parts.size != 2) return null
    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null
    if (hours !in 0..23 || minutes !in 0..59) return null
    return hours * 60 + minutes
}
