package com.samex.kmt_hackathon.core

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class WearLiveActivitySyncService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            dataEvents.forEach { event ->
                if (event.dataItem.uri.path != WEAR_LIVE_ACTIVITY_PATH) return@forEach
                val controller = AndroidLiveActivityController(applicationContext)
                if (event.type == DataEvent.TYPE_DELETED) {
                    controller.end(snapshot = null, reason = LiveActivityEndReason.SessionEnded)
                    return@forEach
                }

                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                if (!isWearLiveActivityActive(dataMap)) {
                    controller.end(snapshot = null, reason = LiveActivityEndReason.SessionEnded)
                    return@forEach
                }

                dataMap.toLiveActivitySnapshot()?.let(controller::update)
            }
        } finally {
            dataEvents.release()
        }
    }
}
