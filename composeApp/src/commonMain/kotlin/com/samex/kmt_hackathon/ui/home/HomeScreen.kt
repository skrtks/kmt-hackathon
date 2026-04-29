package com.samex.kmt_hackathon.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.core.AppScreen
import com.samex.kmt_hackathon.core.HapticEffect
import com.samex.kmt_hackathon.core.TransitAppModel
import com.samex.kmt_hackathon.ui.components.*
import com.samex.kmt_hackathon.ui.activewatch.*
import com.samex.kmt_hackathon.ui.presentation.*

@Composable
internal fun HomeScreen(model: TransitAppModel) {
    CompactAware(modifier = Modifier.fillMaxSize()) { compact ->
        val activeState = model.watchUiState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PermissionCard(model)
            activeState?.let { state ->
                ActiveWatchSection(model = model, state = state, compact = compact)
            }

            if (model.userData.commutes.isEmpty()) {
                EmptyCard("No saved commutes yet. Create one from a saved place and mock stop.")
                HomeManagementActions(model = model, compact = compact)
            } else {
                Text("Saved commutes", style = MaterialTheme.typography.titleMedium)
                model.userData.commutes.forEach { commute ->
                    CommuteSummaryCard(
                        model = model,
                        commute = commute,
                        compact = compact,
                        isActive = activeState?.commute?.id == commute.id,
                        onAutoStartChange = { model.toggleCommuteAutoStart(commute.id) },
                        onDelete = { model.deleteCommute(commute.id) },
                        onEdit = { model.beginCommuteEdit(commute.id) },
                        onStart = { model.startWatch(commute.id) },
                    )
                }
                HomeManagementActions(model = model, compact = compact)
            }
            BottomNavigationScrollSpacer()
        }
    }
}

@Composable
internal fun HomeManagementActions(model: TransitAppModel, compact: Boolean) {
    ActionButtons(compact) {
        Button(
            onClick = hapticClick { model.beginCommuteSetup() },
            enabled = model.userData.places.isNotEmpty(),
            modifier = responsiveButtonModifier(compact),
        ) {
            ButtonLabel("Add commute")
        }
        OutlinedButton(
            onClick = hapticClick { model.beginPlaceEditor() },
            modifier = responsiveButtonModifier(compact),
        ) {
            ButtonLabel("Add place")
        }
        OutlinedButton(
            onClick = hapticClick { model.navigate(AppScreen.Places) },
            modifier = responsiveButtonModifier(compact),
        ) {
            ButtonLabel("Places")
        }
        OutlinedButton(
            onClick = hapticClick(HapticEffect.Confirmation, model::startDemoLeavingWindow),
            enabled = model.userData.commutes.isNotEmpty(),
            modifier = responsiveButtonModifier(compact),
        ) {
            ButtonLabel("Demo")
        }
    }
}
