package com.samex.kmt_hackathon.core

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.samex.kmt_hackathon.MainActivity
import com.samex.kmt_hackathon.R
import java.util.Calendar

actual object PlatformServices {
    private lateinit var applicationContext: Context
    private var activity: ComponentActivity? = null

    fun initialize(activity: ComponentActivity) {
        this.activity = activity
        applicationContext = activity.applicationContext
    }

    actual fun keyValueStore(): KeyValueStore = AndroidKeyValueStore(requireContext())

    actual fun notificationScheduler(): NotificationScheduler =
        AndroidNotificationScheduler(requireContext()) { activity }

    actual fun timeProvider(): TimeProvider = AndroidTimeProvider

    actual fun liveActivityController(): LiveActivityController = AndroidLiveActivityController(requireContext())

    private fun requireContext(): Context {
        check(::applicationContext.isInitialized) {
            "PlatformServices.initialize(activity) must be called before App()"
        }
        return applicationContext
    }
}

private class AndroidKeyValueStore(context: Context) : KeyValueStore {
    private val preferences = context.getSharedPreferences("leave_window_store", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }
}

private object AndroidTimeProvider : TimeProvider {
    override fun nowMinutesOfDay(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    override fun nowSecondsOfDay(): Int {
        val calendar = Calendar.getInstance()
        return (calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)) * 60 +
                calendar.get(Calendar.SECOND)
    }

    override fun currentWeekday(): Weekday {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> Weekday.Monday
            Calendar.TUESDAY -> Weekday.Tuesday
            Calendar.WEDNESDAY -> Weekday.Wednesday
            Calendar.THURSDAY -> Weekday.Thursday
            Calendar.FRIDAY -> Weekday.Friday
            Calendar.SATURDAY -> Weekday.Saturday
            else -> Weekday.Sunday
        }
    }
}

private class AndroidNotificationScheduler(
    private val context: Context,
    private val activityProvider: () -> ComponentActivity?,
) : NotificationScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val preferences = context.getSharedPreferences(NOTIFICATION_PREFERENCES, Context.MODE_PRIVATE)

    override fun permissionStatus(): NotificationPermissionStatus =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationPermissionStatus.Granted
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationPermissionStatus.Granted
        } else {
            NotificationPermissionStatus.NotDetermined
        }

    override fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            activityProvider()?.let { activity ->
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1801)
            }
            return
        }
        requestExactAlarmPermissionIfNeeded()
    }

    override fun schedule(plan: NotificationPlan) {
        createChannel(context)
        clearDeliveredNotificationId(context, plan.id)
        saveScheduledIds(loadScheduledIds() + plan.id)
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_ID, plan.id)
            putExtra(NotificationReceiver.EXTRA_TITLE, plan.title)
            putExtra(NotificationReceiver.EXTRA_BODY, plan.body)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            plan.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val triggerAtMillis = triggerAtMillis(plan.fireAtMinutes)
        scheduleAlarm(plan, triggerAtMillis, pendingIntent)
    }

    private fun scheduleAlarm(plan: NotificationPlan, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (canScheduleExactPendingIntentAlarms()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
                return
            } catch (_: SecurityException) {
                // Fall through to the permission-free in-process exact alarm path.
            }
        }

        scheduleInProcessExactAlarm(plan, triggerAtMillis, pendingIntent)
        scheduleInexactBroadcastFallback(triggerAtMillis, pendingIntent)
    }

    private fun canScheduleExactPendingIntentAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun scheduleInProcessExactAlarm(
        plan: NotificationPlan,
        triggerAtMillis: Long,
        fallbackPendingIntent: PendingIntent,
    ) {
        val listener = AlarmManager.OnAlarmListener {
            removeInProcessAlarm(plan.id)
            alarmManager.cancel(fallbackPendingIntent)
            postNotification(context, plan.id, plan.title, plan.body)
        }

        replaceInProcessAlarm(plan.id, alarmManager, listener)
        try {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, plan.id, listener, alarmHandler)
        } catch (_: SecurityException) {
            removeInProcessAlarm(plan.id)
        }
    }

    private fun scheduleInexactBroadcastFallback(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    override fun cancel(notificationIds: List<String>) {
        val remainingIds = loadScheduledIds().toMutableSet()
        notificationIds.forEach { id ->
            remainingIds -= id
            clearDeliveredNotificationId(context, id)
            removeInProcessAlarm(id)?.let(alarmManager::cancel)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                Intent(context, NotificationReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pendingIntent != null) alarmManager.cancel(pendingIntent)
        }
        saveScheduledIds(remainingIds)
    }

    override fun cancelAll() {
        cancel(loadScheduledIds().toList())
    }

    private fun loadScheduledIds(): Set<String> =
        preferences.getString(KEY_SCHEDULED_IDS, null)
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()

    private fun saveScheduledIds(ids: Set<String>) {
        preferences.edit().putString(KEY_SCHEDULED_IDS, ids.joinToString("\n")).apply()
    }

    private fun triggerAtMillis(minutesOfDay: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
            set(Calendar.MINUTE, minutesOfDay % 60)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DATE, 1)
            }
        }
        return calendar.timeInMillis
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            activityProvider()?.startActivity(intent) ?: context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Some Android builds do not expose this settings screen. The scheduler still uses a fallback.
        }
    }

    companion object {
        private val alarmHandler = Handler(Looper.getMainLooper())
        private val inProcessAlarms = mutableMapOf<String, AlarmManager.OnAlarmListener>()

        private fun replaceInProcessAlarm(
            id: String,
            alarmManager: AlarmManager,
            listener: AlarmManager.OnAlarmListener,
        ) {
            synchronized(inProcessAlarms) {
                inProcessAlarms.remove(id)
            }?.let(alarmManager::cancel)
            synchronized(inProcessAlarms) {
                inProcessAlarms[id] = listener
            }
        }

        private fun removeInProcessAlarm(id: String): AlarmManager.OnAlarmListener? =
            synchronized(inProcessAlarms) {
                inProcessAlarms.remove(id)
            }
    }
}

