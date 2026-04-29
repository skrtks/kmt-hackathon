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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.samex.kmt_hackathon.core.LiveActivitySnapshot
import com.samex.kmt_hackathon.core.WatchStatus
import com.samex.kmt_hackathon.core.activeWatchPresentation
import com.samex.kmt_hackathon.core.formatMinutesOfDay

@Composable
internal fun WearActiveWatchScreen(
    snapshot: LiveActivitySnapshot,
    modifier: Modifier = Modifier,
) {
    var nowSecondsOfDay by rememberCurrentSecondsOfDay(snapshot)
    val presentation = snapshot.activeWatchPresentation(nowSecondsOfDay)
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = presentation.headline,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${snapshot.lineLabel} to ${snapshot.directionHeadsign}",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFE2E8F0),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = snapshot.stopName,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFCBD5E1),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(14.dp))
        if (!snapshot.isLeaving && snapshot.status != WatchStatus.FinalCall) {
            Text(
                text = "Leave by ${formatMinutesOfDay(snapshot.finalCallMinutes)}",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
                color = statusAccent(snapshot.status),
            )
        }
        Text(
            text = "Departure ${formatMinutesOfDay(snapshot.departureTimeMinutes)}",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
        )
    }
}

internal fun statusAccent(status: WatchStatus): Color =
    when (status) {
        WatchStatus.GetReady -> Color(0xFF7DD3FC)
        WatchStatus.LeaveNow -> Color(0xFF5EEAD4)
        WatchStatus.FinalCall -> Color(0xFFFCA5A5)
        WatchStatus.Missed -> Color(0xFFFDE68A)
    }
