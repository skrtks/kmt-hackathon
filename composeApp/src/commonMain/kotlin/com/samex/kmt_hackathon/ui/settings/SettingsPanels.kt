package com.samex.kmt_hackathon.ui.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.core.NotificationPermissionStatus
import com.samex.kmt_hackathon.ui.components.*

@Composable
internal fun SettingsPanel(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = 520f, dampingRatio = 0.86f)),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
internal fun NotificationSettingsPanel(
    status: NotificationPermissionStatus,
    onRequestPermission: () -> Unit,
) {
    SettingsPanel(
        title = "Notifications",
        subtitle = "Alerts are only used for window open and final call timing.",
    ) {
        CompactAware { compact ->
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NotificationStatusSurface(status)
                    Button(
                        onClick = onRequestPermission,
                        enabled = status != NotificationPermissionStatus.Unsupported,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ButtonLabel(if (status == NotificationPermissionStatus.Granted) "Refresh status" else "Enable alerts")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        NotificationStatusSurface(status)
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = onRequestPermission,
                        enabled = status != NotificationPermissionStatus.Unsupported,
                    ) {
                        ButtonLabel(if (status == NotificationPermissionStatus.Granted) "Refresh status" else "Enable alerts")
                    }
                }
            }
        }
    }
}

@Composable
internal fun NotificationStatusSurface(status: NotificationPermissionStatus) {
    val denied = status == NotificationPermissionStatus.Denied
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (denied) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (denied) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            1.dp,
            if (denied) MaterialTheme.colorScheme.error.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text("Current status", style = MaterialTheme.typography.labelMedium)
            Text(
                status.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
