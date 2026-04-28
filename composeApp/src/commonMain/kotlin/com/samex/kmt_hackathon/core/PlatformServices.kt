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
    fun currentWeekday(): Weekday
}

expect object PlatformServices {
    fun keyValueStore(): KeyValueStore
    fun notificationScheduler(): NotificationScheduler
    fun timeProvider(): TimeProvider
}
