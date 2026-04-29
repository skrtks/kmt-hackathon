package com.samex.kmt_hackathon.core

internal class LiveActivitySyncCoordinator(
    private val controller: LiveActivityController,
    private val currentSnapshot: () -> LiveActivitySnapshot?,
    private val clockSecondsOfDay: () -> Int,
) {
    private var lastSnapshot: LiveActivitySnapshot? = null
    private var pendingEndReason: LiveActivityEndReason? = null
    private var forceClockSync: Boolean = false

    fun requestClockSync() {
        forceClockSync = true
    }

    fun start(snapshot: LiveActivitySnapshot) {
        if (!controller.isSupported()) return
        val syncedSnapshot = snapshot.withCurrentClock()
        if (controller.start(syncedSnapshot)) {
            lastSnapshot = syncedSnapshot
            pendingEndReason = null
        }
    }

    fun sync(
        skippedFallbackReason: LiveActivityEndReason? = null,
        forceClockSync: Boolean = false,
    ) {
        if (!controller.isSupported()) return
        val snapshot = currentSnapshot()
        val shouldForceClockSync = forceClockSync || this.forceClockSync
        val pendingReason = pendingEndReason
        when {
            snapshot == null && (lastSnapshot != null || controller.isActivityRunning()) -> {
                val reason = pendingReason ?: skippedFallbackReason ?: LiveActivityEndReason.SessionEnded
                if (controller.end(
                    snapshot = lastSnapshot,
                    reason = reason,
                )) {
                    lastSnapshot = null
                    pendingEndReason = null
                } else {
                    pendingEndReason = reason
                }
            }
            snapshot != null && (lastSnapshot == null || !controller.isActivityRunning()) -> {
                val syncedSnapshot = snapshot.withCurrentClock()
                if (controller.start(syncedSnapshot)) {
                    lastSnapshot = syncedSnapshot
                    pendingEndReason = null
                }
            }
            snapshot != null && (shouldForceClockSync || !snapshot.sameLiveContentAs(lastSnapshot)) -> {
                val syncedSnapshot = snapshot.withCurrentClock()
                if (controller.update(syncedSnapshot)) {
                    lastSnapshot = syncedSnapshot
                    pendingEndReason = null
                }
            }
        }
        this.forceClockSync = false
    }

    fun end(
        reason: LiveActivityEndReason,
        snapshotOverride: LiveActivitySnapshot? = null,
    ) {
        if (!controller.isSupported()) return
        val snapshot = snapshotOverride?.withCurrentClock() ?: lastSnapshot
        if (snapshot == null && !controller.isActivityRunning()) return
        if (controller.end(snapshot = snapshot, reason = reason)) {
            lastSnapshot = if (reason == LiveActivityEndReason.Leaving) snapshot else null
            pendingEndReason = null
        } else {
            pendingEndReason = reason
        }
    }

    fun reconcileOnLoad(restored: Boolean) {
        if (!controller.isSupported()) return
        if (restored) {
            val snapshot = currentSnapshot()
            if (snapshot != null) {
                val syncedSnapshot = snapshot.withCurrentClock()
                val synced = if (controller.isActivityRunning()) {
                    controller.update(syncedSnapshot)
                } else {
                    controller.start(syncedSnapshot)
                }
                if (synced) {
                    lastSnapshot = syncedSnapshot
                    pendingEndReason = null
                }
            } else if (controller.isActivityRunning()) {
                if (!controller.end(snapshot = null, reason = LiveActivityEndReason.SessionEnded)) {
                    pendingEndReason = LiveActivityEndReason.SessionEnded
                }
            }
        } else if (controller.isActivityRunning()) {
            if (!controller.end(snapshot = null, reason = LiveActivityEndReason.SessionEnded)) {
                pendingEndReason = LiveActivityEndReason.SessionEnded
            }
        }
    }

    private fun LiveActivitySnapshot.withCurrentClock(): LiveActivitySnapshot =
        copy(syncedNowSecondsOfDay = clockSecondsOfDay())

    private fun LiveActivitySnapshot.sameLiveContentAs(other: LiveActivitySnapshot?): Boolean =
        copy(syncedNowSecondsOfDay = null) == other?.copy(syncedNowSecondsOfDay = null)
}
