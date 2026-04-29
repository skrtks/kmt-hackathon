package com.samex.kmt_hackathon.wear

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.samex.kmt_hackathon.core.LiveActivitySnapshot
import com.samex.kmt_hackathon.core.activeWatchPresentation
import com.samex.kmt_hackathon.core.formatMinutesOfDay
import java.util.Calendar

internal fun canPostWearOngoingActivity(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

internal fun shouldRequestWearOngoingActivityPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canPostWearOngoingActivity(context)

internal fun postWearOngoingActivity(context: Context, snapshot: LiveActivitySnapshot) {
    if (!canPostWearOngoingActivity(context)) return

    createWearOngoingChannel(context)
    val pendingIntent = launchWearAppPendingIntent(context)
    val presentation = snapshot.activeWatchPresentation(currentSecondsOfDay())
    val title = ongoingTitle(snapshot)
    val status = Status.Builder()
        .addTemplate("#remaining#")
        .addPart("remaining", Status.TextPart(presentation.headline))
        .build()
    val notificationBuilder = NotificationCompat.Builder(context, WEAR_ONGOING_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_transit_ongoing)
        .setColor(0xFF0F766E.toInt())
        .setContentTitle(title)
        .setContentText(presentation.headline)
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
        .setLocalOnly(true)
        .apply {
            if (!presentation.showsFinalCallCue) {
                setWhen(targetMillis(presentation.timerTargetMinutes))
                setUsesChronometer(true)
                setChronometerCountDown(true)
            }
        }

    OngoingActivity.Builder(context, WEAR_ONGOING_NOTIFICATION_ID, notificationBuilder)
        .setStaticIcon(R.drawable.ic_transit_ongoing)
        .setTouchIntent(pendingIntent)
        .setTitle(title)
        .setStatus(status)
        .build()
        .apply(context)

    try {
        NotificationManagerCompat.from(context).notify(WEAR_ONGOING_NOTIFICATION_ID, notificationBuilder.build())
    } catch (_: SecurityException) {
        // Permission can be revoked between the explicit check and notify().
    }
}

internal fun cancelWearOngoingActivity(context: Context) {
    NotificationManagerCompat.from(context).cancel(WEAR_ONGOING_NOTIFICATION_ID)
}

private fun createWearOngoingChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val channel = NotificationChannel(
        WEAR_ONGOING_CHANNEL_ID,
        "Active watch status",
        NotificationManager.IMPORTANCE_LOW,
    )
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

private fun launchWearAppPendingIntent(context: Context): PendingIntent {
    val launchIntent = Intent(context, WearMainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    return PendingIntent.getActivity(
        context,
        0,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

private fun ongoingTitle(snapshot: LiveActivitySnapshot): String =
    snapshot.lineLabel.ifBlank { "Active watch" }

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

private const val WEAR_ONGOING_CHANNEL_ID = "active_watch_status"
private const val WEAR_ONGOING_NOTIFICATION_ID = 41_080
