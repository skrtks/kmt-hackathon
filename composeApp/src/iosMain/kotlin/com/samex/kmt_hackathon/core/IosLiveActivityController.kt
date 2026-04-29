package com.samex.kmt_hackathon.core

interface LiveActivityBridge {
    fun isSupported(): Boolean
    fun isActivityRunning(): Boolean
    fun start(snapshot: LiveActivitySnapshot): Boolean
    fun update(snapshot: LiveActivitySnapshot): Boolean
    fun end(snapshot: LiveActivitySnapshot?, reason: LiveActivityEndReason): Boolean
}

object IosLiveActivityController : LiveActivityController {
    private var bridge: LiveActivityBridge? = null

    fun register(bridge: LiveActivityBridge) {
        this.bridge = bridge
    }

    override fun isSupported(): Boolean = bridge?.isSupported() ?: false

    override fun isActivityRunning(): Boolean = bridge?.isActivityRunning() ?: false

    override fun start(snapshot: LiveActivitySnapshot): Boolean =
        bridge?.start(snapshot) ?: false

    override fun update(snapshot: LiveActivitySnapshot): Boolean =
        bridge?.update(snapshot) ?: false

    override fun end(snapshot: LiveActivitySnapshot?, reason: LiveActivityEndReason): Boolean =
        bridge?.end(snapshot, reason) ?: false

    fun isWatchStopped(reason: LiveActivityEndReason): Boolean =
        reason == LiveActivityEndReason.WatchStopped

    fun statusKey(snapshot: LiveActivitySnapshot): String = snapshot.status.name

    fun endStatusKey(reason: LiveActivityEndReason): String =
        if (isWatchStopped(reason)) "WatchStopped" else "Ended"

    fun endTitle(reason: LiveActivityEndReason, fallback: String): String =
        if (isWatchStopped(reason)) WatchCopy.WATCH_STOPPED_TITLE else fallback

    fun endBody(reason: LiveActivityEndReason, fallback: String): String =
        if (isWatchStopped(reason)) WatchCopy.WATCH_STOPPED_BODY else fallback
}
