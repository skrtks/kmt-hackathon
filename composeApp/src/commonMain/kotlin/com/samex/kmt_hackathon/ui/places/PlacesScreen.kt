package com.samex.kmt_hackathon.ui.places

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.core.AppScreen
import com.samex.kmt_hackathon.core.HapticEffect
import com.samex.kmt_hackathon.core.TransitAppModel
import com.samex.kmt_hackathon.ui.components.*

@Composable
internal fun PlacesScreen(model: TransitAppModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CompactAware { compact ->
            ActionButtons(compact) {
                Button(
                    onClick = hapticClick { model.beginPlaceEditor() },
                    modifier = responsiveButtonModifier(compact),
                ) {
                    ButtonLabel("Add place")
                }
                OutlinedButton(
                    onClick = hapticClick { model.navigate(AppScreen.Home) },
                    modifier = responsiveButtonModifier(compact),
                ) {
                    ButtonLabel("Back")
                }
            }
        }
        model.userData.places.forEach { place ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = flatCardElevation(),
            ) {
                CompactAware { compact ->
                    if (compact) {
                        Column(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PlaceSummary(place.name, "${place.location.latitude}, ${place.location.longitude}")
                            OutlinedButton(
                                onClick = hapticClick(HapticEffect.Warning) { model.deletePlace(place.id) },
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
                            OutlinedButton(onClick = hapticClick(HapticEffect.Warning) { model.deletePlace(place.id) }) {
                                ButtonLabel("Delete")
                            }
                        }
                    }
                }
            }
        }
        BottomNavigationScrollSpacer()
    }
}

@Composable
internal fun PlaceSummary(name: String, locationText: String) {
    Column {
        Text(name, style = MaterialTheme.typography.titleMedium)
        Text(locationText)
    }
}
