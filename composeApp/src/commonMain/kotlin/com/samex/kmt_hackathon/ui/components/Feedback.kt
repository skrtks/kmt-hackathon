package com.samex.kmt_hackathon.ui.components

import com.samex.kmt_hackathon.core.HapticEffect
import com.samex.kmt_hackathon.core.PlatformServices
import com.samex.kmt_hackathon.core.TransitAppModel

internal fun hapticClick(
    effect: HapticEffect = HapticEffect.Selection,
    onClick: () -> Unit,
): () -> Unit = {
    PlatformServices.hapticFeedback().perform(effect)
    onClick()
}

internal fun hapticResultClick(
    model: TransitAppModel,
    successEffect: HapticEffect = HapticEffect.Confirmation,
    onClick: () -> Unit,
): () -> Unit = {
    onClick()
    val effect = if (model.errorMessage == null) successEffect else HapticEffect.Error
    PlatformServices.hapticFeedback().perform(effect)
}
