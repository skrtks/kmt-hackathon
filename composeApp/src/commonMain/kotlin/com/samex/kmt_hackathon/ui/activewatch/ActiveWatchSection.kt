package com.samex.kmt_hackathon.ui.activewatch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.core.HapticEffect
import com.samex.kmt_hackathon.core.LeaveWindowGroup
import com.samex.kmt_hackathon.core.TransitAppModel
import com.samex.kmt_hackathon.core.WatchUiState
import com.samex.kmt_hackathon.core.formatMinutesOfDay
import com.samex.kmt_hackathon.ui.components.*
import com.samex.kmt_hackathon.ui.presentation.*

@Composable
internal fun ActiveWatchSection(model: TransitAppModel, state: WatchUiState, compact: Boolean) {
    var showMoreWindows by remember { mutableStateOf(false) }
    val visibleWindowCount = if (showMoreWindows) 5 else 2
    val canExpandWindows = state.groups.size > 2

    ActiveWatchHero(
        state = state,
        nowSecondsOfDay = model.nowSecondsOfDay,
        compact = compact,
        routeLabels = commuteRouteLabels(model, state.commute),
        onHeadlineClick = (model::debugSkipToNextWatchTransition).takeIf { model.userData.settings.debugModeEnabled },
    ) {
        ActionButtons(compact) {
            Button(
                onClick = hapticClick(HapticEffect.Confirmation, model::markLeaving),
                enabled = state.currentGroup != null && !state.silenced,
                modifier = responsiveButtonModifier(compact),
            ) {
                ButtonLabel("I'm leaving")
            }
            OutlinedButton(
                onClick = hapticClick(HapticEffect.Warning, model::skipCurrentGroup),
                enabled = state.currentGroup != null && !state.silenced,
                modifier = responsiveButtonModifier(compact),
            ) {
                ButtonLabel("Skip this departure")
            }
            OutlinedButton(
                onClick = hapticClick(HapticEffect.Warning, model::stopActiveSession),
                modifier = responsiveButtonModifier(compact),
            ) {
                ButtonLabel("Stop")
            }
        }
    }

    if (state.groups.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Next windows", style = MaterialTheme.typography.titleMedium)
            if (canExpandWindows) {
                TextButton(onClick = hapticClick { showMoreWindows = !showMoreWindows }) {
                    ButtonLabel(if (showMoreWindows) "Show fewer" else "Show more")
                }
            }
        }
        state.groups.take(visibleWindowCount).forEach { group ->
            UpcomingWindowRow(group = group, compact = compact)
        }
    }
}

@Composable
internal fun UpcomingWindowRow(group: com.samex.kmt_hackathon.core.LeaveWindowGroup, compact: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
    ) {
        if (compact) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RouteChipColumn(
                    labels = groupRouteLabels(group),
                    compact = true,
                    maxItems = 3,
                )
                Text(
                    "${formatMinutesOfDay(group.windowOpenMinutes)}-${formatMinutesOfDay(group.finalCallMinutes)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    RouteChipColumn(
                        labels = groupRouteLabels(group),
                        compact = false,
                        maxItems = 3,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "${formatMinutesOfDay(group.windowOpenMinutes)}-${formatMinutesOfDay(group.finalCallMinutes)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
