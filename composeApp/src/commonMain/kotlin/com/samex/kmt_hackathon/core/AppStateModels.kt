package com.samex.kmt_hackathon.core

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
    val scheduleDays: Set<Weekday> = setOf(
        Weekday.Monday,
        Weekday.Tuesday,
        Weekday.Wednesday,
        Weekday.Thursday,
        Weekday.Friday,
    ),
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
