package com.samex.kmt_hackathon.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.core.DemoWatchScenario
import com.samex.kmt_hackathon.core.HapticEffect
import com.samex.kmt_hackathon.ui.components.*

@Composable
internal fun Stepper(onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = hapticClick(onClick = onMinus),
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
        ) {
            StepperMark(isPlus = false)
        }
        Button(
            onClick = hapticClick(onClick = onPlus),
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
        ) {
            StepperMark(isPlus = true)
        }
    }
}

@Composable
internal fun StepperMark(isPlus: Boolean) {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(16.dp)) {
        val inset = 2.dp.toPx()
        val strokeWidth = 2.5.dp.toPx()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        drawLine(
            color = color,
            start = Offset(inset, centerY),
            end = Offset(size.width - inset, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        if (isPlus) {
            drawLine(
                color = color,
                start = Offset(centerX, inset),
                end = Offset(centerX, size.height - inset),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
internal fun SettingStepperRow(
    label: String,
    value: String,
    description: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    CompactAware { compact ->
        val textBlock: @Composable (Modifier) -> Unit = { modifier ->
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val valuePill: @Composable () -> Unit = {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
            ) {
                Text(
                    value,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            textBlock(Modifier.weight(1f))
                            Spacer(Modifier.width(10.dp))
                            valuePill()
                        }
                        Stepper(onMinus = onMinus, onPlus = onPlus)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        textBlock(Modifier.weight(1f))
                        Spacer(Modifier.width(12.dp))
                        valuePill()
                        Spacer(Modifier.width(12.dp))
                        Stepper(onMinus = onMinus, onPlus = onPlus)
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = { enabled ->
                    hapticClick {
                        onCheckedChange(enabled)
                    }()
                },
            )
        }
    }
}

@Composable
internal fun DemoScenarioControls(
    enabled: Boolean,
    onScenarioSelected: (DemoWatchScenario) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Demo state",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            CompactAware { compact ->
                ActionButtons(compact) {
                    OutlinedButton(
                        onClick = hapticClick { onScenarioSelected(DemoWatchScenario.GetReady) },
                        enabled = enabled,
                        modifier = responsiveButtonModifier(compact),
                    ) {
                        ButtonLabel("Get ready")
                    }
                    OutlinedButton(
                        onClick = hapticClick { onScenarioSelected(DemoWatchScenario.LeaveNow) },
                        enabled = enabled,
                        modifier = responsiveButtonModifier(compact),
                    ) {
                        ButtonLabel("Leave now")
                    }
                    Button(
                        onClick = hapticClick(HapticEffect.Critical) { onScenarioSelected(DemoWatchScenario.FinalCall) },
                        enabled = enabled,
                        modifier = responsiveButtonModifier(compact),
                    ) {
                        ButtonLabel("Final call")
                    }
                }
            }
        }
    }
}
