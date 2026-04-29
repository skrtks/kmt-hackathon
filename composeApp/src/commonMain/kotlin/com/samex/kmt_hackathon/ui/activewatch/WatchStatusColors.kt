package com.samex.kmt_hackathon.ui.activewatch

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.samex.kmt_hackathon.leaveStatusColors
import com.samex.kmt_hackathon.core.WatchStatus
import com.samex.kmt_hackathon.ui.components.*
import com.samex.kmt_hackathon.ui.presentation.*

@Composable
internal fun appBackgroundColor(status: WatchStatus?): Color =
    with(leaveStatusColors()) {
        when (status) {
            WatchStatus.LeaveNow -> signalBackground
            WatchStatus.FinalCall -> finalCallBackground
            WatchStatus.Missed -> missedBackground
            else -> MaterialTheme.colorScheme.background
        }
    }

@Composable
internal fun statusContainerColor(status: WatchStatus?): Color =
    with(leaveStatusColors()) {
        when (status) {
            WatchStatus.GetReady -> routeContainer
            WatchStatus.LeaveNow -> signalContainer
            WatchStatus.FinalCall -> finalCallContainer
            WatchStatus.Missed -> missedContainer
            null -> MaterialTheme.colorScheme.surfaceVariant
        }
    }

@Composable
internal fun statusContentColor(status: WatchStatus?): Color =
    with(leaveStatusColors()) {
        when (status) {
            WatchStatus.GetReady -> onRouteContainer
            WatchStatus.LeaveNow -> onSignalContainer
            WatchStatus.FinalCall -> onFinalCallContainer
            WatchStatus.Missed -> onMissedContainer
            null -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

@Composable
internal fun statusBorderColor(status: WatchStatus?): Color =
    with(leaveStatusColors()) {
        when (status) {
            WatchStatus.GetReady -> route
            WatchStatus.LeaveNow -> signal
            WatchStatus.FinalCall -> finalCall
            WatchStatus.Missed -> MaterialTheme.colorScheme.error
            null -> MaterialTheme.colorScheme.outline
        }
    }
