package com.samex.kmt_hackathon.core

interface LiveActivityBridge {
    fun isSupported(): Boolean
    fun isActivityRunning(): Boolean
    fun start(snapshot: LiveActivitySnapshot)
    fun update(snapshot: LiveActivitySnapshot)
    fun end(snapshot: LiveActivitySnapshot?, reason: LiveActivityEndReason)
}

object IosLiveActivityController : LiveActivityController {
    private var bridge: LiveActivityBridge? = null

    fun register(bridge: LiveActivityBridge) {
        this.bridge = bridge
    }

    override fun isSupported(): Boolean = bridge?.isSupported() ?: false

    override fun isActivityRunning(): Boolean = bridge?.isActivityRunning() ?: false

    override fun start(snapshot: LiveActivitySnapshot) {
        bridge?.start(snapshot)
    }

    override fun update(snapshot: LiveActivitySnapshot) {
        bridge?.update(snapshot)
    }

    override fun end(snapshot: LiveActivitySnapshot?, reason: LiveActivityEndReason) {
        bridge?.end(snapshot, reason)
    }

    fun isWatchStopped(reason: LiveActivityEndReason): Boolean =
        reason == LiveActivityEndReason.WatchStopped

    fun statusKey(snapshot: LiveActivitySnapshot): String = snapshot.status.name
}
