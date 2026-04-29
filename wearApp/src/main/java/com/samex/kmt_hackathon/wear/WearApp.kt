package com.samex.kmt_hackathon.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.TimeText
import com.samex.kmt_hackathon.core.LiveActivitySnapshot

@Composable
internal fun WearApp(syncState: WearSyncState, snapshot: LiveActivitySnapshot?) {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            if (snapshot != null) {
                if (!snapshot.isLeaving) {
                    WaterCountdownBackground(
                        snapshot = snapshot,
                        modifier = Modifier.fillMaxSize(),
                    )
                    FinalCallPulseRing(
                        status = snapshot.status,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                WearActiveWatchScreen(
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                WearEmptyState(syncState = syncState, modifier = Modifier.fillMaxSize())
            }
            TimeText()
        }
    }
}
