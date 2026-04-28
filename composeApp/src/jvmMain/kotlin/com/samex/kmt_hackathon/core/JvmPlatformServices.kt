package com.samex.kmt_hackathon.core

import java.time.DayOfWeek
import java.time.LocalTime
import java.util.prefs.Preferences

actual object PlatformServices {
    actual fun keyValueStore(): KeyValueStore = JvmKeyValueStore

    actual fun notificationScheduler(): NotificationScheduler = JvmNotificationScheduler

    actual fun timeProvider(): TimeProvider = JvmTimeProvider
}

private object JvmKeyValueStore : KeyValueStore {
    private val preferences = Preferences.userRoot().node("com/samex/kmt_hackathon")

    override fun getString(key: String): String? = preferences.get(key, null)

    override fun putString(key: String, value: String) {
        preferences.put(key, value)
    }
}

private object JvmNotificationScheduler : NotificationScheduler {
    private val scheduledIds = mutableSetOf<String>()

    override fun permissionStatus(): NotificationPermissionStatus = NotificationPermissionStatus.Unsupported

    override fun requestPermission() {
        // Desktop notifications are intentionally fake for this first version.
    }

    override fun schedule(plan: NotificationPlan) {
        scheduledIds += plan.id
        println("Scheduled ${plan.kind} notification at ${formatMinutesOfDay(plan.fireAtMinutes)}: ${plan.title} - ${plan.body}")
    }

    override fun cancel(notificationIds: List<String>) {
        scheduledIds -= notificationIds.toSet()
    }

    override fun cancelAll() {
        scheduledIds.clear()
    }
}

private object JvmTimeProvider : TimeProvider {
    override fun nowMinutesOfDay(): Int {
        val now = LocalTime.now()
        return now.hour * 60 + now.minute
    }

    override fun nowSecondsOfDay(): Int {
        val now = LocalTime.now()
        return (now.hour * 60 + now.minute) * 60 + now.second
    }

    override fun currentWeekday(): Weekday =
        when (java.time.LocalDate.now().dayOfWeek) {
            DayOfWeek.MONDAY -> Weekday.Monday
            DayOfWeek.TUESDAY -> Weekday.Tuesday
            DayOfWeek.WEDNESDAY -> Weekday.Wednesday
            DayOfWeek.THURSDAY -> Weekday.Thursday
            DayOfWeek.FRIDAY -> Weekday.Friday
            DayOfWeek.SATURDAY -> Weekday.Saturday
            DayOfWeek.SUNDAY -> Weekday.Sunday
        }
}
