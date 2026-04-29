package com.samex.kmt_hackathon.ui.presentation

import com.samex.kmt_hackathon.core.SavedCommute
import com.samex.kmt_hackathon.core.TransitAppModel

internal fun commuteOriginName(model: TransitAppModel, commute: SavedCommute): String =
    model.userData.places.firstOrNull { it.id == commute.originPlaceId }?.name ?: "Unknown origin"
internal fun commuteRouteLabels(model: TransitAppModel, commute: SavedCommute): List<String> =
    commute.selections.map { selection ->
        "${model.lineShortName(selection.lineId)} to ${model.directionHeadsign(selection.directionId)}"
    }
