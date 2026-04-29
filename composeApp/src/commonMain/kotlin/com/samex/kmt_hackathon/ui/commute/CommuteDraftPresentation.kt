package com.samex.kmt_hackathon.ui.commute

import androidx.compose.foundation.layout.only
import com.samex.kmt_hackathon.core.CommuteDraft
import com.samex.kmt_hackathon.core.TransitAppModel
import com.samex.kmt_hackathon.transit.LineDirection
import com.samex.kmt_hackathon.ui.components.*

internal fun originName(model: TransitAppModel, draft: CommuteDraft): String =
    model.userData.places.firstOrNull { it.id == draft.originPlaceId }?.name ?: "Not set"
internal fun stopName(model: TransitAppModel, draft: CommuteDraft): String =
    draft.stopId.takeIf { it.isNotBlank() }?.let(model::stopName) ?: "Not set"
internal fun linesSummary(model: TransitAppModel, draft: CommuteDraft): String =
    if (draft.selections.isEmpty()) {
        "Not set"
    } else {
        draft.selections.joinToString { selection ->
            "${model.lineShortName(selection.lineId)} to ${model.directionHeadsign(selection.directionId)}"
        }
    }

internal fun lineDirectionLabel(model: TransitAppModel, direction: LineDirection): String =
    "${model.lineShortName(direction.lineId)} to ${direction.headsign}"
internal fun arrivalBufferSummary(model: TransitAppModel, draft: CommuteDraft): String {
    val buffer = if (draft.overrideArrivalBuffer) {
        "${draft.minEarlyMinutes}-${draft.maxEarlyMinutes}"
    } else {
        val default = model.userData.settings.defaultArrivalBuffer
        "${default.minEarlyMinutes}-${default.maxEarlyMinutes}"
    }
    return "$buffer min early"
}

internal fun scheduleSummary(draft: CommuteDraft): String =
    if (draft.scheduleEnabled) {
        "${
            draft.scheduleDays.sortedBy { it.ordinal }.joinToString { it.name.take(3) }
        } ${draft.scheduleStart}-${draft.scheduleEnd}"
    } else {
        "Manual start only"
    }
