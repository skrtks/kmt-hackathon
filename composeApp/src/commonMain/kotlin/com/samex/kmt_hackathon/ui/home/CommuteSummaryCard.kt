package com.samex.kmt_hackathon.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.leaveStatusColors
import com.samex.kmt_hackathon.core.HapticEffect
import com.samex.kmt_hackathon.core.SavedCommute
import com.samex.kmt_hackathon.core.TransitAppModel
import com.samex.kmt_hackathon.core.formatMinutesOfDay
import com.samex.kmt_hackathon.ui.components.*
import com.samex.kmt_hackathon.ui.activewatch.*
import com.samex.kmt_hackathon.ui.presentation.*

@Composable
internal fun ActiveStatusPill() {
    val statusColors = leaveStatusColors()
    Surface(
        color = statusColors.signalContainer,
        contentColor = statusColors.onSignalContainer,
        shape = CircleShape,
        tonalElevation = 0.dp,
    ) {
        Text(
            "Watching now",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun ScheduleOrActivePill(commute: SavedCommute, isActive: Boolean) {
    if (isActive) {
        ActiveStatusPill()
    } else {
        SchedulePill(commute)
    }
}

@Composable
internal fun CommuteSummaryCard(
    model: TransitAppModel,
    commute: SavedCommute,
    compact: Boolean,
    isActive: Boolean,
    onAutoStartChange: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onStart: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = 520f, dampingRatio = 0.86f)),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = flatCardElevation(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        commuteOriginName(model, commute),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        model.stopName(commute.stopId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            commuteOriginName(model, commute),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            model.stopName(commute.stopId),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    ScheduleOrActivePill(commute = commute, isActive = isActive)
                }
            }

            if (compact) {
                ScheduleOrActivePill(commute = commute, isActive = isActive)
            }

            RouteChipColumn(
                labels = commuteRouteLabels(model, commute),
                compact = compact,
                maxItems = 4,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            CommuteCardActions(
                compact = compact,
                autoStartEnabled = commute.autoStartEnabled,
                scheduleEnabled = commute.schedule != null,
                onAutoStartChange = onAutoStartChange,
                onDelete = onDelete,
                onEdit = onEdit,
                onStart = onStart,
                startEnabled = !isActive,
                startLabel = if (isActive) "Watching" else "Start",
            )
        }
    }
}

@Composable
internal fun SchedulePill(commute: SavedCommute) {
    val label = commute.schedule?.let { schedule ->
        "${schedule.days.joinToString { it.name.take(3) }} ${formatMinutesOfDay(schedule.startMinutes)}-${formatMinutesOfDay(schedule.endMinutes)}"
    } ?: "Manual"
    val enabled = commute.schedule != null && commute.autoStartEnabled
    Surface(
        color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = CircleShape,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun CommuteCardActions(
    compact: Boolean,
    autoStartEnabled: Boolean,
    scheduleEnabled: Boolean,
    onAutoStartChange: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    startEnabled: Boolean = true,
    startLabel: String = "Start",
) {
    val autoStartControl: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Auto-start")
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = autoStartEnabled,
                onCheckedChange = { hapticClick(onClick = onAutoStartChange)() },
                enabled = scheduleEnabled,
            )
        }
    }
    val actions: @Composable () -> Unit = {
        OutlinedButton(onClick = hapticClick(onClick = onEdit), modifier = responsiveButtonModifier(compact)) {
            ButtonLabel("Edit")
        }
        OutlinedButton(onClick = hapticClick(HapticEffect.Warning, onDelete), modifier = responsiveButtonModifier(compact)) {
            ButtonLabel("Delete")
        }
        Button(
            onClick = hapticClick(HapticEffect.Confirmation, onStart),
            enabled = startEnabled,
            modifier = responsiveButtonModifier(compact),
        ) {
            ButtonLabel(startLabel)
        }
    }

    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            autoStartControl()
            ActionButtons(compact = true) { actions() }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            autoStartControl()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                actions()
            }
        }
    }
}
