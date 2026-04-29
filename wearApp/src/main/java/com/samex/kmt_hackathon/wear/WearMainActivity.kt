package com.samex.kmt_hackathon.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.samex.kmt_hackathon.core.LiveActivitySnapshot
import com.samex.kmt_hackathon.core.WatchStatus
import com.samex.kmt_hackathon.core.formatMinutesOfDay

class WearMainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    private var activeSnapshot by mutableStateOf<LiveActivitySnapshot?>(null)
    private var syncState by mutableStateOf(WearSyncState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WearApp(syncState = syncState, snapshot = activeSnapshot)
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
        loadCurrentSnapshot()
    }

    override fun onPause() {
        Wearable.getDataClient(this).removeListener(this)
        super.onPause()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            dataEvents.forEach { event ->
                if (event.dataItem.uri.path != WEAR_LIVE_ACTIVITY_PATH) return@forEach
                if (event.type == DataEvent.TYPE_DELETED) {
                    setNoActiveWatch()
                    return@forEach
                }
                applyDataMap(DataMapItem.fromDataItem(event.dataItem).dataMap)
            }
        } finally {
            dataEvents.release()
        }
    }

    private fun loadCurrentSnapshot() {
        syncState = WearSyncState.Loading
        Wearable.getDataClient(this).dataItems
            .addOnSuccessListener { dataItems ->
                try {
                    val activeItem = dataItems.firstOrNull { it.uri.path == WEAR_LIVE_ACTIVITY_PATH }
                    if (activeItem == null) {
                        setNoActiveWatch()
                    } else {
                        applyDataMap(DataMapItem.fromDataItem(activeItem).dataMap)
                    }
                } finally {
                    dataItems.release()
                }
            }
            .addOnFailureListener {
                runOnUiThread {
                    activeSnapshot = null
                    syncState = WearSyncState.PhoneUnavailable
                }
            }
    }

    private fun applyDataMap(dataMap: DataMap) {
        val snapshot = dataMap.toLiveActivitySnapshot()
        runOnUiThread {
            if (!dataMap.getBoolean(KEY_WEAR_ACTIVE, false) || snapshot == null) {
                activeSnapshot = null
                syncState = WearSyncState.NoActiveWatch
            } else {
                activeSnapshot = snapshot
                syncState = WearSyncState.Active
            }
        }
    }

    private fun setNoActiveWatch() {
        runOnUiThread {
            activeSnapshot = null
            syncState = WearSyncState.NoActiveWatch
        }
    }
}

@Composable
private fun WearApp(syncState: WearSyncState, snapshot: LiveActivitySnapshot?) {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            TimeText()
            if (snapshot != null) {
                WearActiveWatchScreen(
                    snapshot = snapshot,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                WearEmptyState(syncState = syncState, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun WearEmptyState(syncState: WearSyncState, modifier: Modifier = Modifier) {
    val message = when (syncState) {
        WearSyncState.Loading -> "Syncing"
        WearSyncState.NoActiveWatch -> "No active watch"
        WearSyncState.PhoneUnavailable -> "Phone unavailable"
        WearSyncState.Active -> "No active watch"
    }
    val detail = when (syncState) {
        WearSyncState.Loading -> "Checking phone state"
        WearSyncState.NoActiveWatch -> "Start a commute on your phone"
        WearSyncState.PhoneUnavailable -> "Open Leave Window on your phone"
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

@Composable
private fun WearActiveWatchScreen(
    snapshot: LiveActivitySnapshot,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = headlineFor(snapshot),
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
        Text(
            text = "Leave by ${formatMinutesOfDay(snapshot.finalCallMinutes)}",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall,
            color = statusAccent(snapshot.status),
        )
        Text(
            text = "Departure ${formatMinutesOfDay(snapshot.departureTimeMinutes)}",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
        )
        Spacer(Modifier.height(18.dp))
        WatchActionButton("I'm leaving", enabled = false)
        Spacer(Modifier.height(8.dp))
        WatchActionButton("Skip", enabled = false)
        Spacer(Modifier.height(8.dp))
        WatchActionButton("Stop", enabled = false)
    }
}

@Composable
private fun WatchActionButton(label: String, enabled: Boolean) {
    Button(
        onClick = {},
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun headlineFor(snapshot: LiveActivitySnapshot): String =
    when (snapshot.status) {
        WatchStatus.GetReady -> "Leave at ${formatMinutesOfDay(snapshot.windowOpenMinutes)}"
        WatchStatus.LeaveNow -> "Leave now"
        WatchStatus.FinalCall -> "Final call"
        WatchStatus.Missed -> "Next chance"
    }

private fun statusAccent(status: WatchStatus): Color =
    when (status) {
        WatchStatus.GetReady -> Color(0xFF7DD3FC)
        WatchStatus.LeaveNow -> Color(0xFF5EEAD4)
        WatchStatus.FinalCall -> Color(0xFFFCA5A5)
        WatchStatus.Missed -> Color(0xFFFDE68A)
    }

private enum class WearSyncState {
    Loading,
    NoActiveWatch,
    PhoneUnavailable,
    Active,
}

private const val WEAR_LIVE_ACTIVITY_PATH = "/transit-live-activity"
private const val KEY_WEAR_ACTIVE = "active"
private const val KEY_WEAR_COMMUTE_ID = "commute_id"
private const val KEY_WEAR_GROUP_ID = "group_id"
private const val KEY_WEAR_STATUS = "status"
private const val KEY_WEAR_TITLE = "title"
private const val KEY_WEAR_BODY = "body"
private const val KEY_WEAR_STOP_NAME = "stop_name"
private const val KEY_WEAR_LINE_LABEL = "line_label"
private const val KEY_WEAR_DIRECTION_HEADSIGN = "direction_headsign"
private const val KEY_WEAR_DEPARTURE_TIME_MINUTES = "departure_time_minutes"
private const val KEY_WEAR_WINDOW_OPEN_MINUTES = "window_open_minutes"
private const val KEY_WEAR_FINAL_CALL_MINUTES = "final_call_minutes"
private const val KEY_WEAR_WALKING_MINUTES = "walking_minutes"

private fun DataMap.toLiveActivitySnapshot(): LiveActivitySnapshot? {
    val status = getString(KEY_WEAR_STATUS)?.let { value ->
        runCatching { WatchStatus.valueOf(value) }.getOrNull()
    } ?: return null
    return LiveActivitySnapshot(
        commuteId = getString(KEY_WEAR_COMMUTE_ID).orEmpty(),
        groupId = getString(KEY_WEAR_GROUP_ID).orEmpty(),
        status = status,
        title = getString(KEY_WEAR_TITLE).orEmpty(),
        body = getString(KEY_WEAR_BODY).orEmpty(),
        stopName = getString(KEY_WEAR_STOP_NAME).orEmpty(),
        lineLabel = getString(KEY_WEAR_LINE_LABEL).orEmpty(),
        directionHeadsign = getString(KEY_WEAR_DIRECTION_HEADSIGN).orEmpty(),
        departureTimeMinutes = getInt(KEY_WEAR_DEPARTURE_TIME_MINUTES),
        windowOpenMinutes = getInt(KEY_WEAR_WINDOW_OPEN_MINUTES),
        finalCallMinutes = getInt(KEY_WEAR_FINAL_CALL_MINUTES),
        walkingMinutes = getInt(KEY_WEAR_WALKING_MINUTES),
    )
}