private class AndroidLiveActivityController(
    private val context: Context,
) : LiveActivityController {
    private val preferences = context.getSharedPreferences(NOTIFICATION_PREFERENCES, Context.MODE_PRIVATE)

    override fun isSupported(): Boolean = canPostNotifications(context)

    override fun isActivityRunning(): Boolean =
        preferences.getBoolean(KEY_LIVE_ACTIVITY_RUNNING, false)

    override fun start(snapshot: LiveActivitySnapshot) {
        post(snapshot)
    }

    override fun update(snapshot: LiveActivitySnapshot) {
        post(snapshot)
    }

    override fun end(snapshot: LiveActivitySnapshot?, reason: LiveActivityEndReason) {
        NotificationManagerCompat.from(context).cancel(LIVE_ACTIVITY_NOTIFICATION_ID)
        preferences.edit()
            .remove(KEY_LIVE_ACTIVITY_RUNNING)
            .remove(KEY_LIVE_ACTIVITY_COMMUTE_ID)
            .remove(KEY_LIVE_ACTIVITY_GROUP_ID)
            .apply()
    }

    private fun post(snapshot: LiveActivitySnapshot) {
        if (!canPostNotifications(context)) return
        createLiveActivityChannel(context)
        val notification = NotificationCompat.Builder(context, LIVE_ACTIVITY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(snapshot.title)
            .setContentText(liveActivityContentText(snapshot))
            .setStyle(NotificationCompat.BigTextStyle().bigText(liveActivityBigText(snapshot)))
            .setContentIntent(launchPendingIntent(context))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(formatMinutesOfDay(snapshot.finalCallMinutes))
            .setSilent(true)
            .setWhen(targetMillis(snapshot.finalCallMinutes))
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(LIVE_ACTIVITY_NOTIFICATION_ID, notification)
            preferences.edit()
                .putBoolean(KEY_LIVE_ACTIVITY_RUNNING, true)
                .putString(KEY_LIVE_ACTIVITY_COMMUTE_ID, snapshot.commuteId)
                .putString(KEY_LIVE_ACTIVITY_GROUP_ID, snapshot.groupId)
                .apply()
        } catch (_: SecurityException) {
            // Permission can be revoked between the explicit check and notify().
        }
    }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val id = intent.getStringExtra(EXTRA_ID).orEmpty()
        postNotification(context, id, title, body)
    }

    companion object {
        const val EXTRA_ID = "notification_id"
        const val EXTRA_TITLE = "notification_title"
        const val EXTRA_BODY = "notification_body"
    }
}

