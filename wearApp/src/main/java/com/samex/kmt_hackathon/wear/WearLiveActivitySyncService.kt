package com.samex.kmt_hackathon.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class WearLiveActivitySyncService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            dataEvents.forEach { event ->
                if (event.dataItem.uri.path != WEAR_LIVE_ACTIVITY_PATH) return@forEach
                if (event.type == DataEvent.TYPE_DELETED) {
                    cancelWearOngoingActivity(applicationContext)
                    return@forEach
                }

                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val snapshot = dataMap.toLiveActivitySnapshot()
                if (!dataMap.isWearLiveActivityActive() || snapshot == null) {
                    cancelWearOngoingActivity(applicationContext)
                    return@forEach
                }

                postWearOngoingActivity(applicationContext, snapshot)
            }
        } finally {
            dataEvents.release()
        }
    }
}
