package com.samex.kmt_hackathon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun BottomNavigationScrollSpacer() {
    Spacer(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .height(16.dp),
    )
}

@Composable
internal fun CompactAware(
    modifier: Modifier = Modifier.fillMaxWidth(),
    threshold: Dp = 520.dp,
    content: @Composable (compact: Boolean) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        content(maxWidth < threshold)
    }
}

@Composable
internal fun ActionButtons(compact: Boolean, content: @Composable () -> Unit) {
    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

internal fun responsiveButtonModifier(compact: Boolean): Modifier =
    if (compact) Modifier.fillMaxWidth() else Modifier.widthIn(min = 96.dp)
