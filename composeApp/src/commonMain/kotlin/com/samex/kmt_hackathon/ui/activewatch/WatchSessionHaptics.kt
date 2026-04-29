package com.samex.kmt_hackathon.ui.activewatch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.samex.kmt_hackathon.core.HapticEffect
import com.samex.kmt_hackathon.core.PlatformServices
import com.samex.kmt_hackathon.core.TransitAppModel
import com.samex.kmt_hackathon.core.WatchStatus
import com.samex.kmt_hackathon.ui.components.*
import com.samex.kmt_hackathon.ui.presentation.*

@Composable
internal fun WatchSessionHaptics(model: TransitAppModel) {
    val state = model.watchUiState()
    val group = state?.currentGroup
    val status = state?.currentStatus
    var lastGroupId by remember { mutableStateOf<String?>(null) }
    var lastStatus by remember { mutableStateOf<WatchStatus?>(null) }
    var lastProgressPulseSecond by remember { mutableStateOf<Int?>(null) }
    var lastGetReadyMinute by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(group?.id, status, model.nowSecondsOfDay, state?.silenced) {
        if (group == null || status == null || state.silenced) {
            lastGroupId = null
            lastStatus = null
            lastProgressPulseSecond = null
            lastGetReadyMinute = null
            return@LaunchedEffect
        }

        val progress = leaveWindowProgressFraction(
            windowOpenMinutes = group.windowOpenMinutes,
            finalCallMinutes = group.finalCallMinutes,
            nowSecondsOfDay = model.nowSecondsOfDay,
        )
        val getReadyMinute = getReadyHapticMinute(group.windowOpenMinutes, model.nowSecondsOfDay)
        val getReadyProgress = getReadyHapticProgress(group.windowOpenMinutes, model.nowSecondsOfDay)

        if (lastGroupId != group.id) {
            lastGroupId = group.id
            lastStatus = status
            lastProgressPulseSecond = model.nowSecondsOfDay
            lastGetReadyMinute = getReadyMinute
            when (status) {
                WatchStatus.LeaveNow -> PlatformServices.hapticFeedback().performProgress(progress)
                WatchStatus.FinalCall -> PlatformServices.hapticFeedback().perform(HapticEffect.Critical)
                WatchStatus.GetReady,
                WatchStatus.Missed -> Unit
            }
            return@LaunchedEffect
        }

        if (status != lastStatus) {
            when (status) {
                WatchStatus.LeaveNow -> PlatformServices.hapticFeedback().performProgress(progress)
                WatchStatus.FinalCall -> PlatformServices.hapticFeedback().perform(HapticEffect.Critical)
                WatchStatus.GetReady,
                WatchStatus.Missed -> Unit
            }
            lastStatus = status
            lastProgressPulseSecond = model.nowSecondsOfDay
            lastGetReadyMinute = getReadyMinute
            return@LaunchedEffect
        }

        if (status == WatchStatus.GetReady && getReadyMinute != null && getReadyMinute != lastGetReadyMinute) {
            PlatformServices.hapticFeedback().performProgress(getReadyProgress ?: 0f)
            lastGetReadyMinute = getReadyMinute
        }

        if (status == WatchStatus.LeaveNow &&
            model.nowSecondsOfDay != lastProgressPulseSecond
        ) {
            PlatformServices.hapticFeedback().performProgress(progress)
            lastProgressPulseSecond = model.nowSecondsOfDay
        }
    }
}

internal fun leaveWindowProgressFraction(windowOpenMinutes: Int, finalCallMinutes: Int, nowSecondsOfDay: Int): Float {
    val openSeconds = windowOpenMinutes * 60
    val finalCallSeconds = finalCallMinutes * 60
    if (finalCallSeconds <= openSeconds) return 1f
    return ((nowSecondsOfDay - openSeconds).toFloat() / (finalCallSeconds - openSeconds).toFloat())
        .coerceIn(0f, 1f)
}

internal fun getReadyHapticMinute(windowOpenMinutes: Int, nowSecondsOfDay: Int): Int? {
    val secondsUntilOpen = windowOpenMinutes * 60 - nowSecondsOfDay
    if (secondsUntilOpen !in 1..180) return null
    return ((secondsUntilOpen + 59) / 60).coerceIn(1, 3)
}

internal fun getReadyHapticProgress(windowOpenMinutes: Int, nowSecondsOfDay: Int): Float? {
    val secondsUntilOpen = windowOpenMinutes * 60 - nowSecondsOfDay
    if (secondsUntilOpen !in 1..180) return null
    return (1f - secondsUntilOpen / 180f).coerceIn(0f, 1f)
}
