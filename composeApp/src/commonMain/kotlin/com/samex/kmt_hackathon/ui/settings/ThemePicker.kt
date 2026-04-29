package com.samex.kmt_hackathon.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.LeaveThemeSpec
import com.samex.kmt_hackathon.leaveThemeSpecs
import com.samex.kmt_hackathon.core.AppColorTheme
import com.samex.kmt_hackathon.ui.components.*

@Composable
internal fun ThemePicker(
    selectedTheme: AppColorTheme,
    onThemeSelected: (AppColorTheme) -> Unit,
) {
    val specs = leaveThemeSpecs()
    CompactAware { compact ->
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                specs.chunked(2).forEach { rowSpecs ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowSpecs.forEach { spec ->
                            ThemeChoiceCard(
                                spec = spec,
                                selected = spec.theme == selectedTheme,
                                onClick = { onThemeSelected(spec.theme) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowSpecs.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                specs.forEach { spec ->
                    ThemeChoiceCard(
                        spec = spec,
                        selected = spec.theme == selectedTheme,
                        onClick = { onThemeSelected(spec.theme) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
internal fun ThemeChoiceCard(
    spec: LeaveThemeSpec,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) spec.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = TweenSpec(durationMillis = 220),
        label = "themeChoiceBorder",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) spec.colorScheme.primaryContainer.copy(alpha = 0.58f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        animationSpec = TweenSpec(durationMillis = 220),
        label = "themeChoiceContainer",
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = hapticClick(onClick = onClick)),
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ThemeSwatch(spec = spec, selected = selected)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    spec.label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    spec.description,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun ThemeSwatch(spec: LeaveThemeSpec, selected: Boolean) {
    Box(modifier = Modifier.size(56.dp)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = spec.colorScheme.primaryContainer,
            shape = CircleShape,
            border = BorderStroke(1.dp, spec.colorScheme.primary.copy(alpha = 0.75f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    ThemeSwatchDot(color = spec.colorScheme.primary)
                    ThemeSwatchDot(color = spec.colorScheme.secondary)
                    ThemeSwatchDot(color = spec.colorScheme.tertiary)
                }
            }
        }
        if (selected) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp),
                color = spec.colorScheme.primary,
                contentColor = spec.colorScheme.onPrimary,
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
            ) {
                ThemeCheckMark()
            }
        }
    }
}

@Composable
internal fun ThemeSwatchDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
internal fun ThemeCheckMark() {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(22.dp)) {
        val strokeWidth = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.28f, size.height * 0.52f),
            end = Offset(size.width * 0.43f, size.height * 0.67f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.43f, size.height * 0.67f),
            end = Offset(size.width * 0.74f, size.height * 0.34f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}
