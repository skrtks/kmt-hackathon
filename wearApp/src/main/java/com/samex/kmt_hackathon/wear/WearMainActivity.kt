package com.samex.kmt_hackathon.wear

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.samex.kmt_hackathon.core.LiveActivitySnapshot

class WearMainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                activeSnapshot?.let { postWearOngoingActivity(this, it) }
            }
        }

    private var activeSnapshot by mutableStateOf<LiveActivitySnapshot?>(null)
    private var syncState by mutableStateOf(WearSyncState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (shouldRequestWearOngoingActivityPermission(this)) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

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
                    cancelWearOngoingActivity(this@WearMainActivity)
                }
            }
    }

    private fun applyDataMap(dataMap: DataMap) {
        val snapshot = dataMap.toLiveActivitySnapshot()
        runOnUiThread {
            if (!dataMap.isWearLiveActivityActive() || snapshot == null) {
                activeSnapshot = null
                syncState = WearSyncState.NoActiveWatch
                cancelWearOngoingActivity(this@WearMainActivity)
            } else {
                activeSnapshot = snapshot
                syncState = WearSyncState.Active
                postWearOngoingActivity(this@WearMainActivity, snapshot)
            }
        }
    }

    private fun setNoActiveWatch() {
        runOnUiThread {
            activeSnapshot = null
            syncState = WearSyncState.NoActiveWatch
            cancelWearOngoingActivity(this@WearMainActivity)
        }
    }
}
