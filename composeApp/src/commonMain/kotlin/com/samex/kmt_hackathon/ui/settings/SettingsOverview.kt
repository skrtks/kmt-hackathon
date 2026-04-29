package com.samex.kmt_hackathon.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.leaveThemeSpec
import com.samex.kmt_hackathon.core.AppColorTheme
import com.samex.kmt_hackathon.core.NotificationPermissionStatus
import com.samex.kmt_hackathon.ui.components.*

@Composable
internal fun SettingsOverviewCard(
    earlyWindow: String,
    walkingSpeed: String,
    colorTheme: AppColorTheme,
    notificationStatus: NotificationPermissionStatus,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
        elevation = flatCardElevation(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
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
                        "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Tune the timing defaults behind every leave window.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                ) {
                    Text(
                        "Defaults",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            CompactAware { compact ->
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsMetricPill("Window", earlyWindow)
                        SettingsMetricPill("Walk", walkingSpeed)
                        SettingsMetricPill("Theme", leaveThemeSpec(colorTheme).label)
                        SettingsMetricPill("Alerts", notificationStatus.name)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsMetricPill("Window", earlyWindow, modifier = Modifier.weight(1f))
                        SettingsMetricPill("Walk", walkingSpeed, modifier = Modifier.weight(1f))
                        SettingsMetricPill("Theme", leaveThemeSpec(colorTheme).label, modifier = Modifier.weight(1f))
                        SettingsMetricPill("Alerts", notificationStatus.name, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsMetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
