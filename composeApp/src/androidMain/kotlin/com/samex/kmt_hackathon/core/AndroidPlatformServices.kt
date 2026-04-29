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
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
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

    actual fun hapticFeedback(): HapticFeedbackController = AndroidHapticFeedbackController(requireContext()) { activity }

    actual fun isWearDevice(): Boolean = isWearDevice(requireContext())

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

private class AndroidHapticFeedbackController(
    private val context: Context,
    private val activityProvider: () -> ComponentActivity?,
) : HapticFeedbackController {
    override fun perform(effect: HapticEffect) {
        val view = activityProvider()?.window?.decorView ?: return
        if (effect == HapticEffect.Critical) {
            performCriticalHaptic(context, view)
            return
        }
        view.performHapticFeedback(hapticFeedbackConstant(effect))
    }

    override fun performProgress(progress: Float) {
        val view = activityProvider()?.window?.decorView ?: return
        performProgressHaptic(context, view, progress)
    }
}

private fun performCriticalHaptic(context: Context, view: android.view.View) {
    val vibrator = systemVibrator(context)
    if (vibrator != null && vibrator.hasVibrator()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 120L, 70L, 220L),
                    intArrayOf(0, 255, 0, 255),
                    -1,
                ),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0L, 120L, 70L, 220L), -1)
        }
    }
    view.performHapticFeedback(hapticFeedbackConstant(HapticEffect.Critical))
}

private fun performProgressHaptic(context: Context, view: android.view.View, progress: Float) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val vibrator = systemVibrator(context)
    if (vibrator == null || !vibrator.hasVibrator()) {
        view.performHapticFeedback(hapticFeedbackConstant(HapticEffect.ProgressTick))
        return
    }

    val leadPulseMs = 28L + (72L * clampedProgress).toLong()
    val gapMs = (130L - (80L * clampedProgress).toLong()).coerceAtLeast(45L)
    val followPulseMs = 20L + (90L * clampedProgress).toLong()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val leadAmplitude = (45 + (140 * clampedProgress).toInt()).coerceIn(1, 255)
        val followAmplitude = (70 + (185 * clampedProgress).toInt()).coerceIn(1, 255)
        vibrator.vibrate(
            VibrationEffect.createWaveform(
                longArrayOf(0L, leadPulseMs, gapMs, followPulseMs),
                intArrayOf(0, leadAmplitude, 0, followAmplitude),
                -1,
            ),
        )
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(longArrayOf(0L, leadPulseMs, gapMs, followPulseMs), -1)
    }
}

private fun systemVibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

