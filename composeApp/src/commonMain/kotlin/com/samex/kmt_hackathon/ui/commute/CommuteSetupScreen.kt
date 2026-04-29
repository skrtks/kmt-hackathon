package com.samex.kmt_hackathon.ui.commute

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.core.AppScreen
import com.samex.kmt_hackathon.core.CommuteDraft
import com.samex.kmt_hackathon.core.CommuteLineSelection
import com.samex.kmt_hackathon.core.TransitAppModel
import com.samex.kmt_hackathon.ui.components.*

@Composable
internal fun CommuteSetup(model: TransitAppModel) {
    val draft = model.commuteDraft
    val initialStep = remember {
        if (model.userData.places.size == 1 && draft.originPlaceId.isNotBlank()) {
            CommuteSetupStep.Stop
        } else {
            CommuteSetupStep.Origin
        }
    }
    var step by remember { mutableStateOf(initialStep) }

    CompactAware(modifier = Modifier.fillMaxSize()) { compact ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("New commute", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            CommuteSetupSnapshot(model, draft)
            SetupProgress(step)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = flatCardElevation(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(step.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    when (step) {
                        CommuteSetupStep.Origin -> OriginStep(model, draft)
                        CommuteSetupStep.Stop -> StopStep(model, draft, compact)
                        CommuteSetupStep.Lines -> LinesStep(model, draft, compact)
                        CommuteSetupStep.Timing -> TimingStep(model, draft, compact)
                        CommuteSetupStep.Review -> ReviewStep(model, draft)
                    }
                }
            }

            SetupNavigation(
                compact = compact,
                step = step,
                canContinue = canContinueSetupStep(step, draft),
                canSave = canSaveCommuteDraft(draft),
                onBack = { step = previousSetupStep(step) },
                onNext = { step = nextSetupStep(step) },
                onCancel = { model.navigate(AppScreen.Home) },
                onSave = hapticResultClick(model, onClick = model::saveCommute),
            )
            BottomNavigationScrollSpacer()
        }
    }
}

internal enum class CommuteSetupStep(val title: String) {
    Origin("Origin"),
    Stop("Stop"),
    Lines("Line"),
    Timing("Timing"),
    Review("Review"),
}

@Composable
internal fun CommuteSetupSnapshot(model: TransitAppModel, draft: CommuteDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("From ${originName(model, draft)}", style = MaterialTheme.typography.bodyMedium)
        Text("Stop ${stopName(model, draft)}", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Line ${linesSummary(model, draft)}",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun SetupProgress(step: CommuteSetupStep) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "${step.ordinal + 1} of ${CommuteSetupStep.entries.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        HorizontalDivider()
    }
}

@Composable
internal fun OriginStep(model: TransitAppModel, draft: CommuteDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        model.userData.places.forEach { place ->
            SetupChoice(
                label = place.name,
                selected = draft.originPlaceId == place.id,
                onClick = { model.updateCommuteDraft(draft.copy(originPlaceId = place.id)) },
            )
        }
    }
}

