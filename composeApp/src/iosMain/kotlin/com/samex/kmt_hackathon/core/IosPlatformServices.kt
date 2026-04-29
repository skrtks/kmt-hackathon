package com.samex.kmt_hackathon.core

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSUserDefaults
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

actual object PlatformServices {
    actual fun keyValueStore(): KeyValueStore = IosKeyValueStore

    actual fun notificationScheduler(): NotificationScheduler = IosNotificationScheduler

    actual fun timeProvider(): TimeProvider = IosTimeProvider

    actual fun liveActivityController(): LiveActivityController = IosLiveActivityController

    actual fun hapticFeedback(): HapticFeedbackController = IosHapticFeedbackController

    actual fun isWearDevice(): Boolean = false
}

private object IosKeyValueStore : KeyValueStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? = defaults.stringForKey(key)

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = key)
    }
}

private object IosHapticFeedbackController : HapticFeedbackController {
    override fun perform(effect: HapticEffect) {
        when (effect) {
            HapticEffect.Selection,
            HapticEffect.ProgressTick -> UISelectionFeedbackGenerator().run {
                prepare()
                selectionChanged()
            }
            HapticEffect.Confirmation -> UINotificationFeedbackGenerator().run {
                prepare()
                notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
            }
            HapticEffect.Warning -> UINotificationFeedbackGenerator().run {
                prepare()
                notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
            }
            HapticEffect.Critical,
            HapticEffect.Error -> UINotificationFeedbackGenerator().run {
                prepare()
                notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
            }
        }
    }

    override fun performProgress(progress: Float) {
        val style = when {
            progress >= 0.75f -> UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy
            progress >= 0.40f -> UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium
            else -> UIImpactFeedbackStyle.UIImpactFeedbackStyleLight
        }
        UIImpactFeedbackGenerator(style = style).run {
            prepare()
            impactOccurred()
        }
    }
}

private object IosNotificationScheduler : NotificationScheduler {
    private var cachedStatus: NotificationPermissionStatus = NotificationPermissionStatus.NotDetermined

    override fun permissionStatus(): NotificationPermissionStatus {
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            cachedStatus = when (settings?.authorizationStatus) {
                UNAuthorizationStatusAuthorized -> NotificationPermissionStatus.Granted
                UNAuthorizationStatusDenied -> NotificationPermissionStatus.Denied
                UNAuthorizationStatusNotDetermined -> NotificationPermissionStatus.NotDetermined
                else -> NotificationPermissionStatus.NotDetermined
            }
        }
        return cachedStatus
    }

    override fun requestPermission() {
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
        ) { granted, _ ->
            cachedStatus = if (granted) NotificationPermissionStatus.Granted else NotificationPermissionStatus.Denied
        }
    }

    override fun schedule(plan: NotificationPlan) {
        val content = UNMutableNotificationContent().apply {
            setTitle(plan.title)
            setBody(plan.body)
            setSound(UNNotificationSound.defaultSound())
        }
        val components = NSDateComponents().apply {
            hour = (plan.fireAtMinutes / 60).toLong()
            minute = (plan.fireAtMinutes % 60).toLong()
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = components,
            repeats = false,
        )
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = plan.id,
            content = content,
            trigger = trigger,
        )
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request, withCompletionHandler = null)
    }

    override fun cancel(notificationIds: List<String>) {
        UNUserNotificationCenter.currentNotificationCenter().removePendingNotificationRequestsWithIdentifiers(notificationIds)
    }

    override fun cancelAll() {
        UNUserNotificationCenter.currentNotificationCenter().removeAllPendingNotificationRequests()
    }
}

private object IosTimeProvider : TimeProvider {
    override fun nowMinutesOfDay(): Int {
        val calendar = NSCalendar.currentCalendar
        val now = NSDate()
        val hour = calendar.component(NSCalendarUnitHour, fromDate = now).toInt()
        val minute = calendar.component(NSCalendarUnitMinute, fromDate = now).toInt()
        return hour * 60 + minute
    }

    override fun nowSecondsOfDay(): Int {
        val calendar = NSCalendar.currentCalendar
        val now = NSDate()
        val hour = calendar.component(NSCalendarUnitHour, fromDate = now).toInt()
        val minute = calendar.component(NSCalendarUnitMinute, fromDate = now).toInt()
        val second = calendar.component(NSCalendarUnitSecond, fromDate = now).toInt()
        return (hour * 60 + minute) * 60 + second
    }

    override fun currentWeekday(): Weekday {
        val weekday = NSCalendar.currentCalendar.component(NSCalendarUnitWeekday, fromDate = NSDate()).toInt()
        return when (weekday) {
            2 -> Weekday.Monday
            3 -> Weekday.Tuesday
            4 -> Weekday.Wednesday
            5 -> Weekday.Thursday
            6 -> Weekday.Friday
            7 -> Weekday.Saturday
            else -> Weekday.Sunday
        }
    }
}
