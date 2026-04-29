package com.samex.kmt_hackathon.core

internal fun newCommuteDraft(firstStopId: String, originPlaceId: String): CommuteDraft =
    CommuteDraft(stopId = firstStopId, originPlaceId = originPlaceId)

internal fun SavedCommute.toDraft(defaultBuffer: ArrivalBuffer): CommuteDraft {
    val buffer = arrivalBufferOverride ?: defaultBuffer
    return CommuteDraft(
        editingCommuteId = id,
        stopId = stopId,
        originPlaceId = originPlaceId,
        selections = selections.take(1).toSet(),
        minEarlyMinutes = buffer.minEarlyMinutes.toString(),
        maxEarlyMinutes = buffer.maxEarlyMinutes.toString(),
        overrideArrivalBuffer = arrivalBufferOverride != null,
        scheduleEnabled = schedule != null,
        scheduleDays = schedule?.days?.toSet() ?: CommuteDraft().scheduleDays,
        scheduleStart = schedule?.startMinutes?.let(::formatMinutesOfDay) ?: CommuteDraft().scheduleStart,
        scheduleEnd = schedule?.endMinutes?.let(::formatMinutesOfDay) ?: CommuteDraft().scheduleEnd,
    )
}

internal fun CommuteDraft.withSingleSelection(): CommuteDraft =
    if (selections.size <= 1) {
        this
    } else {
        copy(selections = selections.take(1).toSet())
    }

internal fun CommuteDraft.isTimingValid(): Boolean {
    val bufferValid = if (overrideArrivalBuffer) {
        val min = minEarlyMinutes.toIntOrNull()
        val max = maxEarlyMinutes.toIntOrNull()
        min != null && max != null && min >= 0 && max >= min
    } else {
        true
    }
    val scheduleValid = if (scheduleEnabled) {
        val start = parseMinutesOfDay(scheduleStart)
        val end = parseMinutesOfDay(scheduleEnd)
        start != null && end != null && start < end && scheduleDays.isNotEmpty()
    } else {
        true
    }
    return bufferValid && scheduleValid
}

internal fun CommuteDraft.isSaveable(): Boolean =
    originPlaceId.isNotBlank() &&
        stopId.isNotBlank() &&
        selections.size == 1 &&
        isTimingValid()

internal sealed interface CommuteDraftBuildResult {
    data class Success(
        val commute: SavedCommute,
        val existingCommute: SavedCommute?,
    ) : CommuteDraftBuildResult

    data class Error(val message: String) : CommuteDraftBuildResult
}

internal fun CommuteDraft.toSavedCommute(
    userData: UserData,
    newCommuteId: () -> String,
): CommuteDraftBuildResult {
    if (originPlaceId.isBlank() || userData.places.none { it.id == originPlaceId }) {
        return CommuteDraftBuildResult.Error("Choose an origin place.")
    }
    if (selections.size != 1) {
        return CommuteDraftBuildResult.Error("Select one line and direction.")
    }

    val arrivalBuffer = if (overrideArrivalBuffer) {
        val min = minEarlyMinutes.toIntOrNull()
        val max = maxEarlyMinutes.toIntOrNull()
        if (min == null || max == null || min < 0 || max < min) {
            return CommuteDraftBuildResult.Error("Enter a valid arrival buffer.")
        }
        ArrivalBuffer(min, max)
    } else {
        null
    }

    val schedule = if (scheduleEnabled) {
        val start = parseMinutesOfDay(scheduleStart)
        val end = parseMinutesOfDay(scheduleEnd)
        if (start == null || end == null || scheduleDays.isEmpty() || start >= end) {
            return CommuteDraftBuildResult.Error("Enter a valid schedule.")
        }
        AutoStartSchedule(
            days = scheduleDays.sortedBy { it.ordinal },
            startMinutes = start,
            endMinutes = end,
        )
    } else {
        null
    }

    val existingCommute = editingCommuteId?.let { editingId ->
        userData.commutes.firstOrNull { it.id == editingId }
    }
    val autoStartEnabled = when {
        schedule == null -> false
        existingCommute == null -> true
        existingCommute.schedule == null -> true
        else -> existingCommute.autoStartEnabled
    }
    return CommuteDraftBuildResult.Success(
        commute = SavedCommute(
            id = existingCommute?.id ?: newCommuteId(),
            originPlaceId = originPlaceId,
            stopId = stopId,
            selections = selections.toList(),
            arrivalBufferOverride = arrivalBuffer,
            schedule = schedule,
            autoStartEnabled = autoStartEnabled,
        ),
        existingCommute = existingCommute,
    )
}
