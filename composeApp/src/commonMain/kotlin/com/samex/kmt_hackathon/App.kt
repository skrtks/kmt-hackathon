package com.samex.kmt_hackathon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.core.AppScreen
import com.samex.kmt_hackathon.core.CommuteDraft
import com.samex.kmt_hackathon.core.MockTransitRepository
import com.samex.kmt_hackathon.core.NotificationPermissionStatus
import com.samex.kmt_hackathon.core.PlatformServices
import com.samex.kmt_hackathon.core.TransitAppModel
import com.samex.kmt_hackathon.core.UserDataRepository
import com.samex.kmt_hackathon.core.WatchStatus
import com.samex.kmt_hackathon.core.Weekday
import com.samex.kmt_hackathon.core.formatMinutesOfDay
import com.samex.kmt_hackathon.transit.LineDirection
import com.samex.kmt_hackathon.transit.TransitStop
import kotlinx.coroutines.delay

@Composable
@Preview
fun App() {
    val model = remember {
        TransitAppModel(
            transitRepository = MockTransitRepository(),
            userDataRepository = UserDataRepository(PlatformServices.keyValueStore()),
            notificationScheduler = PlatformServices.notificationScheduler(),
            timeProvider = PlatformServices.timeProvider(),
        )
    }

    LaunchedEffect(model) {
        model.load()
        while (true) {
            delay(30_000)
            model.tick()
        }
    }

    MaterialTheme {
        AppContent(model)
    }
}

@Composable
private fun AppContent(model: TransitAppModel) {
    Column(
        modifier = Modifier
            .safeContentPadding()
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Header(model)
        model.errorMessage?.let { ErrorCard(it) }
        model.pendingReplacementCommuteId?.let {
            ReplacementPrompt(
                onConfirm = model::confirmReplacement,
                onCancel = model::cancelReplacement,
            )
        }

        when (val screen = model.screen) {
            AppScreen.Home -> HomeScreen(model)
            is AppScreen.PlaceEditor -> PlaceEditor(model)
            AppScreen.CommuteSetup -> CommuteSetup(model)
            AppScreen.Watch -> WatchScreen(model)
            AppScreen.Settings -> SettingsScreen(model)
            AppScreen.Places -> PlacesScreen(model)
        }
    }
}

@Composable
private fun Header(model: TransitAppModel) {
    CompactAware { compact ->
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderTitle(model)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NavButton("Home", onClick = { model.navigate(AppScreen.Home) })
                    NavButton("Settings", onClick = { model.navigate(AppScreen.Settings) })
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderTitle(model)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NavButton("Home", onClick = { model.navigate(AppScreen.Home) })
                    NavButton("Settings", onClick = { model.navigate(AppScreen.Settings) })
                }
            }
        }
    }
}

