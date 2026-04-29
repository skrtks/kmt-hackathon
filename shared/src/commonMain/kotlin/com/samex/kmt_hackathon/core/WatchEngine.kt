package com.samex.kmt_hackathon.core

import com.samex.kmt_hackathon.transit.Departure
import com.samex.kmt_hackathon.transit.TransitMode
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class WatchEngine(
    private val transitRepository: TransitRepository,
) {
    fun walkingTimeMinutes(origin: GeoPoint, stopId: String, walkingSpeed: WalkingSpeed): Int {
        val stop = transitRepository.stopById(stopId) ?: return 1
        val distanceMeters = haversineMeters(
            from = origin,
            to = GeoPoint(stop.latitude, stop.longitude),
        )
        return ceil(distanceMeters / walkingSpeed.metersPerMinute).toInt().coerceAtLeast(1)
    }

    fun leaveWindows(
        commute: SavedCommute,
        origin: SavedPlace,
        settings: UserSettings,
        departures: List<Departure>,
    ): List<LeaveWindow> {
        val buffer = commute.arrivalBufferOverride ?: settings.defaultArrivalBuffer
        val walkingTime = walkingTimeMinutes(origin.location, commute.stopId, settings.walkingSpeed)

        return departures.mapNotNull { departure ->
            val line = transitRepository.lineById(departure.lineId) ?: return@mapNotNull null
            LeaveWindow(
                departureId = departure.id,
                stopId = departure.stopId,
                lineId = departure.lineId,
                lineShortName = line.shortName,
                directionId = departure.directionId,
                headsign = departure.headsign,
                departureTimeMinutes = departure.scheduledTimeMinutes,
                windowOpenMinutes = departure.scheduledTimeMinutes - walkingTime - buffer.maxEarlyMinutes,
                finalCallMinutes = departure.scheduledTimeMinutes - walkingTime - buffer.minEarlyMinutes,
            )
        }.sortedWith(compareBy<LeaveWindow> { it.windowOpenMinutes }.thenBy { it.departureTimeMinutes })
    }

    fun groupWindows(windows: List<LeaveWindow>, toleranceMinutes: Int = 1): List<LeaveWindowGroup> {
        val groups = mutableListOf<LeaveWindowGroup>()

        windows.forEach { window ->
            val matchingIndex = groups.indexOfFirst { group ->
                kotlin.math.abs(group.windowOpenMinutes - window.windowOpenMinutes) <= toleranceMinutes &&
                    kotlin.math.abs(group.finalCallMinutes - window.finalCallMinutes) <= toleranceMinutes
            }

            if (matchingIndex == -1) {
                groups += groupOf(listOf(window))
            } else {
                val mergedWindows = (groups[matchingIndex].windows + window)
                    .sortedWith(compareBy<LeaveWindow> { it.departureTimeMinutes }.thenBy { it.lineShortName })
                groups[matchingIndex] = groupOf(mergedWindows)
            }
        }

        return groups.sortedWith(compareBy<LeaveWindowGroup> { it.windowOpenMinutes }.thenBy { it.primaryWindow.departureTimeMinutes })
    }

    fun currentGroup(groups: List<LeaveWindowGroup>, nowMinutes: Int, skippedGroupIds: Set<String>): LeaveWindowGroup? =
        groups
            .filterNot { it.id in skippedGroupIds }
            .firstOrNull { it.finalCallMinutes >= nowMinutes }
            ?: groups.filterNot { it.id in skippedGroupIds }.firstOrNull { it.windowOpenMinutes >= nowMinutes }

    fun statusFor(group: LeaveWindowGroup, nowMinutes: Int): WatchStatus =
        when {
            nowMinutes < group.windowOpenMinutes -> WatchStatus.GetReady
            nowMinutes < group.finalCallMinutes -> WatchStatus.LeaveNow
            nowMinutes == group.finalCallMinutes -> WatchStatus.FinalCall
            else -> WatchStatus.Missed
        }

    fun notificationPlansForSession(
        groups: List<LeaveWindowGroup>,
        sessionStartMinutes: Int,
        skippedGroupIds: Set<String>,
        silenced: Boolean,
    ): List<NotificationPlan> {
        if (silenced) return emptyList()

        return groups
            .filterNot { it.id in skippedGroupIds }
            .filter { it.windowOpenMinutes > sessionStartMinutes }
            .flatMap { group ->
                listOfNotNull(
                    NotificationPlan(
                        id = "${group.id}-open",
                        groupId = group.id,
                        kind = NotificationKind.WindowOpen,
                        fireAtMinutes = group.windowOpenMinutes,
                        title = WatchCopy.title(NotificationKind.WindowOpen),
                        body = notificationBody(group),
                    ),
                    NotificationPlan(
                        id = "${group.id}-final",
                        groupId = group.id,
                        kind = NotificationKind.FinalCall,
                        fireAtMinutes = group.finalCallMinutes,
                        title = WatchCopy.title(NotificationKind.FinalCall),
                        body = notificationBody(group),
                    ).takeIf { group.finalCallMinutes > sessionStartMinutes },
                )
            }
    }

    fun validateSchedules(commutes: List<SavedCommute>): ScheduleValidationResult {
        val scheduled = commutes
            .filter { it.autoStartEnabled && it.schedule != null }
            .map { it to requireNotNull(it.schedule) }

        scheduled.forEachIndexed { index, first ->
            scheduled.drop(index + 1).forEach { second ->
                if (first.second.overlaps(second.second)) {
                    return ScheduleValidationResult.Overlap(first.first.id, second.first.id)
                }
            }
        }

        return ScheduleValidationResult.Valid
    }

    fun notificationBody(group: LeaveWindowGroup): String {
        val options = group.windows.joinToString(" or ") { window ->
            "${modeLabel(window.lineId)} ${window.lineShortName} at ${formatMinutesOfDay(window.departureTimeMinutes)}"
        }
        return "$options from ${formatMinutesOfDay(group.windowOpenMinutes)}-${formatMinutesOfDay(group.finalCallMinutes)}"
    }

    fun liveActivitySnapshot(
        commuteId: String,
        group: LeaveWindowGroup,
        status: WatchStatus,
        walkingMinutes: Int,
    ): LiveActivitySnapshot {
        val primary = group.primaryWindow
        val stopName = transitRepository.stopById(primary.stopId)?.name ?: ""
        return LiveActivitySnapshot(
            commuteId = commuteId,
            groupId = group.id,
            status = status,
            title = WatchCopy.headline(status),
            body = notificationBody(group),
            stopName = stopName,
            lineLabel = "${modeLabel(primary.lineId)} ${primary.lineShortName}",
            directionHeadsign = primary.headsign,
            departureTimeMinutes = primary.departureTimeMinutes,
            windowOpenMinutes = group.windowOpenMinutes,
            finalCallMinutes = group.finalCallMinutes,
            walkingMinutes = walkingMinutes,
        )
    }

    private fun modeLabel(lineId: String): String {
        val mode = transitRepository.lineById(lineId)?.mode
        return when (mode) {
            TransitMode.Bus -> "bus"
            TransitMode.Tram -> "tram"
            TransitMode.Metro -> "metro"
            null -> "line"
        }
    }

    private fun groupOf(windows: List<LeaveWindow>): LeaveWindowGroup {
        val sorted = windows.sortedWith(compareBy<LeaveWindow> { it.departureTimeMinutes }.thenBy { it.lineShortName })
        val primary = sorted.first()
        val open = sorted.minOf { it.windowOpenMinutes }
        val final = sorted.minOf { it.finalCallMinutes }
        return LeaveWindowGroup(
            id = "group-${open}-${final}-${primary.departureId}",
            windows = sorted,
            windowOpenMinutes = open,
            finalCallMinutes = final,
        )
    }

    private fun haversineMeters(from: GeoPoint, to: GeoPoint): Double {
        val earthRadiusMeters = 6_371_000.0
        val deltaLat = (to.latitude - from.latitude).toRadians()
        val deltaLon = (to.longitude - from.longitude).toRadians()
        val fromLat = from.latitude.toRadians()
        val toLat = to.latitude.toRadians()

        val a = sin(deltaLat / 2).pow(2) +
            cos(fromLat) * cos(toLat) * sin(deltaLon / 2).pow(2)
        val c = 2 * asin(sqrt(a))
        return earthRadiusMeters * c
    }

    private fun Double.toRadians(): Double = this * PI / 180.0
}

sealed interface ScheduleValidationResult {
    data object Valid : ScheduleValidationResult
    data class Overlap(val firstCommuteId: String, val secondCommuteId: String) : ScheduleValidationResult
}

object WatchCopy {
    const val GET_READY = "Get ready"
    const val LEAVE_NOW = "Leave now"
    const val FINAL_CALL = "Final call"
    const val MISSED = "Next chance"
    const val WATCHING = "Watching"
    const val WATCH_STOPPED_TITLE = "Watch stopped"
    const val WATCH_STOPPED_BODY = "Watch ended unexpectedly. Open the app to resume."

    fun headline(status: WatchStatus?): String = when (status) {
        WatchStatus.GetReady -> GET_READY
        WatchStatus.LeaveNow -> LEAVE_NOW
        WatchStatus.FinalCall -> FINAL_CALL
        WatchStatus.Missed -> MISSED
        null -> WATCHING
    }

    fun title(kind: NotificationKind): String = when (kind) {
        NotificationKind.WindowOpen -> LEAVE_NOW
        NotificationKind.FinalCall -> FINAL_CALL
        NotificationKind.WatchStopped -> WATCH_STOPPED_TITLE
    }
}
