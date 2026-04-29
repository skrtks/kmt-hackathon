package com.samex.kmt_hackathon.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.core.TransitAppModel
import com.samex.kmt_hackathon.ui.components.*

@Composable
internal fun SettingsScreen(model: TransitAppModel, modifier: Modifier = Modifier) {
    val settings = model.userData.settings
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SettingsOverviewCard(
            earlyWindow = "${settings.defaultArrivalBuffer.minEarlyMinutes}-${settings.defaultArrivalBuffer.maxEarlyMinutes} min",
            walkingSpeed = "${settings.walkingSpeed.metersPerMinute.toInt()} m/min",
            colorTheme = settings.colorTheme,
            notificationStatus = model.notificationStatus,
        )

        SettingsPanel(
            title = "Arrival window",
            subtitle = "The leave window is calculated from how early you want to reach the stop.",
        ) {
            SettingStepperRow(
                label = "Minimum early",
                value = "${settings.defaultArrivalBuffer.minEarlyMinutes} min",
                description = "Closest acceptable arrival before departure.",
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
                label = "Maximum early",
                value = "${settings.defaultArrivalBuffer.maxEarlyMinutes} min",
                description = "Earliest acceptable arrival before departure.",
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
        SettingsPanel(
            title = "Walking pace",
            subtitle = "Used to estimate how long it takes to reach a stop from a saved place.",
        ) {
            SettingStepperRow(
                label = "Default pace",
                value = "${settings.walkingSpeed.metersPerMinute.toInt()} m/min",
                description = "Adjust in 5 meter/minute steps.",
                onMinus = { model.updateWalkingSpeed((settings.walkingSpeed.metersPerMinute - 5).coerceAtLeast(30.0)) },
                onPlus = { model.updateWalkingSpeed(settings.walkingSpeed.metersPerMinute + 5) },
            )
        }
        NotificationSettingsPanel(
            status = model.notificationStatus,
            onRequestPermission = model::requestNotificationPermission,
        )
        SettingsPanel(
            title = "Theme",
            subtitle = "Choose a color mood for the app.",
        ) {
            ThemePicker(
                selectedTheme = settings.colorTheme,
                onThemeSelected = model::updateColorTheme,
            )
        }
        SettingsPanel(
            title = "Debug",
            subtitle = "Testing controls for active watch timing.",
        ) {
            SettingSwitchRow(
                label = "Debug mode",
                description = "Tap the active watch headline to jump to 5 seconds before the next transition.",
                checked = settings.debugModeEnabled,
                onCheckedChange = model::setDebugModeEnabled,
            )
            DemoScenarioControls(
                enabled = settings.debugModeEnabled && model.userData.commutes.isNotEmpty(),
                onScenarioSelected = model::startDemoWatch,
            )
        }
        BottomNavigationScrollSpacer()
    }
}