@Composable
private fun HeaderTitle(model: TransitAppModel) {
    Column {
        Text(
            "Leave Window",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text("Now ${formatMinutesOfDay(model.nowMinutes)}", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun HomeScreen(model: TransitAppModel) {
    CompactAware { compact ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PermissionCard(model)
            ActiveWatchCard(model, compact)

            ActionButtons(compact) {
                Button(
                    onClick = { model.beginCommuteSetup() },
                    enabled = model.userData.places.isNotEmpty(),
                    modifier = responsiveButtonModifier(compact),
                ) {
                    ButtonLabel("Add commute")
                }
                OutlinedButton(
                    onClick = { model.beginPlaceEditor() },
                    modifier = responsiveButtonModifier(compact),
                ) {
                    ButtonLabel("Add place")
                }
                OutlinedButton(
                    onClick = { model.navigate(AppScreen.Places) },
                    modifier = responsiveButtonModifier(compact),
                ) {
                    ButtonLabel("Places")
                }
            }

            if (model.userData.commutes.isEmpty()) {
                EmptyCard("No saved commutes yet. Create one from a saved place and mock stop.")
            } else {
                model.userData.commutes.forEach { commute ->
                    val origin = model.userData.places.firstOrNull { it.id == commute.originPlaceId }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(model.stopName(commute.stopId), style = MaterialTheme.typography.titleMedium)
                            Text(
                                commute.selections.joinToString { selection ->
                                    "${model.lineShortName(selection.lineId)} to ${model.directionHeadsign(selection.directionId)}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text("From ${origin?.name ?: "Unknown place"}")
                            Text(commute.schedule?.let {
                                "Schedule ${it.days.joinToString { day -> day.name.take(3) }} ${formatMinutesOfDay(it.startMinutes)}-${
                                    formatMinutesOfDay(
                                        it.endMinutes
                                    )
                                }"
                            } ?: "Manual start only")
                            CommuteCardActions(
                                compact = compact,
                                autoStartEnabled = commute.autoStartEnabled,
                                scheduleEnabled = commute.schedule != null,
                                onAutoStartChange = { model.toggleCommuteAutoStart(commute.id) },
                                onDelete = { model.deleteCommute(commute.id) },
                                onStart = { model.startWatch(commute.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveWatchCard(model: TransitAppModel, compact: Boolean) {
    val state = model.watchUiState() ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Active watch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(state.stopName, style = MaterialTheme.typography.bodyLarge)
            state.currentGroup?.let { group ->
                Text("${statusHeadline(state.currentStatus)} for ${formatMinutesOfDay(group.primaryWindow.departureTimeMinutes)}")
                Text("Leave ${formatMinutesOfDay(group.windowOpenMinutes)}-${formatMinutesOfDay(group.finalCallMinutes)}")
            } ?: Text("No upcoming departure window.")
            Button(
                onClick = { model.navigate(AppScreen.Watch) },
                modifier = responsiveButtonModifier(compact),
            ) {
                ButtonLabel("Return to watch")
            }
        }
    }
}

@Composable
private fun PlaceEditor(model: TransitAppModel) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Create saved place", style = MaterialTheme.typography.titleLarge)
        Text("Use a preset for the mock app or enter coordinates manually.")
        CompactAware { compact ->
            ActionButtons(compact) {
                model.presetPlaces.forEach { preset ->
                    OutlinedButton(
                        onClick = { model.applyPresetPlace(preset) },
                        modifier = responsiveButtonModifier(compact),
                    ) {
                        ButtonLabel(preset.name)
                    }
                }
            }
        }
        OutlinedTextField(
            value = model.placeDraft.name,
            onValueChange = { model.updatePlaceDraft(model.placeDraft.copy(name = it)) },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
        )
        CompactAware { compact ->
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlaceCoordinateFields(
                        model = model,
                        latitudeModifier = Modifier.fillMaxWidth(),
                        longitudeModifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlaceCoordinateFields(
                        model = model,
                        latitudeModifier = Modifier.weight(1f),
                        longitudeModifier = Modifier.weight(1f),
                    )
                }
            }
        }
        CompactAware { compact ->
            ActionButtons(compact) {
                Button(onClick = model::savePlace, modifier = responsiveButtonModifier(compact)) {
                    ButtonLabel("Save place")
                }
                if (!model.placeDraft.onboarding) {
                    OutlinedButton(
                        onClick = { model.navigate(AppScreen.Home) },
                        modifier = responsiveButtonModifier(compact),
                    ) {
                        ButtonLabel("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun CommuteSetup(model: TransitAppModel) {
    val draft = model.commuteDraft
    val initialStep = remember {
        if (model.userData.places.size == 1 && draft.originPlaceId.isNotBlank()) {
            CommuteSetupStep.Stop
        } else {
            CommuteSetupStep.Origin
        }
    }
    var step by remember { mutableStateOf(initialStep) }

    CompactAware { compact ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("New commute", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            CommuteSetupSnapshot(model, draft)
            SetupProgress(step)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
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
                onSave = model::saveCommute,
            )
        }
    }
}

private enum class CommuteSetupStep(val title: String) {
    Origin("Origin"),
    Stop("Stop"),
    Lines("Lines"),
    Timing("Timing"),
    Review("Review"),
}

@Composable
private fun CommuteSetupSnapshot(model: TransitAppModel, draft: CommuteDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("From ${originName(model, draft)}", style = MaterialTheme.typography.bodyMedium)
        Text("Stop ${stopName(model, draft)}", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Lines ${linesSummary(model, draft)}",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SetupProgress(step: CommuteSetupStep) {
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
private fun OriginStep(model: TransitAppModel, draft: CommuteDraft) {
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
private fun StopStep(model: TransitAppModel, draft: CommuteDraft, compact: Boolean) {
    var query by remember { mutableStateOf("") }
    val stops = model.stops()
    val visibleStops = stops
        .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
        .sortedWith(compareBy<TransitStop> { if (it.id == draft.stopId) 0 else 1 }.thenBy { it.name })
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
private fun LinesStep(model: TransitAppModel, draft: CommuteDraft, compact: Boolean) {
    var query by remember(draft.stopId) { mutableStateOf("") }
    val directions = model.directionsForDraftStop()
    val visibleDirections = directions
        .filter { direction ->
            val label = lineDirectionLabel(model, direction)
            query.isBlank() || label.contains(query, ignoreCase = true)
        }
        .sortedWith(
            compareBy<LineDirection> { direction ->
                if (draft.selections.any { it.directionId == direction.id }) 0 else 1
            }.thenBy { lineDirectionLabel(model, it) },
        )
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
                val selected = draft.selections.any { it.directionId == direction.id }
                SetupChoice(
                    label = lineDirectionLabel(model, direction),
                    selected = selected,
                    onClick = { model.toggleSelection(direction) },
                )
            }
        }
        Text(
            "${draft.selections.size} selected",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun TimingStep(model: TransitAppModel, draft: CommuteDraft, compact: Boolean) {
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

        HorizontalDivider()

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
private fun ReviewStep(model: TransitAppModel, draft: CommuteDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ReviewLine("Origin", originName(model, draft))
        ReviewLine("Stop", stopName(model, draft))
        ReviewLine("Lines", linesSummary(model, draft))
        ReviewLine("Arrival", arrivalBufferSummary(model, draft))
        ReviewLine("Schedule", scheduleSummary(draft))
    }
}

@Composable
private fun WeekdayGrid(model: TransitAppModel, draft: CommuteDraft, compact: Boolean) {
    val daysPerRow = if (compact) 3 else 7
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Weekday.entries.chunked(daysPerRow).forEach { rowDays ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowDays.forEach { day ->
                    val selected = day in draft.scheduleDays
                    val modifier = Modifier.weight(1f)
                    val onClick = {
                        val days = if (selected) draft.scheduleDays - day else draft.scheduleDays + day
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
private fun SetupChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    val content: @Composable () -> Unit = {
        Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
    if (selected) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            content()
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun ReviewLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SetupNavigation(
    compact: Boolean,
    step: CommuteSetupStep,
    canContinue: Boolean,
    canSave: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    ActionButtons(compact) {
        OutlinedButton(
            onClick = if (step == CommuteSetupStep.Origin) onCancel else onBack,
            modifier = responsiveButtonModifier(compact),
        ) {
            ButtonLabel(if (step == CommuteSetupStep.Origin) "Cancel" else "Back")
        }
        Button(
            onClick = if (step == CommuteSetupStep.Review) onSave else onNext,
            enabled = if (step == CommuteSetupStep.Review) canSave else canContinue,
            modifier = responsiveButtonModifier(compact),
        ) {
            ButtonLabel(if (step == CommuteSetupStep.Review) "Save commute" else "Next")
        }
    }
}

private fun nextSetupStep(step: CommuteSetupStep): CommuteSetupStep =
    CommuteSetupStep.entries.getOrElse(step.ordinal + 1) { step }

private fun previousSetupStep(step: CommuteSetupStep): CommuteSetupStep =
    CommuteSetupStep.entries.getOrElse(step.ordinal - 1) { step }

private fun canContinueSetupStep(step: CommuteSetupStep, draft: CommuteDraft): Boolean =
    when (step) {
        CommuteSetupStep.Origin -> draft.originPlaceId.isNotBlank()
        CommuteSetupStep.Stop -> draft.stopId.isNotBlank()
        CommuteSetupStep.Lines -> draft.selections.isNotEmpty()
        CommuteSetupStep.Timing -> isTimingDraftValid(draft)
        CommuteSetupStep.Review -> canSaveCommuteDraft(draft)
    }

private fun canSaveCommuteDraft(draft: CommuteDraft): Boolean =
    draft.originPlaceId.isNotBlank() &&
            draft.stopId.isNotBlank() &&
            draft.selections.isNotEmpty() &&
            isTimingDraftValid(draft)

private fun isTimingDraftValid(draft: CommuteDraft): Boolean {
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

private fun parseSetupMinutesOfDay(value: String): Int? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun originName(model: TransitAppModel, draft: CommuteDraft): String =
    model.userData.places.firstOrNull { it.id == draft.originPlaceId }?.name ?: "Not set"

private fun stopName(model: TransitAppModel, draft: CommuteDraft): String =
    draft.stopId.takeIf { it.isNotBlank() }?.let(model::stopName) ?: "Not set"

private fun linesSummary(model: TransitAppModel, draft: CommuteDraft): String =
    if (draft.selections.isEmpty()) {
        "Not set"
    } else {
        draft.selections.joinToString { selection ->
            "${model.lineShortName(selection.lineId)} to ${model.directionHeadsign(selection.directionId)}"
        }
    }

private fun lineDirectionLabel(model: TransitAppModel, direction: LineDirection): String =
    "${model.lineShortName(direction.lineId)} to ${direction.headsign}"

private fun arrivalBufferSummary(model: TransitAppModel, draft: CommuteDraft): String {
    val buffer = if (draft.overrideArrivalBuffer) {
        "${draft.minEarlyMinutes}-${draft.maxEarlyMinutes}"
    } else {
        val default = model.userData.settings.defaultArrivalBuffer
        "${default.minEarlyMinutes}-${default.maxEarlyMinutes}"
    }
    return "$buffer min early"
}

private fun scheduleSummary(draft: CommuteDraft): String =
    if (draft.scheduleEnabled) {
        "${
            draft.scheduleDays.sortedBy { it.ordinal }.joinToString { it.name.take(3) }
        } ${draft.scheduleStart}-${draft.scheduleEnd}"
    } else {
        "Manual start only"
    }

@Composable
private fun WatchScreen(model: TransitAppModel) {
    val state = model.watchUiState()
    if (state == null) {
        EmptyCard("No active watch session.")
        Button(onClick = { model.navigate(AppScreen.Home) }) { Text("Back home") }
        return
    }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PermissionCard(model)
        state.errorMessage?.let { ErrorCard(it) }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    statusHeadline(state.currentStatus),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(state.stopName, style = MaterialTheme.typography.titleMedium)
                Text("Walk time ${state.walkingTimeMinutes} min from ${state.origin.name}")
                state.currentGroup?.let { group ->
                    Text("Leave ${formatMinutesOfDay(group.windowOpenMinutes)}-${formatMinutesOfDay(group.finalCallMinutes)}")
                    Text("Departure ${formatMinutesOfDay(group.primaryWindow.departureTimeMinutes)}")
                    Text(group.windows.joinToString { "${it.lineShortName} to ${it.headsign}" })
                } ?: Text("No upcoming departure window.")
                if (state.silenced) Text("Notifications silenced after leaving.")
                CompactAware { compact ->
                    ActionButtons(compact) {
                        Button(
                            onClick = model::markLeaving,
                            enabled = !state.silenced,
                            modifier = responsiveButtonModifier(compact),
                        ) {
                            ButtonLabel("I'm leaving")
                        }
                        OutlinedButton(
                            onClick = model::skipCurrentGroup,
                            enabled = state.currentGroup != null && !state.silenced,
                            modifier = responsiveButtonModifier(compact),
                        ) {
                            ButtonLabel("Skip this departure")
                        }
                        OutlinedButton(
                            onClick = model::stopActiveSession,
                            modifier = responsiveButtonModifier(compact),
                        ) {
                            ButtonLabel("Stop")
                        }
                    }
                }
            }
        }

        Text("Upcoming windows", style = MaterialTheme.typography.titleMedium)
        state.groups.take(8).forEach { group ->
            CompactAware { compact ->
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(group.windows.joinToString { "${it.lineShortName} ${formatMinutesOfDay(it.departureTimeMinutes)}" })
                        Text("${formatMinutesOfDay(group.windowOpenMinutes)}-${formatMinutesOfDay(group.finalCallMinutes)}")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(group.windows.joinToString { "${it.lineShortName} ${formatMinutesOfDay(it.departureTimeMinutes)}" })
                        Text("${formatMinutesOfDay(group.windowOpenMinutes)}-${formatMinutesOfDay(group.finalCallMinutes)}")
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun PlacesScreen(model: TransitAppModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CompactAware { compact ->
            ActionButtons(compact) {
                Button(
                    onClick = { model.beginPlaceEditor() },
                    modifier = responsiveButtonModifier(compact),
                ) {
                    ButtonLabel("Add place")
                }
                OutlinedButton(
                    onClick = { model.navigate(AppScreen.Home) },
                    modifier = responsiveButtonModifier(compact),
                ) {
                    ButtonLabel("Back")
                }
            }
        }
        model.userData.places.forEach { place ->
            Card(modifier = Modifier.fillMaxWidth()) {
                CompactAware { compact ->
                    if (compact) {
                        Column(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PlaceSummary(place.name, "${place.location.latitude}, ${place.location.longitude}")
                            OutlinedButton(
                                onClick = { model.deletePlace(place.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                ButtonLabel("Delete")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PlaceSummary(place.name, "${place.location.latitude}, ${place.location.longitude}")
                            OutlinedButton(onClick = { model.deletePlace(place.id) }) {
                                ButtonLabel("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(model: TransitAppModel) {
    val settings = model.userData.settings
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        Section("Global arrival buffer") {
            SettingStepperRow(
                label = "At least ${settings.defaultArrivalBuffer.minEarlyMinutes} min early",
                onMinus = {
                    model.updateDefaultArrivalBuffer(
                        (settings.defaultArrivalBuffer.minEarlyMinutes - 1).coerceAtLeast(0),
                        settings.defaultArrivalBuffer.maxEarlyMinutes,
                    )
                },
                onPlus = {
                    model.updateDefaultArrivalBuffer(
                        settings.defaultArrivalBuffer.minEarlyMinutes + 1,
                        settings.defaultArrivalBuffer.maxEarlyMinutes.coerceAtLeast(settings.defaultArrivalBuffer.minEarlyMinutes + 1),
                    )
                },
            )
            SettingStepperRow(
                label = "At most ${settings.defaultArrivalBuffer.maxEarlyMinutes} min early",
                onMinus = {
                    model.updateDefaultArrivalBuffer(
                        settings.defaultArrivalBuffer.minEarlyMinutes,
                        (settings.defaultArrivalBuffer.maxEarlyMinutes - 1).coerceAtLeast(settings.defaultArrivalBuffer.minEarlyMinutes),
                    )
                },
                onPlus = {
                    model.updateDefaultArrivalBuffer(
                        settings.defaultArrivalBuffer.minEarlyMinutes,
                        settings.defaultArrivalBuffer.maxEarlyMinutes + 1,
                    )
                },
            )
        }
        Section("Walking speed") {
            SettingStepperRow(
                label = "${settings.walkingSpeed.metersPerMinute.toInt()} meters/min",
                onMinus = { model.updateWalkingSpeed((settings.walkingSpeed.metersPerMinute - 5).coerceAtLeast(30.0)) },
                onPlus = { model.updateWalkingSpeed(settings.walkingSpeed.metersPerMinute + 5) },
            )
        }
        Section("Notifications") {
            Text("Status: ${model.notificationStatus.name}")
            Button(onClick = model::requestNotificationPermission) {
                Text("Request permission")
            }
        }
    }
}

@Composable
private fun PermissionCard(model: TransitAppModel) {
    if (model.notificationStatus == NotificationPermissionStatus.Granted) return
    Card(modifier = Modifier.fillMaxWidth()) {
        CompactAware { compact ->
            val buttonVisible = model.notificationStatus != NotificationPermissionStatus.Unsupported
            val message = if (model.notificationStatus == NotificationPermissionStatus.Unsupported) {
                "Notifications are unsupported on this platform. Foreground watching still works."
            } else {
                "Notifications are ${model.notificationStatus.name.lowercase()}. Foreground watching still works."
            }
            if (compact || !buttonVisible) {
                Column(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(message)
                    if (buttonVisible) {
                        Button(onClick = model::requestNotificationPermission, modifier = Modifier.fillMaxWidth()) {
                            ButtonLabel("Enable")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(message, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = model::requestNotificationPermission) {
                        ButtonLabel("Enable")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplacementPrompt(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Another watch session is active.")
            CompactAware { compact ->
                ActionButtons(compact) {
                    Button(onClick = onConfirm, modifier = responsiveButtonModifier(compact)) {
                        ButtonLabel("Replace")
                    }
                    OutlinedButton(onClick = onCancel, modifier = responsiveButtonModifier(compact)) {
                        ButtonLabel("Keep current")
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun Stepper(onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = onMinus) { ButtonLabel("-") }
        OutlinedButton(onClick = onPlus) { ButtonLabel("+") }
    }
}

@Composable
private fun CompactAware(
    threshold: Dp = 520.dp,
    content: @Composable (compact: Boolean) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        content(maxWidth < threshold)
    }
}

@Composable
private fun ActionButtons(compact: Boolean, content: @Composable () -> Unit) {
    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

private fun responsiveButtonModifier(compact: Boolean): Modifier =
    if (compact) Modifier.fillMaxWidth() else Modifier.widthIn(min = 96.dp)

@Composable
private fun ButtonLabel(text: String) {
    Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun NavButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.widthIn(min = 72.dp)) {
        ButtonLabel(text)
    }
}

@Composable
private fun PlaceCoordinateFields(
    model: TransitAppModel,
    latitudeModifier: Modifier,
    longitudeModifier: Modifier,
) {
    OutlinedTextField(
        value = model.placeDraft.latitude,
        onValueChange = { model.updatePlaceDraft(model.placeDraft.copy(latitude = it)) },
        label = { Text("Latitude") },
        modifier = latitudeModifier,
    )
    OutlinedTextField(
        value = model.placeDraft.longitude,
        onValueChange = { model.updatePlaceDraft(model.placeDraft.copy(longitude = it)) },
        label = { Text("Longitude") },
        modifier = longitudeModifier,
    )
}

@Composable
private fun ArrivalBufferFields(
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
private fun ScheduleTimeFields(
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

@Composable
private fun CommuteCardActions(
    compact: Boolean,
    autoStartEnabled: Boolean,
    scheduleEnabled: Boolean,
    onAutoStartChange: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit,
) {
    val autoStartControl: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Auto-start")
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = autoStartEnabled,
                onCheckedChange = { onAutoStartChange() },
                enabled = scheduleEnabled,
            )
        }
    }
    val actions: @Composable () -> Unit = {
        OutlinedButton(onClick = onDelete, modifier = responsiveButtonModifier(compact)) {
            ButtonLabel("Delete")
        }
        Button(onClick = onStart, modifier = responsiveButtonModifier(compact)) {
            ButtonLabel("Start")
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

@Composable
private fun PlaceSummary(name: String, locationText: String) {
    Column {
        Text(name, style = MaterialTheme.typography.titleMedium)
        Text(locationText)
    }
}

@Composable
private fun SettingStepperRow(label: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    CompactAware { compact ->
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label)
                Stepper(onMinus = onMinus, onPlus = onPlus)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label)
                Stepper(onMinus = onMinus, onPlus = onPlus)
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun EmptyCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(message, modifier = Modifier.padding(16.dp))
    }
}

private fun statusHeadline(status: WatchStatus?): String =
    when (status) {
        WatchStatus.GetReady -> "Get ready"
        WatchStatus.LeaveNow -> "Leave now"
        WatchStatus.FinalCall -> "Final call"
        WatchStatus.Missed -> "Next chance"
        null -> "Watching"
    }
