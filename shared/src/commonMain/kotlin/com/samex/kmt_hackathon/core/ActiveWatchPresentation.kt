package com.samex.kmt_hackathon.core

data class ActiveWatchPresentation(
    val headline: String,
    val compactText: String,
    val expandedLines: List<String>,
    val timerTargetMinutes: Int,
    val tone: WatchSurfaceTone,
    val showsFinalCallCue: Boolean,
)

enum class WatchSurfaceTone {
    Route,
    Signal,
    FinalCall,
    Error,
}

fun LiveActivitySnapshot.activeWatchPresentation(nowSecondsOfDay: Int): ActiveWatchPresentation {
    val routeText = "$lineLabel to $directionHeadsign"
    val departureText = "Departure ${formatMinutesOfDay(departureTimeMinutes)}"
    val headline = when {
        isLeaving -> "Departure in ${formatCountdownToMinutesOfDay(departureTimeMinutes, nowSecondsOfDay)}"
        status == WatchStatus.GetReady -> "Leave at ${formatMinutesOfDay(windowOpenMinutes)}"
        status == WatchStatus.LeaveNow -> WatchCopy.LEAVE_NOW
        status == WatchStatus.FinalCall -> WatchCopy.FINAL_CALL
        status == WatchStatus.Missed -> "Next chance"
        else -> WatchCopy.WATCHING
    }
    val timerTargetMinutes = when {
        isLeaving -> departureTimeMinutes
        status == WatchStatus.GetReady -> windowOpenMinutes
        else -> finalCallMinutes
    }
    val compactText = "$routeText - ${departureText.lowercase()}"
    val expandedLines = buildList {
        add(routeText)
        if (stopName.isNotBlank()) add(stopName)
        when {
            isLeaving -> add(departureText)
            status == WatchStatus.GetReady -> {
                add("Leave at ${formatMinutesOfDay(windowOpenMinutes)}")
                add(departureText)
            }
            status == WatchStatus.LeaveNow -> {
                add("Window closes ${formatMinutesOfDay(finalCallMinutes)}")
                add(departureText)
            }
            status == WatchStatus.FinalCall -> {
                add("Last safe leave time")
                add(departureText)
            }
            status == WatchStatus.Missed -> add(departureText)
        }
    }
    return ActiveWatchPresentation(
        headline = headline,
        compactText = compactText,
        expandedLines = expandedLines,
        timerTargetMinutes = timerTargetMinutes,
        tone = when {
            isLeaving -> WatchSurfaceTone.Signal
            status == WatchStatus.GetReady -> WatchSurfaceTone.Route
            status == WatchStatus.LeaveNow -> WatchSurfaceTone.Signal
            status == WatchStatus.FinalCall -> WatchSurfaceTone.FinalCall
            status == WatchStatus.Missed -> WatchSurfaceTone.Error
            else -> WatchSurfaceTone.Route
        },
        showsFinalCallCue = !isLeaving && status == WatchStatus.FinalCall,
    )
}

fun formatCountdownToMinutesOfDay(targetMinutesOfDay: Int, nowSecondsOfDay: Int): String {
    val targetSeconds = targetMinutesOfDay.mod(MINUTES_PER_DAY) * SECONDS_PER_MINUTE
    val remainingSeconds = secondsBetween(
        startSeconds = nowSecondsOfDay.mod(SECONDS_PER_DAY),
        endSeconds = targetSeconds,
    )
    val hours = remainingSeconds / SECONDS_PER_HOUR
    val minutes = (remainingSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = remainingSeconds % SECONDS_PER_MINUTE
    return if (hours > 0) {
        "${hours}h ${minutes.toString().padStart(2, '0')}m"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}

private fun secondsBetween(startSeconds: Int, endSeconds: Int): Int {
    val raw = (endSeconds - startSeconds) % SECONDS_PER_DAY
    return if (raw < 0) raw + SECONDS_PER_DAY else raw
}

private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 60 * SECONDS_PER_MINUTE
private const val SECONDS_PER_DAY = MINUTES_PER_DAY * SECONDS_PER_MINUTE
