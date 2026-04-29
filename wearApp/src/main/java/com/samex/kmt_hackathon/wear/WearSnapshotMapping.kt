package com.samex.kmt_hackathon.wear

import com.google.android.gms.wearable.DataMap
import com.samex.kmt_hackathon.core.LiveActivitySnapshot
import com.samex.kmt_hackathon.core.WatchStatus

internal const val WEAR_LIVE_ACTIVITY_PATH = "/transit-live-activity"
private const val KEY_WEAR_ACTIVE = "active"
private const val KEY_WEAR_COMMUTE_ID = "commute_id"
private const val KEY_WEAR_GROUP_ID = "group_id"
private const val KEY_WEAR_STATUS = "status"
private const val KEY_WEAR_TITLE = "title"
private const val KEY_WEAR_BODY = "body"
private const val KEY_WEAR_STOP_NAME = "stop_name"
private const val KEY_WEAR_LINE_LABEL = "line_label"
private const val KEY_WEAR_DIRECTION_HEADSIGN = "direction_headsign"
private const val KEY_WEAR_DEPARTURE_TIME_MINUTES = "departure_time_minutes"
private const val KEY_WEAR_WINDOW_OPEN_MINUTES = "window_open_minutes"
private const val KEY_WEAR_FINAL_CALL_MINUTES = "final_call_minutes"
private const val KEY_WEAR_WALKING_MINUTES = "walking_minutes"
private const val KEY_WEAR_IS_LEAVING = "is_leaving"
private const val KEY_WEAR_SYNCED_NOW_SECONDS = "synced_now_seconds"

internal fun DataMap.isWearLiveActivityActive(): Boolean =
    getBoolean(KEY_WEAR_ACTIVE, false)

internal fun DataMap.toLiveActivitySnapshot(): LiveActivitySnapshot? {
    val status = getString(KEY_WEAR_STATUS)?.let { value ->
        runCatching { WatchStatus.valueOf(value) }.getOrNull()
    } ?: return null
    return LiveActivitySnapshot(
        commuteId = getString(KEY_WEAR_COMMUTE_ID).orEmpty(),
        groupId = getString(KEY_WEAR_GROUP_ID).orEmpty(),
        status = status,
        title = getString(KEY_WEAR_TITLE).orEmpty(),
        body = getString(KEY_WEAR_BODY).orEmpty(),
        stopName = getString(KEY_WEAR_STOP_NAME).orEmpty(),
        lineLabel = getString(KEY_WEAR_LINE_LABEL).orEmpty(),
        directionHeadsign = getString(KEY_WEAR_DIRECTION_HEADSIGN).orEmpty(),
        departureTimeMinutes = getInt(KEY_WEAR_DEPARTURE_TIME_MINUTES),
        windowOpenMinutes = getInt(KEY_WEAR_WINDOW_OPEN_MINUTES),
        finalCallMinutes = getInt(KEY_WEAR_FINAL_CALL_MINUTES),
        walkingMinutes = getInt(KEY_WEAR_WALKING_MINUTES),
        isLeaving = getBoolean(KEY_WEAR_IS_LEAVING, false),
        syncedNowSecondsOfDay = getOptionalInt(KEY_WEAR_SYNCED_NOW_SECONDS),
    )
}

internal fun DataMap.getOptionalInt(key: String): Int? =
    if (containsKey(key)) getInt(key) else null
