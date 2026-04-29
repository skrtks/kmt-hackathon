package com.samex.kmt_hackathon.ui.components

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun ButtonLabel(text: String) {
    Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun NavButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.widthIn(min = 72.dp)) {
        ButtonLabel(text)
    }
}