@Composable
internal fun StopStep(model: TransitAppModel, draft: CommuteDraft, compact: Boolean) {
    var query by remember { mutableStateOf("") }
    val stops = model.stops()
    val visibleStops = stops
        .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
        .sortedBy { it.name }
        .take(if (compact) 7 else 10)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Stop search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (visibleStops.isEmpty()) {
            Text("No matching stops.", style = MaterialTheme.typography.bodyMedium)
        } else {
            visibleStops.forEach { stop ->
                SetupChoice(
                    label = stop.name,
                    selected = draft.stopId == stop.id,
                    onClick = { model.selectStop(stop.id) },
                )
            }
        }
        if (visibleStops.size < stops.size) {
            Text(
                "${visibleStops.size} of ${stops.size} stops",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
internal fun LinesStep(model: TransitAppModel, draft: CommuteDraft, compact: Boolean) {
    var query by remember(draft.stopId) { mutableStateOf("") }
    val directions = model.directionsForDraftStop()
    val visibleDirections = directions
        .filter { direction ->
            val label = lineDirectionLabel(model, direction)
            query.isBlank() || label.contains(query, ignoreCase = true)
        }
        .sortedBy { lineDirectionLabel(model, it) }
        .take(if (compact) 7 else 10)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (directions.size > 5 || query.isNotBlank()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Line search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (visibleDirections.isEmpty()) {
            Text("No lines for this stop.", style = MaterialTheme.typography.bodyMedium)
        } else {
            visibleDirections.forEach { direction ->
                val selection = CommuteLineSelection(direction.lineId, direction.id)
                val selected = selection in draft.selections
                SetupChoice(
                    label = lineDirectionLabel(model, direction),
                    selected = selected,
                    onClick = { model.selectLineDirection(direction) },
                )
            }
        }
        Text(
            draft.selections.firstOrNull()?.let { selection ->
                "Selected ${model.lineShortName(selection.lineId)} to ${model.directionHeadsign(selection.directionId)}"
            } ?: "Choose one line and direction",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun TimingStep(model: TransitAppModel, draft: CommuteDraft, compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ArrivalBufferEditor(model, draft, compact)
        HorizontalDivider()
        ScheduleEditor(model, draft, compact)
    }
}

@Composable
internal fun ReviewStep(model: TransitAppModel, draft: CommuteDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ReviewLine("Origin", originName(model, draft))
        ReviewLine("Stop", stopName(model, draft))
        ReviewLine("Line", linesSummary(model, draft))
        ReviewLine("Arrival", arrivalBufferSummary(model, draft))
        ReviewLine("Schedule", scheduleSummary(draft))
    }
}

@Composable
internal fun SetupNavigation(
    compact: Boolean,
    step: CommuteSetupStep,
    canContinue: Boolean,
    canSave: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    saveLabel: String = "Save commute",
) {
    ActionButtons(compact) {
        OutlinedButton(
            onClick = hapticClick(onClick = if (step == CommuteSetupStep.Origin) onCancel else onBack),
            modifier = responsiveButtonModifier(compact),
        ) {
            ButtonLabel(if (step == CommuteSetupStep.Origin) "Cancel" else "Back")
        }
        Button(
            onClick = if (step == CommuteSetupStep.Review) {
                onSave
            } else {
                hapticClick(onClick = onNext)
            },
            enabled = if (step == CommuteSetupStep.Review) canSave else canContinue,
            modifier = responsiveButtonModifier(compact),
        ) {
            ButtonLabel(if (step == CommuteSetupStep.Review) saveLabel else "Next")
        }
    }
}

internal fun nextSetupStep(step: CommuteSetupStep): CommuteSetupStep =
    CommuteSetupStep.entries.getOrElse(step.ordinal + 1) { step }

internal fun previousSetupStep(step: CommuteSetupStep): CommuteSetupStep =
    CommuteSetupStep.entries.getOrElse(step.ordinal - 1) { step }

internal fun canContinueSetupStep(step: CommuteSetupStep, draft: CommuteDraft): Boolean =
    when (step) {
        CommuteSetupStep.Origin -> draft.originPlaceId.isNotBlank()
        CommuteSetupStep.Stop -> draft.stopId.isNotBlank()
        CommuteSetupStep.Lines -> draft.selections.size == 1
        CommuteSetupStep.Timing -> isTimingDraftValid(draft)
        CommuteSetupStep.Review -> canSaveCommuteDraft(draft)
    }

internal fun canSaveCommuteDraft(draft: CommuteDraft): Boolean =
    draft.originPlaceId.isNotBlank() &&
            draft.stopId.isNotBlank() &&
            draft.selections.size == 1 &&
            isTimingDraftValid(draft)
internal fun isTimingDraftValid(draft: CommuteDraft): Boolean {
    val bufferValid = if (draft.overrideArrivalBuffer) {
        val min = draft.minEarlyMinutes.toIntOrNull()
        val max = draft.maxEarlyMinutes.toIntOrNull()
        min != null && max != null && min >= 0 && max >= min
    } else {
        true
    }
    val scheduleValid = if (draft.scheduleEnabled) {
        val start = parseSetupMinutesOfDay(draft.scheduleStart)
        val end = parseSetupMinutesOfDay(draft.scheduleEnd)
        start != null && end != null && start < end && draft.scheduleDays.isNotEmpty()
    } else {
        true
    }
    return bufferValid && scheduleValid
}

internal fun parseSetupMinutesOfDay(value: String): Int? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}
