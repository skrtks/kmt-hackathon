package com.samex.kmt_hackathon.ui.commute

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.core.AppScreen
import com.samex.kmt_hackathon.core.CommuteDraft
import com.samex.kmt_hackathon.core.TransitAppModel
import com.samex.kmt_hackathon.ui.components.*

internal enum class CommuteEditSection {
    Origin,
    Stop,
    Lines,
    Arrival,
    Schedule,
}

@Composable
internal fun CommuteEditScreen(model: TransitAppModel) {
    val draft = model.commuteDraft
    var openSection by remember(draft.editingCommuteId) { mutableStateOf<CommuteEditSection?>(null) }

    CompactAware(modifier = Modifier.fillMaxSize()) { compact ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Edit commute", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            CommuteEditSummary(model, draft)

            CommuteEditSectionCard(
                title = "Origin",
                value = originName(model, draft),
                expanded = openSection == CommuteEditSection.Origin,
                onToggle = {
                    openSection = openSection.toggle(CommuteEditSection.Origin)
                },
                compact = compact,
            ) {
                OriginStep(model, draft)
            }

            CommuteEditSectionCard(
                title = "Stop",
                value = stopName(model, draft),
                expanded = openSection == CommuteEditSection.Stop,
                onToggle = {
                    openSection = openSection.toggle(CommuteEditSection.Stop)
                },
                compact = compact,
            ) {
                StopStep(model, draft, compact)
            }

            CommuteEditSectionCard(
                title = "Line",
                value = linesSummary(model, draft),
                expanded = openSection == CommuteEditSection.Lines,
                onToggle = {
                    openSection = openSection.toggle(CommuteEditSection.Lines)
                },
                compact = compact,
            ) {
                LinesStep(model, draft, compact)
            }

            CommuteEditSectionCard(
                title = "Leave timing",
                value = arrivalBufferSummary(model, draft),
                expanded = openSection == CommuteEditSection.Arrival,
                onToggle = {
                    openSection = openSection.toggle(CommuteEditSection.Arrival)
                },
                compact = compact,
            ) {
                ArrivalBufferEditor(model, draft, compact)
            }

            CommuteEditSectionCard(
                title = "Auto-start",
                value = scheduleSummary(draft),
                expanded = openSection == CommuteEditSection.Schedule,
                onToggle = {
                    openSection = openSection.toggle(CommuteEditSection.Schedule)
                },
                compact = compact,
            ) {
                ScheduleEditor(model, draft, compact)
            }

            ActionButtons(compact) {
                OutlinedButton(
                    onClick = hapticClick { model.navigate(AppScreen.Home) },
                    modifier = responsiveButtonModifier(compact),
                ) {
                    ButtonLabel("Cancel")
                }
                Button(
                    onClick = hapticResultClick(model, onClick = model::saveCommute),
                    enabled = canSaveCommuteDraft(draft),
                    modifier = responsiveButtonModifier(compact),
                ) {
                    ButtonLabel("Save changes")
                }
            }
            BottomNavigationScrollSpacer()
        }
    }
}

internal fun CommuteEditSection?.toggle(section: CommuteEditSection): CommuteEditSection? =
    if (this == section) null else section
@Composable
internal fun CommuteEditSummary(model: TransitAppModel, draft: CommuteDraft) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "${originName(model, draft)} to ${stopName(model, draft)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                linesSummary(model, draft),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${arrivalBufferSummary(model, draft)} - ${scheduleSummary(draft)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
internal fun CommuteEditSectionCard(
    title: String,
    value: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    compact: Boolean,
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
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CommuteEditSectionText(title = title, value = value)
                    OutlinedButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                        ButtonLabel(if (expanded) "Done" else "Change")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CommuteEditSectionText(
                        title = title,
                        value = value,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = onToggle, modifier = Modifier.widthIn(min = 112.dp)) {
                        ButtonLabel(if (expanded) "Done" else "Change")
                    }
                }
            }

            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                content()
            }
        }
    }
}

@Composable
internal fun CommuteEditSectionText(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