private const val CHANNEL_ID = "leave_window_alerts"
private const val KEY_SCHEDULED_IDS = "scheduled_notification_ids"
private const val KEY_DELIVERED_IDS = "delivered_notification_ids"
private const val KEY_LIVE_ACTIVITY_RUNNING = "live_activity_running"
private const val KEY_LIVE_ACTIVITY_COMMUTE_ID = "live_activity_commute_id"
private const val KEY_LIVE_ACTIVITY_GROUP_ID = "live_activity_group_id"
private const val NOTIFICATION_PREFERENCES = "leave_window_notifications"
private const val LIVE_ACTIVITY_CHANNEL_ID = "active_watch_status"
private const val LIVE_ACTIVITY_NOTIFICATION_ID = 41_080

private fun createChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        CHANNEL_ID,
        "Leave window alerts",
        NotificationManager.IMPORTANCE_HIGH,
    )
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

private fun createLiveActivityChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        LIVE_ACTIVITY_CHANNEL_ID,
        "Active watch status",
        NotificationManager.IMPORTANCE_LOW,
    )
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

private fun postNotification(context: Context, id: String, title: String, body: String) {
    if (!canPostNotifications(context) || !markNotificationDelivered(context, id)) return
    createChannel(context)
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setContentIntent(launchPendingIntent(context))
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(id.hashCode(), notification)
}

private fun launchPendingIntent(context: Context): PendingIntent {
    val launchIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    return PendingIntent.getActivity(
        context,
        0,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

private fun liveActivityContentText(snapshot: LiveActivitySnapshot): String =
    "${snapshot.lineLabel} to ${snapshot.directionHeadsign} - ${snapshot.walkingMinutes} min walk"

private fun liveActivityBigText(snapshot: LiveActivitySnapshot): String =
    listOf(
        snapshot.body,
        "${snapshot.stopName} - ${snapshot.lineLabel} to ${snapshot.directionHeadsign}",
        "Leave by ${formatMinutesOfDay(snapshot.finalCallMinutes)}",
    ).joinToString("\n")

private fun targetMillis(minutesOfDay: Int): Long {
    val now = Calendar.getInstance()
    val nowSeconds = (now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)) * 60 +
        now.get(Calendar.SECOND)
    val targetSeconds = minutesOfDay * 60
    return Calendar.getInstance().apply {
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
        set(Calendar.MINUTE, minutesOfDay % 60)
        if (targetSeconds < nowSeconds - 600) {
            add(Calendar.DATE, 1)
        }
    }.timeInMillis
}

private fun canPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun markNotificationDelivered(context: Context, id: String): Boolean {
    val preferences = context.getSharedPreferences(NOTIFICATION_PREFERENCES, Context.MODE_PRIVATE)
    val deliveredIds = preferences.getStringSet(KEY_DELIVERED_IDS, emptySet()).orEmpty()
    if (id in deliveredIds) return false
    preferences.edit().putStringSet(KEY_DELIVERED_IDS, deliveredIds + id).apply()
    return true
}

private fun clearDeliveredNotificationId(context: Context, id: String) {
    val preferences = context.getSharedPreferences(NOTIFICATION_PREFERENCES, Context.MODE_PRIVATE)
    val deliveredIds = preferences.getStringSet(KEY_DELIVERED_IDS, emptySet()).orEmpty()
    preferences.edit().putStringSet(KEY_DELIVERED_IDS, deliveredIds - id).apply()
}
