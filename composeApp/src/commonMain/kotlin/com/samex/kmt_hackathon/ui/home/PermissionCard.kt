package com.samex.kmt_hackathon.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.core.NotificationPermissionStatus
import com.samex.kmt_hackathon.core.TransitAppModel
import com.samex.kmt_hackathon.ui.components.*
import com.samex.kmt_hackathon.ui.activewatch.*
import com.samex.kmt_hackathon.ui.presentation.*

@Composable
internal fun PermissionCard(model: TransitAppModel) {
    if (model.notificationStatus == NotificationPermissionStatus.Granted) return
    val denied = model.notificationStatus == NotificationPermissionStatus.Denied
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (denied) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (denied) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        elevation = flatCardElevation(),
    ) {
        CompactAware { compact ->
            val buttonVisible = model.notificationStatus != NotificationPermissionStatus.Unsupported
            val message = if (model.notificationStatus == NotificationPermissionStatus.Unsupported) {
                "Notifications are unsupported on this platform. Foreground watching still works."
            } else {
                "Notifications are ${model.notificationStatus.name.lowercase()}. Foreground watching still works."
            }
            if (compact || !buttonVisible) {
                Column(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(message, style = MaterialTheme.typography.bodyLarge)
                    if (buttonVisible) {
                        Button(onClick = model::requestNotificationPermission, modifier = Modifier.fillMaxWidth()) {
                            ButtonLabel("Enable")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = model::requestNotificationPermission) {
                        ButtonLabel("Enable")
                    }
                }
            }
        }
    }
}
