package com.samex.kmt_hackathon.core

interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

enum class NotificationPermissionStatus {
    Granted,
    Denied,
    NotDetermined,
    Unsupported,
}

interface NotificationScheduler {
    fun permissionStatus(): NotificationPermissionStatus
    fun requestPermission()
    fun schedule(plan: NotificationPlan)
    fun cancel(notificationIds: List<String>)
    fun cancelAll()
}

interface TimeProvider {
    fun nowMinutesOfDay(): Int
    fun nowSecondsOfDay(): Int
    fun currentWeekday(): Weekday
}

interface LiveActivityController {
    fun isSupported(): Boolean
    fun isActivityRunning(): Boolean
    fun start(snapshot: LiveActivitySnapshot)
    fun update(snapshot: LiveActivitySnapshot)
    fun end(snapshot: LiveActivitySnapshot?, reason: LiveActivityEndReason)
}

enum class HapticEffect {
    Selection,
    ProgressTick,
    Confirmation,
    Warning,
    Critical,
    Error,
}

interface HapticFeedbackController {
    fun perform(effect: HapticEffect)
    fun performProgress(progress: Float) = perform(HapticEffect.ProgressTick)
}

object NoopLiveActivityController : LiveActivityController {
    override fun isSupported(): Boolean = false
    override fun isActivityRunning(): Boolean = false
    override fun start(snapshot: LiveActivitySnapshot) = Unit
    override fun update(snapshot: LiveActivitySnapshot) = Unit
    override fun end(snapshot: LiveActivitySnapshot?, reason: LiveActivityEndReason) = Unit
}

object NoopHapticFeedbackController : HapticFeedbackController {
    override fun perform(effect: HapticEffect) = Unit
}