private fun hapticFeedbackConstant(effect: HapticEffect): Int =
    when (effect) {
        HapticEffect.Selection,
        HapticEffect.ProgressTick -> HapticFeedbackConstants.CLOCK_TICK
        HapticEffect.Confirmation ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
        HapticEffect.Critical,
        HapticEffect.Warning,
        HapticEffect.Error ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
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
            putExtra(NotificationReceiver.EXTRA_KIND, plan.kind.name)
            putExtra(NotificationReceiver.EXTRA_TITLE, plan.title)
            putExtra(NotificationReceiver.EXTRA_BODY, plan.body)
            putExtra(NotificationReceiver.EXTRA_EXPANDED_BODY, plan.expandedBody)
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
            postNotification(context, plan.id, plan.kind, plan.title, plan.body, plan.expandedBody)
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

internal class AndroidLiveActivityController(
    private val context: Context,
) : LiveActivityController {
    private val preferences = context.getSharedPreferences(NOTIFICATION_PREFERENCES, Context.MODE_PRIVATE)

    override fun isSupported(): Boolean = true

    override fun isActivityRunning(): Boolean =
        preferences.getBoolean(KEY_LIVE_ACTIVITY_RUNNING, false)

    override fun start(snapshot: LiveActivitySnapshot): Boolean =
        post(snapshot)

    override fun update(snapshot: LiveActivitySnapshot): Boolean =
        post(snapshot)

    override fun end(snapshot: LiveActivitySnapshot?, reason: LiveActivityEndReason): Boolean {
        if (reason == LiveActivityEndReason.Leaving && snapshot != null) {
            post(snapshot.copy(isLeaving = true))
            return
        }
        cancelCountdownRefresh()
        NotificationManagerCompat.from(context).cancel(LIVE_ACTIVITY_NOTIFICATION_ID)
        preferences.edit()
            .remove(KEY_LIVE_ACTIVITY_RUNNING)
            .remove(KEY_LIVE_ACTIVITY_COMMUTE_ID)
            .remove(KEY_LIVE_ACTIVITY_GROUP_ID)
            .apply()
        clearWearLiveActivity(context)
        return true
    }

    private fun post(snapshot: LiveActivitySnapshot): Boolean {
        syncWearLiveActivity(context, snapshot)
        val presentation = snapshot.activeWatchPresentation(currentSecondsOfDay())
        preferences.edit()
            .putBoolean(KEY_LIVE_ACTIVITY_RUNNING, true)
            .putString(KEY_LIVE_ACTIVITY_COMMUTE_ID, snapshot.commuteId)
            .putString(KEY_LIVE_ACTIVITY_GROUP_ID, snapshot.groupId)
            .apply()
        scheduleCountdownRefresh(snapshot)if (!canPostNotifications(context)) return false
        createLiveActivityChannel(context)
        val pendingIntent = launchPendingIntent(context)
        val notificationBuilder = NotificationCompat.Builder(context, LIVE_ACTIVITY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_transit_ongoing)
            .setColor(notificationColor(presentation.tone))
            .setContentTitle(presentation.headline)
            .setContentText(presentation.compactText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(presentation.expandedLines.joinToString("\n")))
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(formatMinutesOfDay(presentation.timerTargetMinutes))
            .setSilent(true)
            .apply {
                if (!presentation.showsFinalCallCue) {
                    setWhen(targetMillis(presentation.timerTargetMinutes))
                    setUsesChronometer(true)
                    setChronometerCountDown(true)
                }
            }

        try {
            NotificationManagerCompat.from(context).notify(LIVE_ACTIVITY_NOTIFICATION_ID, notificationBuilder.build())
            preferences.edit()
                .putBoolean(KEY_LIVE_ACTIVITY_RUNNING, true)
                .putString(KEY_LIVE_ACTIVITY_COMMUTE_ID, snapshot.commuteId)
                .putString(KEY_LIVE_ACTIVITY_GROUP_ID, snapshot.groupId)
                .apply()
            syncWearLiveActivity(context, snapshot)
            scheduleCountdownRefresh(snapshot)
            return true
        } catch (_: SecurityException) {
            // Permission can be revoked between the explicit check and notify().
            return false
        }
    }

    private fun scheduleCountdownRefresh(snapshot: LiveActivitySnapshot) {
        cancelCountdownRefresh()
        val presentation = snapshot.activeWatchPresentation(currentSecondsOfDay())
        val remainingMillis = remainingMillisUntil(presentation.timerTargetMinutes)
        val delayMillis = when {
            remainingMillis > 60_000L -> 60_000L.coerceAtMost(remainingMillis - 59_999L)
            remainingMillis > 0L -> 1_000L.coerceAtMost(remainingMillis)
            else -> return
        }
        countdownRefresh = Runnable { post(snapshot) }
        countdownHandler.postDelayed(countdownRefresh ?: return, delayMillis)
    }

    private fun cancelCountdownRefresh() {
        countdownRefresh?.let(countdownHandler::removeCallbacks)
        countdownRefresh = null
    }

    companion object {
        private val countdownHandler = Handler(Looper.getMainLooper())
        private var countdownRefresh: Runnable? = null
    }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val expandedBody = intent.getStringExtra(EXTRA_EXPANDED_BODY) ?: body
        val id = intent.getStringExtra(EXTRA_ID).orEmpty()
        val kind = intent.getStringExtra(EXTRA_KIND)
            ?.let { runCatching { NotificationKind.valueOf(it) }.getOrNull() }
            ?: NotificationKind.WindowOpen
        postNotification(context, id, kind, title, body, expandedBody)
    }

    companion object {
        const val EXTRA_ID = "notification_id"
        const val EXTRA_KIND = "notification_kind"
        const val EXTRA_TITLE = "notification_title"
        const val EXTRA_BODY = "notification_body"
        const val EXTRA_EXPANDED_BODY = "notification_expanded_body"
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
internal const val WEAR_LIVE_ACTIVITY_PATH = "/transit-live-activity"
private const val KEY_WEAR_ACTIVE = "active"
private const val KEY_WEAR_UPDATED_AT = "updated_at"
private const val KEY_WEAR_COMMUTE_ID = "commute_id"
private const val KEY_WEAR_GROUP_ID = "group_id"
private const val KEY_WEAR_STATUS = "status"
private const val KEY_WEAR_TITLE = "title"
private const val KEY_WEAR_BODY = "body"
private const val KEY_WEAR_STOP_NAME = "stop_name"
private const val KEY_WEAR_LINE_LABEL = "line_label"
private const val KEY_WEAR_DIRECTION_HEADSIGN = "direction_headsign"
private const val KEY_WEAR_DEPARTURE_TIME_MINUTES = "departure_time_minutes"
private const val KEY_WEAR_WINDOW_OPEN_MINUTES = "window_open_minutes"
private const val KEY_WEAR_FINAL_CALL_MINUTES = "final_call_minutes"
private const val KEY_WEAR_WALKING_MINUTES = "walking_minutes"
private const val KEY_WEAR_IS_LEAVING = "is_leaving"
private const val KEY_WEAR_SYNCED_NOW_SECONDS = "synced_now_seconds"

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

private fun postNotification(
    context: Context,
    id: String,
    kind: NotificationKind,
    title: String,
    body: String,
    expandedBody: String,
) {
    if (!canPostNotifications(context) || !markNotificationDelivered(context, id)) return
    createChannel(context)
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_transit_ongoing)
        .setColor(notificationColor(kind))
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(expandedBody))
        .setContentIntent(launchPendingIntent(context))
        .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
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

private fun syncWearLiveActivity(context: Context, snapshot: LiveActivitySnapshot) {
    if (isWearDevice(context)) return
    val request = PutDataMapRequest.create(WEAR_LIVE_ACTIVITY_PATH).apply {
        dataMap.putBoolean(KEY_WEAR_ACTIVE, true)
        dataMap.putLong(KEY_WEAR_UPDATED_AT, System.currentTimeMillis())
        dataMap.putLiveActivitySnapshot(snapshot)
    }.asPutDataRequest().setUrgent()
    Wearable.getDataClient(context).putDataItem(request)
}

private fun clearWearLiveActivity(context: Context) {
    if (isWearDevice(context)) return
    val request = PutDataMapRequest.create(WEAR_LIVE_ACTIVITY_PATH).apply {
        dataMap.putBoolean(KEY_WEAR_ACTIVE, false)
        dataMap.putLong(KEY_WEAR_UPDATED_AT, System.currentTimeMillis())
    }.asPutDataRequest().setUrgent()
    Wearable.getDataClient(context).putDataItem(request)
}

private fun DataMap.putLiveActivitySnapshot(snapshot: LiveActivitySnapshot) {
    putString(KEY_WEAR_COMMUTE_ID, snapshot.commuteId)
    putString(KEY_WEAR_GROUP_ID, snapshot.groupId)
    putString(KEY_WEAR_STATUS, snapshot.status.name)
    putString(KEY_WEAR_TITLE, snapshot.title)
    putString(KEY_WEAR_BODY, snapshot.body)
    putString(KEY_WEAR_STOP_NAME, snapshot.stopName)
    putString(KEY_WEAR_LINE_LABEL, snapshot.lineLabel)
    putString(KEY_WEAR_DIRECTION_HEADSIGN, snapshot.directionHeadsign)
    putInt(KEY_WEAR_DEPARTURE_TIME_MINUTES, snapshot.departureTimeMinutes)
    putInt(KEY_WEAR_WINDOW_OPEN_MINUTES, snapshot.windowOpenMinutes)
    putInt(KEY_WEAR_FINAL_CALL_MINUTES, snapshot.finalCallMinutes)
    putInt(KEY_WEAR_WALKING_MINUTES, snapshot.walkingMinutes)
    putBoolean(KEY_WEAR_IS_LEAVING, snapshot.isLeaving)
    snapshot.syncedNowSecondsOfDay?.let { putInt(KEY_WEAR_SYNCED_NOW_SECONDS, it) }
}

internal fun DataMap.toLiveActivitySnapshot(): LiveActivitySnapshot? {
    val status = getString(KEY_WEAR_STATUS)?.let { value ->
        runCatching { WatchStatus.valueOf(value) }.getOrNull()
    } ?: return null
    return LiveActivitySnapshot(
        commuteId = getString(KEY_WEAR_COMMUTE_ID).orEmpty(),
        groupId = getString(KEY_WEAR_GROUP_ID).orEmpty(),
        status = status,
        title = getString(KEY_WEAR_TITLE).orEmpty(),
        body = getString(KEY_WEAR_BODY).orEmpty(),
        stopName = getString(KEY_WEAR_STOP_NAME).orEmpty(),
        lineLabel = getString(KEY_WEAR_LINE_LABEL).orEmpty(),
        directionHeadsign = getString(KEY_WEAR_DIRECTION_HEADSIGN).orEmpty(),
        departureTimeMinutes = getInt(KEY_WEAR_DEPARTURE_TIME_MINUTES),
        windowOpenMinutes = getInt(KEY_WEAR_WINDOW_OPEN_MINUTES),
        finalCallMinutes = getInt(KEY_WEAR_FINAL_CALL_MINUTES),
        walkingMinutes = getInt(KEY_WEAR_WALKING_MINUTES),
        isLeaving = getBoolean(KEY_WEAR_IS_LEAVING, false),
        syncedNowSecondsOfDay = getOptionalInt(KEY_WEAR_SYNCED_NOW_SECONDS),
    )
}

internal fun isWearLiveActivityActive(dataMap: DataMap): Boolean =
    dataMap.getBoolean(KEY_WEAR_ACTIVE, false)

private fun DataMap.getOptionalInt(key: String): Int? =
    if (containsKey(key)) getInt(key) else null

private fun remainingMillisUntil(minutesOfDay: Int): Long =
    (targetMillis(minutesOfDay) - System.currentTimeMillis()).coerceAtLeast(0L)

private fun currentSecondsOfDay(): Int {
    val now = Calendar.getInstance()
    return (now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)) * 60 +
        now.get(Calendar.SECOND)
}

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

private fun notificationColor(kind: NotificationKind): Int =
    when (kind) {
        NotificationKind.FinalCall -> 0xFFF43F5E.toInt()
        NotificationKind.WindowOpen,
        NotificationKind.WatchStopped -> 0xFF0F766E.toInt()
    }

private fun notificationColor(tone: WatchSurfaceTone): Int =
    when (tone) {
        WatchSurfaceTone.Route -> 0xFF38BDF8.toInt()
        WatchSurfaceTone.Signal -> 0xFF0F766E.toInt()
        WatchSurfaceTone.FinalCall -> 0xFFF43F5E.toInt()
        WatchSurfaceTone.Error -> 0xFFDC2626.toInt()
    }

private fun isWearDevice(context: Context): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)

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
