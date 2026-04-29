package com.samex.kmt_hackathon.wear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

@Composable
internal fun WearEmptyState(syncState: WearSyncState, modifier: Modifier = Modifier) {
    val message = when (syncState) {
        WearSyncState.Loading -> "Syncing"
        WearSyncState.NoActiveWatch -> "No active watch"
        WearSyncState.PhoneUnavailable -> "Phone unavailable"
        WearSyncState.Active -> "No active watch"
    }
    val detail = when (syncState) {
        WearSyncState.Loading -> "Checking phone state"
        WearSyncState.NoActiveWatch -> "Start a commute on your phone"
        WearSyncState.PhoneUnavailable -> "Open Leave on your phone"
        WearSyncState.Active -> "Start a commute on your phone"
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = detail,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFCBD5E1),
        )
    }
}

internal enum class WearSyncState {
    Loading,
    NoActiveWatch,
    PhoneUnavailable,
    Active,
}
