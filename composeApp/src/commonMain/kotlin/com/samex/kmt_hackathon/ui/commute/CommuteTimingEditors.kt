package com.samex.kmt_hackathon.ui.commute

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.core.CommuteDraft
import com.samex.kmt_hackathon.core.HapticEffect
import com.samex.kmt_hackathon.core.PlatformServices
import com.samex.kmt_hackathon.core.TransitAppModel
import com.samex.kmt_hackathon.core.Weekday
import com.samex.kmt_hackathon.ui.components.*

@Composable
internal fun ArrivalBufferEditor(model: TransitAppModel, draft: CommuteDraft, compact: Boolean) {
    val settings = model.userData.settings
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = draft.overrideArrivalBuffer,
                onCheckedChange = { model.updateCommuteDraft(draft.copy(overrideArrivalBuffer = it)) },
            )
            Spacer(Modifier.width(8.dp))
            Text("Custom arrival buffer")
        }
        Text(
            "Default ${settings.defaultArrivalBuffer.minEarlyMinutes}-${settings.defaultArrivalBuffer.maxEarlyMinutes} min early",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ArrivalBufferFields(
                    model = model,
                    draft = draft,
                    minModifier = Modifier.fillMaxWidth(),
                    maxModifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArrivalBufferFields(
                    model = model,
                    draft = draft,
                    minModifier = Modifier.weight(1f),
                    maxModifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun ScheduleEditor(model: TransitAppModel, draft: CommuteDraft, compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = draft.scheduleEnabled,
                onCheckedChange = { model.updateCommuteDraft(draft.copy(scheduleEnabled = it)) },
            )
            Spacer(Modifier.width(8.dp))
            Text("Auto-start schedule")
        }
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScheduleTimeFields(
                    model = model,
                    draft = draft,
                    startModifier = Modifier.fillMaxWidth(),
                    endModifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScheduleTimeFields(
                    model = model,
                    draft = draft,
                    startModifier = Modifier.weight(1f),
                    endModifier = Modifier.weight(1f),
                )
            }
        }
        WeekdayGrid(model, draft, compact)
    }
}

@Composable
internal fun WeekdayGrid(model: TransitAppModel, draft: CommuteDraft, compact: Boolean) {
    val daysPerRow = if (compact) 3 else 7
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Weekday.entries.chunked(daysPerRow).forEach { rowDays ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowDays.forEach { day ->
                    val selected = day in draft.scheduleDays
                    val modifier = Modifier.weight(1f)
                    val onClick = {
                        val days = if (selected) draft.scheduleDays - day else draft.scheduleDays + day
                        PlatformServices.hapticFeedback().perform(HapticEffect.Selection)
                        model.updateCommuteDraft(draft.copy(scheduleDays = days))
                    }
                    if (selected) {
                        Button(onClick = onClick, enabled = draft.scheduleEnabled, modifier = modifier) {
                            ButtonLabel(day.name.take(3))
                        }
                    } else {
                        OutlinedButton(onClick = onClick, enabled = draft.scheduleEnabled, modifier = modifier) {
                            ButtonLabel(day.name.take(3))
                        }
                    }
                }
                repeat(daysPerRow - rowDays.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun ArrivalBufferFields(
    model: TransitAppModel,
    draft: CommuteDraft,
    minModifier: Modifier,
    maxModifier: Modifier,
) {
    OutlinedTextField(
        value = draft.minEarlyMinutes,
        onValueChange = { model.updateCommuteDraft(draft.copy(minEarlyMinutes = it)) },
        label = { Text("At least early") },
        enabled = draft.overrideArrivalBuffer,
        modifier = minModifier,
    )
    OutlinedTextField(
        value = draft.maxEarlyMinutes,
        onValueChange = { model.updateCommuteDraft(draft.copy(maxEarlyMinutes = it)) },
        label = { Text("At most early") },
        enabled = draft.overrideArrivalBuffer,
        modifier = maxModifier,
    )
}

@Composable
internal fun ScheduleTimeFields(
    model: TransitAppModel,
    draft: CommuteDraft,
    startModifier: Modifier,
    endModifier: Modifier,
) {
    OutlinedTextField(
        value = draft.scheduleStart,
        onValueChange = { model.updateCommuteDraft(draft.copy(scheduleStart = it)) },
        label = { Text("Start") },
        enabled = draft.scheduleEnabled,
        modifier = startModifier,
    )
    OutlinedTextField(
        value = draft.scheduleEnd,
        onValueChange = { model.updateCommuteDraft(draft.copy(scheduleEnd = it)) },
        label = { Text("End") },
        enabled = draft.scheduleEnabled,
        modifier = endModifier,
    )
}
