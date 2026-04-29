package com.samex.kmt_hackathon.ui.activewatch

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.leaveStatusColors
import com.samex.kmt_hackathon.core.LeaveWindowGroup
import com.samex.kmt_hackathon.core.WatchStatus
import com.samex.kmt_hackathon.core.WatchUiState
import com.samex.kmt_hackathon.core.formatMinutesOfDay
import com.samex.kmt_hackathon.ui.components.*
import com.samex.kmt_hackathon.ui.presentation.*

@Composable
internal fun ActiveWatchHero(
    state: WatchUiState,
    nowSecondsOfDay: Int,
    compact: Boolean,
    routeLabels: List<String>,
    onHeadlineClick: (() -> Unit)? = null,
    actions: @Composable () -> Unit,
) {
    val status = state.currentStatus
    val containerColor by animateColorAsState(
        targetValue = statusContainerColor(status),
        animationSpec = TweenSpec(durationMillis = 300),
        label = "watchHeroContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = statusContentColor(status),
        animationSpec = TweenSpec(durationMillis = 300),
        label = "watchHeroContent",
    )
    val borderColor by animateColorAsState(
        targetValue = statusBorderColor(status),
        animationSpec = TweenSpec(durationMillis = 300),
        label = "watchHeroBorder",
    )
    val currentGroup = state.currentGroup
    val statusStyle = if (compact) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displaySmall
    val headline = state.leavingDepartureTimeMinutes
        ?.takeIf { state.silenced }
        ?.let { departureCountdownHeadline(departureTimeMinutes = it, nowSecondsOfDay = nowSecondsOfDay) }
        ?: statusHeadline(
            status = status,
            leaveAtMinutes = currentGroup?.windowOpenMinutes,
        )
    val headlineModifier = if (onHeadlineClick != null) {
        Modifier.clickable(onClick = hapticClick(onClick = onHeadlineClick))
    } else {
        Modifier
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = 450f, dampingRatio = 0.9f)),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        border = BorderStroke(2.dp, borderColor),
        elevation = flatCardElevation(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    headline,
                    modifier = headlineModifier,
                    style = statusStyle,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.stopName,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor.copy(alpha = 0.82f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            RouteChipColumn(
                labels = currentGroup?.let(::groupRouteLabels) ?: routeLabels,
                compact = compact,
                maxItems = 5,
            )

            currentGroup?.let { group ->
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WatchMetric("Walk", "${state.walkingTimeMinutes} min")
                        WatchMetric("Departure", formatMinutesOfDay(group.primaryWindow.departureTimeMinutes))
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WatchMetric(
                            label = "Walk",
                            value = "${state.walkingTimeMinutes} min",
                            modifier = Modifier.weight(1f),
                        )
                        WatchMetric(
                            label = "Departure",
                            value = formatMinutesOfDay(group.primaryWindow.departureTimeMinutes),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                val isLeaving = state.silenced && state.leavingDepartureTimeMinutes != null
                val showWindowCountdown = !isLeaving && status != WatchStatus.FinalCall
                if (showWindowCountdown) {
                    LeaveWindowCountdown(
                        group = group,
                        nowSecondsOfDay = nowSecondsOfDay,
                        status = status,
                        compact = compact,
                    )
                }
            } ?: Text(
                "No upcoming departure window.",
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor.copy(alpha = 0.82f),
            )

            actions()
        }
    }
}

@Composable
internal fun WatchMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun LeaveWindowCountdown(
    group: LeaveWindowGroup,
    nowSecondsOfDay: Int,
    status: WatchStatus?,
    compact: Boolean,
) {
    val statusColors = leaveStatusColors()
    val targetAccentColor = when (status) {
        WatchStatus.GetReady -> statusColors.route
        WatchStatus.FinalCall -> statusColors.finalCall
        WatchStatus.Missed -> MaterialTheme.colorScheme.error
        else -> statusColors.signal
    }
    val accentColor by animateColorAsState(
        targetValue = targetAccentColor,
        animationSpec = TweenSpec(durationMillis = 300),
        label = "windowCountdownAccent",
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(
            width = 1.dp,
            color = accentColor.copy(alpha = 0.34f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = if (compact) 14.dp else 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    windowCountdownLabel(status),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    windowCountdownValue(group, status, nowSecondsOfDay),
                    style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.64f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = CircleShape,
            ) {
                Text(
                    "Leave ${formatMinutesOfDay(group.windowOpenMinutes)}  •  Final ${formatMinutesOfDay(group.finalCallMinutes)}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

internal fun windowCountdownLabel(status: WatchStatus?): String =
    when (status) {
        WatchStatus.GetReady -> "Window opens at"
        WatchStatus.LeaveNow -> "Window closes in"
        WatchStatus.FinalCall -> "Window"
        WatchStatus.Missed -> "Window"
        null -> "Window"
    }

internal fun windowCountdownValue(
    group: LeaveWindowGroup,
    status: WatchStatus?,
    nowSecondsOfDay: Int,
): String = when (status) {
    WatchStatus.GetReady -> formatMinutesOfDay(group.windowOpenMinutes)
    WatchStatus.LeaveNow -> formatDepartureCountdown(group.finalCallMinutes, nowSecondsOfDay)
    WatchStatus.FinalCall -> "Window closed"
    WatchStatus.Missed -> "Closed"
    null -> "Watching"
}

internal fun statusHeadline(status: WatchStatus?, leaveAtMinutes: Int? = null): String {
    val leaveAtText = leaveAtMinutes?.let(::formatMinutesOfDay)
    return when (status) {
        WatchStatus.GetReady -> leaveAtText?.let { "Leave at $it" } ?: "Leave soon"
        WatchStatus.LeaveNow -> "Leave now"
        WatchStatus.FinalCall -> "Final call"
        WatchStatus.Missed -> leaveAtText?.let { "Next chance at $it" } ?: "Next chance"
        null -> leaveAtText?.let { "Leave at $it" } ?: "Watching"
    }
}

internal fun departureCountdownHeadline(departureTimeMinutes: Int, nowSecondsOfDay: Int): String =
    "Departure in ${formatDepartureCountdown(departureTimeMinutes, nowSecondsOfDay)}"
internal fun formatDepartureCountdown(departureTimeMinutes: Int, nowSecondsOfDay: Int): String {
    val departureSeconds = departureTimeMinutes * 60
    val remainingSeconds = (departureSeconds - nowSecondsOfDay).coerceAtLeast(0)
    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60
    return if (hours > 0) {
        "${hours}h ${minutes.toString().padStart(2, '0')}m"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
