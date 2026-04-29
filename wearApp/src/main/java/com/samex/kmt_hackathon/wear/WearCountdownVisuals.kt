package com.samex.kmt_hackathon.wear

import android.graphics.BlurMaskFilter
import android.graphics.Paint as AndroidPaint
import android.graphics.RectF
import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.core.LiveActivitySnapshot
import com.samex.kmt_hackathon.core.WatchStatus
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
internal fun FinalCallPulseRing(status: WatchStatus, modifier: Modifier = Modifier) {
    if (status != WatchStatus.FinalCall) return

    val pulseTransition = rememberInfiniteTransition(label = "finalCallRingPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "finalCallRingAlpha",
    )
    val ringColor = Color(0xFFF43F5E)

    Canvas(modifier = modifier) {
        val inset = 5.dp.toPx()
        val strokeWidth = 4.dp.toPx() + (2.dp.toPx() * pulse)
        val alpha = 0.55f + (0.35f * pulse)
        val glowInset = inset + strokeWidth + 6.dp.toPx()
        val glowRect = RectF(glowInset, glowInset, size.width - glowInset, size.height - glowInset)
        if (glowRect.width() > 0f && glowRect.height() > 0f) {
            val glowPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                style = AndroidPaint.Style.STROKE
                color = ringColor.copy(alpha = 0.28f + (0.12f * pulse)).toArgb()
                this.strokeWidth = 24.dp.toPx() + (6.dp.toPx() * pulse)
                maskFilter = BlurMaskFilter(16.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
            }
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawOval(glowRect, glowPaint)
            }
        }
        drawOval(
            color = ringColor.copy(alpha = alpha),
            topLeft = Offset(x = inset, y = inset),
            size = Size(
                width = size.width - (inset * 2f),
                height = size.height - (inset * 2f),
            ),
            style = Stroke(width = strokeWidth),
        )
    }
}

@Composable
internal fun WaterCountdownBackground(
    snapshot: LiveActivitySnapshot,
    modifier: Modifier = Modifier,
) {
    var nowSecondsOfDay by rememberCurrentSecondsOfDay(snapshot)
    val elapsedFraction by animateFloatAsState(
        targetValue = leaveWindowElapsedFraction(
            snapshot = snapshot,
            nowSecondsOfDay = nowSecondsOfDay,
        ),
        animationSpec = tween(durationMillis = 1_000, easing = LinearEasing),
        label = "waterCountdownLevel",
    )
    val waterColor = Color(0xFF0369A1)
    val waterEdgeColor = Color(0xFF38BDF8)

    Canvas(modifier = modifier) {
        val waterTop = size.height * elapsedFraction.coerceIn(0f, 1f)
        if (waterTop < size.height) {
            drawRect(
                color = waterColor,
                topLeft = Offset(x = 0f, y = waterTop),
                size = Size(width = size.width, height = size.height - waterTop),
            )
            drawLine(
                color = waterEdgeColor,
                start = Offset(x = 0f, y = waterTop),
                end = Offset(x = size.width, y = waterTop),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}

@Composable
internal fun rememberCurrentSecondsOfDay(snapshot: LiveActivitySnapshot): MutableState<Int> {
    val baseSeconds = snapshot.syncedNowSecondsOfDay ?: currentWearSecondsOfDay()
    val nowSecondsOfDay = remember(
        snapshot.commuteId,
        snapshot.groupId,
        snapshot.isLeaving,
        snapshot.syncedNowSecondsOfDay,
    ) {
        mutableStateOf(baseSeconds)
    }
    LaunchedEffect(snapshot.commuteId, snapshot.groupId, snapshot.isLeaving, snapshot.syncedNowSecondsOfDay) {
        val startedAtRealtimeMillis = SystemClock.elapsedRealtime()
        val startedAtSeconds = baseSeconds
        while (true) {
            delay(1_000)
            val elapsedSeconds = ((SystemClock.elapsedRealtime() - startedAtRealtimeMillis) / 1_000L).toInt()
            nowSecondsOfDay.value = (startedAtSeconds + elapsedSeconds).mod(SECONDS_PER_DAY)
        }
    }
    return nowSecondsOfDay
}

internal fun leaveWindowElapsedFraction(snapshot: LiveActivitySnapshot, nowSecondsOfDay: Int): Float =
    when (snapshot.status) {
        WatchStatus.GetReady -> 0f
        WatchStatus.FinalCall,
        WatchStatus.Missed -> 1f
        WatchStatus.LeaveNow -> {
            val windowOpenSeconds = snapshot.windowOpenMinutes * SECONDS_PER_MINUTE
            val finalCallSeconds = snapshot.finalCallMinutes * SECONDS_PER_MINUTE
            val totalSeconds = secondsBetween(windowOpenSeconds, finalCallSeconds).coerceAtLeast(1)
            val elapsedSeconds = elapsedSecondsInWindow(
                startSeconds = windowOpenSeconds,
                endSeconds = finalCallSeconds,
                nowSeconds = nowSecondsOfDay,
            )
            (elapsedSeconds / totalSeconds.toFloat()).coerceIn(0f, 1f)
        }
    }

internal fun currentWearSecondsOfDay(): Int {
    val calendar = Calendar.getInstance()
    return (calendar.get(Calendar.HOUR_OF_DAY) * MINUTES_PER_HOUR + calendar.get(Calendar.MINUTE)) *
        SECONDS_PER_MINUTE + calendar.get(Calendar.SECOND)
}

internal fun secondsBetween(startSeconds: Int, endSeconds: Int): Int {
    val raw = (endSeconds - startSeconds) % SECONDS_PER_DAY
    return if (raw < 0) raw + SECONDS_PER_DAY else raw
}

internal fun elapsedSecondsInWindow(startSeconds: Int, endSeconds: Int, nowSeconds: Int): Int {
    val totalSeconds = secondsBetween(startSeconds, endSeconds).coerceAtLeast(1)
    val elapsedSeconds = secondsBetween(startSeconds, nowSeconds)
    if (elapsedSeconds <= totalSeconds) return elapsedSeconds

    val secondsUntilStart = secondsBetween(nowSeconds, startSeconds)
    val secondsSinceEnd = secondsBetween(endSeconds, nowSeconds)
    return if (secondsUntilStart < secondsSinceEnd) 0 else totalSeconds
}

private const val MINUTES_PER_HOUR = 60
private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_DAY = 24 * MINUTES_PER_HOUR * SECONDS_PER_MINUTE
