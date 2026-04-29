package com.samex.kmt_hackathon.ui.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.core.AppScreen
import com.samex.kmt_hackathon.core.TransitAppModel
import com.samex.kmt_hackathon.ui.components.*

@Composable
internal fun PlaceEditor(model: TransitAppModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Create saved place", style = MaterialTheme.typography.titleLarge)
        Text("Use a preset for the mock app or enter coordinates manually.")
        CompactAware { compact ->
            ActionButtons(compact) {
                model.presetPlaces.forEach { preset ->
                    OutlinedButton(
                        onClick = hapticClick { model.applyPresetPlace(preset) },
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
                Button(
                    onClick = hapticResultClick(model, onClick = model::savePlace),
                    modifier = responsiveButtonModifier(compact),
                ) {
                    ButtonLabel("Save place")
                }
                if (!model.placeDraft.onboarding) {
                    OutlinedButton(
                        onClick = hapticClick { model.navigate(AppScreen.Home) },
                        modifier = responsiveButtonModifier(compact),
                    ) {
                        ButtonLabel("Cancel")
                    }
                }
            }
        }
        BottomNavigationScrollSpacer()
    }
}

@Composable
internal fun PlaceCoordinateFields(
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
