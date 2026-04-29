package com.samex.kmt_hackathon.ui.activewatch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.core.LeaveWindowGroup
import com.samex.kmt_hackathon.core.formatMinutesOfDay
import com.samex.kmt_hackathon.ui.components.*
import com.samex.kmt_hackathon.ui.presentation.*

@Composable
internal fun RouteChipColumn(labels: List<String>, compact: Boolean, maxItems: Int) {
    val visibleCount = if (compact) maxItems.coerceAtMost(3) else maxItems
    val visibleLabels = labels.take(visibleCount)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        visibleLabels.forEachIndexed { index, label ->
            RouteChip(label = label, accent = routeAccentColor(index))
        }
        if (labels.size > visibleCount) {
            RouteChip(label = "+${labels.size - visibleCount} more", accent = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
internal fun RouteChip(label: String, accent: Color) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun routeAccentColor(index: Int): Color =
    when (index.mod(4)) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.secondary
        2 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

internal fun groupRouteLabels(group: com.samex.kmt_hackathon.core.LeaveWindowGroup): List<String> =
    group.windows.map { window ->
        "${window.lineShortName} to ${window.headsign} ${formatMinutesOfDay(window.departureTimeMinutes)}"
    }
