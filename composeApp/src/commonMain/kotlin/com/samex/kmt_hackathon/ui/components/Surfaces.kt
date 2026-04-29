package com.samex.kmt_hackathon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.core.HapticEffect

@Composable
internal fun flatCardElevation() = CardDefaults.cardElevation(
    defaultElevation = 0.dp,
    pressedElevation = 0.dp,
    focusedElevation = 0.dp,
    hoveredElevation = 0.dp,
    draggedElevation = 0.dp,
    disabledElevation = 0.dp,
)
@Composable
internal fun ReplacementPrompt(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = flatCardElevation(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Another watch session is active.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            CompactAware { compact ->
                ActionButtons(compact) {
                    Button(
                        onClick = hapticClick(HapticEffect.Warning, onConfirm),
                        modifier = responsiveButtonModifier(compact),
                    ) {
                        ButtonLabel("Replace")
                    }
                    OutlinedButton(
                        onClick = hapticClick(onClick = onCancel),
                        modifier = responsiveButtonModifier(compact),
                    ) {
                        ButtonLabel("Keep current")
                    }
                }
            }
        }
    }
}

@Composable
internal fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        elevation = flatCardElevation(),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
internal fun EmptyCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = flatCardElevation(),
    ) {
        Text(
            message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
