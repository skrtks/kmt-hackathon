package com.samex.kmt_hackathon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Leave Window", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Now ${formatMinutesOfDay(model.nowMinutes)}", style = MaterialTheme.typography.bodyMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { model.navigate(AppScreen.Home) }) { Text("Home") }
            TextButton(onClick = { model.navigate(AppScreen.Settings) }) { Text("Settings") }
        }
    }
}

@Composable
private fun HomeScreen(model: TransitAppModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PermissionCard(model)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { model.beginCommuteSetup() }, enabled = model.userData.places.isNotEmpty()) {
                Text("Add commute")
            }
            OutlinedButton(onClick = { model.beginPlaceEditor() }) {
                Text("Add place")
            }
            OutlinedButton(onClick = { model.navigate(AppScreen.Places) }) {
                Text("Places")
            }
        }

        if (model.userData.commutes.isEmpty()) {
            EmptyCard("No saved commutes yet. Create one from a saved place and mock stop.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(model.userData.commutes, key = { it.id }) { commute ->
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
                                "Schedule ${it.days.joinToString { day -> day.name.take(3) }} ${formatMinutesOfDay(it.startMinutes)}-${formatMinutesOfDay(it.endMinutes)}"
                            } ?: "Manual start only")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Auto-start")
                                    Spacer(Modifier.width(8.dp))
                                    Switch(
                                        checked = commute.autoStartEnabled,
                                        onCheckedChange = { model.toggleCommuteAutoStart(commute.id) },
                                        enabled = commute.schedule != null,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { model.deleteCommute(commute.id) }) {
                                        Text("Delete")
                                    }
                                    Button(onClick = { model.startWatch(commute.id) }) {
                                        Text("Start")
                                    }
                                }
                            }
                        }
                    }
                }
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            model.presetPlaces.forEach { preset ->
                OutlinedButton(onClick = { model.applyPresetPlace(preset) }) {
                    Text(preset.name)
                }
            }
        }
        OutlinedTextField(
            value = model.placeDraft.name,
            onValueChange = { model.updatePlaceDraft(model.placeDraft.copy(name = it)) },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = model.placeDraft.latitude,
                onValueChange = { model.updatePlaceDraft(model.placeDraft.copy(latitude = it)) },
                label = { Text("Latitude") },
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = model.placeDraft.longitude,
                onValueChange = { model.updatePlaceDraft(model.placeDraft.copy(longitude = it)) },
                label = { Text("Longitude") },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = model::savePlace) {
                Text("Save place")
            }
            if (!model.placeDraft.onboarding) {
                OutlinedButton(onClick = { model.navigate(AppScreen.Home) }) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun CommuteSetup(model: TransitAppModel) {
    val draft = model.commuteDraft
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Create saved commute", style = MaterialTheme.typography.titleLarge)

        Section("1. Stop") {
            model.stops().forEach { stop ->
                SelectRow(
                    label = stop.name,
                    selected = draft.stopId == stop.id,
                    onClick = { model.selectStop(stop.id) },
                )
            }
        }

        Section("2. Lines and directions") {
            model.directionsForDraftStop().forEach { direction ->
                val selected = draft.selections.any { it.directionId == direction.id }
                SelectRow(
                    label = "${model.lineShortName(direction.lineId)} to ${direction.headsign}",
                    selected = selected,
                    onClick = { model.toggleSelection(direction) },
                )
            }
        }

        Section("3. Origin") {
            model.userData.places.forEach { place ->
                SelectRow(
                    label = place.name,
                    selected = draft.originPlaceId == place.id,
                    onClick = { model.updateCommuteDraft(draft.copy(originPlaceId = place.id)) },
                )
            }
        }

        ArrivalBufferEditor(model, draft)
        ScheduleEditor(model, draft)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = model::saveCommute) {
                Text("Save commute")
            }
            OutlinedButton(onClick = { model.navigate(AppScreen.Home) }) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun ArrivalBufferEditor(model: TransitAppModel, draft: CommuteDraft) {
    Section("4. Arrival buffer") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = draft.overrideArrivalBuffer,
                onCheckedChange = { model.updateCommuteDraft(draft.copy(overrideArrivalBuffer = it)) },
            )
            Text("Override global default")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft.minEarlyMinutes,
                onValueChange = { model.updateCommuteDraft(draft.copy(minEarlyMinutes = it)) },
                label = { Text("At least early") },
                enabled = draft.overrideArrivalBuffer,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = draft.maxEarlyMinutes,
                onValueChange = { model.updateCommuteDraft(draft.copy(maxEarlyMinutes = it)) },
                label = { Text("At most early") },
                enabled = draft.overrideArrivalBuffer,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ScheduleEditor(model: TransitAppModel, draft: CommuteDraft) {
    Section("5. Schedule") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = draft.scheduleEnabled,
                onCheckedChange = { model.updateCommuteDraft(draft.copy(scheduleEnabled = it)) },
            )
            Text("Enable auto-start schedule")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft.scheduleStart,
                onValueChange = { model.updateCommuteDraft(draft.copy(scheduleStart = it)) },
                label = { Text("Start") },
                enabled = draft.scheduleEnabled,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = draft.scheduleEnd,
                onValueChange = { model.updateCommuteDraft(draft.copy(scheduleEnd = it)) },
                label = { Text("End") },
                enabled = draft.scheduleEnabled,
                modifier = Modifier.weight(1f),
            )
        }
        Weekday.entries.forEach { day ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = day in draft.scheduleDays,
                    onCheckedChange = {
                        val days = if (day in draft.scheduleDays) draft.scheduleDays - day else draft.scheduleDays + day
                        model.updateCommuteDraft(draft.copy(scheduleDays = days))
                    },
                    enabled = draft.scheduleEnabled,
                )
                Text(day.name)
            }
        }
    }
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
                Text(statusHeadline(state.currentStatus), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(state.stopName, style = MaterialTheme.typography.titleMedium)
                Text("Walk time ${state.walkingTimeMinutes} min from ${state.origin.name}")
                state.currentGroup?.let { group ->
                    Text("Leave ${formatMinutesOfDay(group.windowOpenMinutes)}-${formatMinutesOfDay(group.finalCallMinutes)}")
                    Text("Departure ${formatMinutesOfDay(group.primaryWindow.departureTimeMinutes)}")
                    Text(group.windows.joinToString { "${it.lineShortName} to ${it.headsign}" })
                } ?: Text("No upcoming departure window.")
                if (state.silenced) Text("Notifications silenced after leaving.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = model::markLeaving, enabled = !state.silenced) {
                        Text("I'm leaving")
                    }
                    OutlinedButton(onClick = model::skipCurrentGroup, enabled = state.currentGroup != null && !state.silenced) {
                        Text("Skip this departure")
                    }
                    OutlinedButton(onClick = model::stopActiveSession) {
                        Text("Stop")
                    }
                }
            }
        }

        Text("Upcoming windows", style = MaterialTheme.typography.titleMedium)
        state.groups.take(8).forEach { group ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(group.windows.joinToString { "${it.lineShortName} ${formatMinutesOfDay(it.departureTimeMinutes)}" })
                Text("${formatMinutesOfDay(group.windowOpenMinutes)}-${formatMinutesOfDay(group.finalCallMinutes)}")
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun PlacesScreen(model: TransitAppModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { model.beginPlaceEditor() }) { Text("Add place") }
            OutlinedButton(onClick = { model.navigate(AppScreen.Home) }) { Text("Back") }
        }
        model.userData.places.forEach { place ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(place.name, style = MaterialTheme.typography.titleMedium)
                        Text("${place.location.latitude}, ${place.location.longitude}")
                    }
                    OutlinedButton(onClick = { model.deletePlace(place.id) }) {
                        Text("Delete")
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("At least ${settings.defaultArrivalBuffer.minEarlyMinutes} min early")
                Stepper(
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
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("At most ${settings.defaultArrivalBuffer.maxEarlyMinutes} min early")
                Stepper(
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
        }
        Section("Walking speed") {
            Text("${settings.walkingSpeed.metersPerMinute.toInt()} meters/min")
            Stepper(
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
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Notifications are ${model.notificationStatus.name.lowercase()}")
                Text("Foreground watching still works.")
            }
            Button(onClick = model::requestNotificationPermission) {
                Text("Enable")
            }
        }
    }
}

@Composable
private fun ReplacementPrompt(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Another watch session is active.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onConfirm) { Text("Replace") }
                OutlinedButton(onClick = onCancel) { Text("Keep current") }
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
private fun SelectRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = selected, onCheckedChange = { onClick() })
        Text(label)
    }
}

@Composable
private fun Stepper(onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = onMinus) { Text("-") }
        OutlinedButton(onClick = onPlus) { Text("+") }
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
