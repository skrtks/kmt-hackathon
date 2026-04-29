package com.samex.kmt_hackathon.core

expect object PlatformServices {
    fun keyValueStore(): KeyValueStore
    fun notificationScheduler(): NotificationScheduler
    fun timeProvider(): TimeProvider
    fun liveActivityController(): LiveActivityController
    fun hapticFeedback(): HapticFeedbackController
    fun isWearDevice(): Boolean
}
