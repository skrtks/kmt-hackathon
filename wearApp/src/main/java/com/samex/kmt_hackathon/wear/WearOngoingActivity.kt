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
    val statusText = ongoingStatusText(snapshot)
    val targetMinutes = ongoingCountdownTargetMinutes(snapshot)
    val status = Status.Builder()
        .addTemplate("#remaining#")
        .addPart("remaining", Status.TextPart(statusText))
        .build()
    val notificationBuilder = NotificationCompat.Builder(context, WEAR_ONGOING_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_transit_ongoing)
        .setColor(0xFF0F766E.toInt())
        .setContentTitle(ongoingTitle(snapshot, statusText))
        .setContentText(ongoingContentText(snapshot))
        .setStyle(NotificationCompat.BigTextStyle().bigText(ongoingBigText(snapshot)))
        .setContentIntent(pendingIntent)
        .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setRequestPromotedOngoing(true)
        .setShortCriticalText(formatMinutesOfDay(targetMinutes))
        .setSilent(true)
        .setLocalOnly(true)
        .setWhen(targetMillis(targetMinutes))
        .setUsesChronometer(true)
        .setChronometerCountDown(true)

    OngoingActivity.Builder(context, WEAR_ONGOING_NOTIFICATION_ID, notificationBuilder)
        .setStaticIcon(R.drawable.ic_transit_ongoing)
        .setTouchIntent(pendingIntent)
        .setTitle(ongoingTitle(snapshot, statusText))
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

private fun ongoingCountdownTargetMinutes(snapshot: LiveActivitySnapshot): Int =
    if (snapshot.isLeaving) snapshot.departureTimeMinutes else snapshot.finalCallMinutes

private fun ongoingStatusText(snapshot: LiveActivitySnapshot): String =
    if (snapshot.isLeaving) {
        "Departure in ${remainingDurationText(snapshot.departureTimeMinutes)}"
    } else {
        remainingTimeText(snapshot.finalCallMinutes)
    }

private fun ongoingTitle(snapshot: LiveActivitySnapshot, statusText: String): String =
    if (snapshot.isLeaving) statusText else snapshot.title.ifBlank { "Active watch" }

private fun ongoingContentText(snapshot: LiveActivitySnapshot): String =
    "${ongoingStatusText(snapshot)} - ${snapshot.lineLabel} to ${snapshot.directionHeadsign}"

private fun ongoingBigText(snapshot: LiveActivitySnapshot): String =
    listOf(
        ongoingStatusText(snapshot),
        snapshot.body,
        "${snapshot.stopName} - ${snapshot.lineLabel} to ${snapshot.directionHeadsign}",
        if (snapshot.isLeaving) {
            "Departure ${formatMinutesOfDay(snapshot.departureTimeMinutes)}"
        } else {
            "Leave by ${formatMinutesOfDay(snapshot.finalCallMinutes)}"
        },
    ).joinToString("\n")

private fun remainingDurationText(minutesOfDay: Int): String {
    val remainingMillis = remainingMillisUntil(minutesOfDay)
    if (remainingMillis < 60_000L) {
        val remainingSeconds = if (remainingMillis == 0L) 0L else (remainingMillis / 1_000L).coerceAtLeast(1L)
        val unit = if (remainingSeconds == 1L) "second" else "seconds"
        return "$remainingSeconds $unit"
    }

    val remainingMinutes = ((remainingMillis + 59_999L) / 60_000L).coerceAtLeast(0L)
    val unit = if (remainingMinutes == 1L) "minute" else "minutes"
    return "$remainingMinutes $unit"
}

private fun remainingTimeText(minutesOfDay: Int): String =
    "${remainingDurationText(minutesOfDay)} left"

private fun remainingMillisUntil(minutesOfDay: Int): Long =
    (targetMillis(minutesOfDay) - System.currentTimeMillis()).coerceAtLeast(0L)

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
