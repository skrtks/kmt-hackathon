package com.samex.kmt_hackathon

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.core.AppScreen
import com.samex.kmt_hackathon.core.AppColorTheme
import com.samex.kmt_hackathon.core.CommuteDraft
import com.samex.kmt_hackathon.core.CommuteLineSelection
import com.samex.kmt_hackathon.core.MockTransitRepository
import com.samex.kmt_hackathon.core.NotificationPermissionStatus
import com.samex.kmt_hackathon.core.PlatformServices
import com.samex.kmt_hackathon.core.SavedCommute
import com.samex.kmt_hackathon.core.TransitAppModel
import com.samex.kmt_hackathon.core.UserDataRepository
import com.samex.kmt_hackathon.core.WatchCopy
import com.samex.kmt_hackathon.core.WatchStatus
import com.samex.kmt_hackathon.core.WatchUiState
import com.samex.kmt_hackathon.core.Weekday
import com.samex.kmt_hackathon.core.formatMinutesOfDay
import com.samex.kmt_hackathon.transit.LineDirection
import com.samex.kmt_hackathon.transit.TransitStop
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
@Preview
fun App() {
    val model = remember {
        TransitAppModel(
            transitRepository = MockTransitRepository(),
            userDataRepository = UserDataRepository(PlatformServices.keyValueStore()),
            notificationScheduler = PlatformServices.notificationScheduler(),
            timeProvider = PlatformServices.timeProvider(),
            liveActivityController = PlatformServices.liveActivityController(),
        )
    }

    LaunchedEffect(model) {
        model.load()
        while (true) {
            delay(1_000)
            model.tick()
        }
    }

    LeaveTheme(theme = model.userData.settings.colorTheme) {
        AppContent(model)
    }
}

@Composable
private fun AppContent(model: TransitAppModel) {
    val activeStatus = model.watchUiState()?.currentStatus
    val backgroundColor by animateColorAsState(
        targetValue = appBackgroundColor(activeStatus),
        animationSpec = TweenSpec(durationMillis = 300),
        label = "appBackgroundColor",
    )
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = backgroundColor,
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
                .fillMaxSize()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Header(
                model = model,
            )
            model.errorMessage?.let { ErrorCard(it) }
            model.pendingReplacementCommuteId?.let {
                ReplacementPrompt(
                    onConfirm = model::confirmReplacement,
                    onCancel = model::cancelReplacement,
                )
            }

            AnimatedScreenContent(
                model = model,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Header(model: TransitAppModel) {
    if (model.screen == AppScreen.Settings) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            NavButton("Back", onClick = model::closeSettings)
        }
        return
    }

    CompactAware(threshold = 320.dp) { compact ->
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderTitle(model)
                NavButton("Settings", onClick = model::openSettings)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderTitle(model, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                NavButton("Settings", onClick = model::openSettings)
            }
        }
    }
}

@Composable
private fun AnimatedScreenContent(model: TransitAppModel, modifier: Modifier = Modifier) {
    AnimatedContent(
        modifier = modifier,
        targetState = model.screen,
        transitionSpec = {
            when {
                targetState == AppScreen.Settings -> {
                    val enterFromRight = slideInHorizontally(animationSpec = tween(260)) { width -> width } +
                            fadeIn(tween(180))
                    val exitToLeft = slideOutHorizontally(animationSpec = tween(220)) { width -> -width / 3 } +
                            fadeOut(tween(160))
                    enterFromRight togetherWith exitToLeft
                }

                initialState == AppScreen.Settings -> {
                    val enterFromLeft = slideInHorizontally(animationSpec = tween(260)) { width -> -width / 3 } +
                            fadeIn(tween(180))
                    val exitToRight = slideOutHorizontally(animationSpec = tween(220)) { width -> width } +
                            fadeOut(tween(160))
                    enterFromLeft togetherWith exitToRight
                }

                else -> fadeIn(tween(120)) togetherWith fadeOut(tween(120))
            }
        },
        label = "screenContent",
    ) { screen ->
        when (screen) {
            AppScreen.Home -> HomeScreen(model)
            is AppScreen.PlaceEditor -> PlaceEditor(model)
            AppScreen.CommuteSetup -> CommuteSetup(model)
            AppScreen.CommuteEdit -> CommuteEditScreen(model)
            AppScreen.Settings -> SettingsScreen(model)
            AppScreen.Places -> PlacesScreen(model)
        }
    }
}

@Composable
private fun HeaderTitle(model: TransitAppModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Leave Window",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!PlatformServices.isWearDevice()) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.large,
                tonalElevation = 0.dp,
            ) {
                Text(
                    "Now ${formatMinutesOfDay(model.nowMinutes)}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(model: TransitAppModel) {
    CompactAware(modifier = Modifier.fillMaxSize()) { compact ->
        val activeState = model.watchUiState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PermissionCard(model)
            activeState?.let { state ->
                ActiveWatchSection(model = model, state = state, compact = compact)
            }

            if (model.userData.commutes.isEmpty()) {
                EmptyCard("No saved commutes yet. Create one from a saved place and mock stop.")
                HomeManagementActions(model = model, compact = compact)
            } else {
                Text("Saved commutes", style = MaterialTheme.typography.titleMedium)
                model.userData.commutes.forEach { commute ->
                    CommuteSummaryCard(
                        model = model,
                        commute = commute,
                        compact = compact,
                        isActive = activeState?.commute?.id == commute.id,
                        onAutoStartChange = { model.toggleCommuteAutoStart(commute.id) },
                        onDelete = { model.deleteCommute(commute.id) },
                        onEdit = { model.beginCommuteEdit(commute.id) },
                        onStart = { model.startWatch(commute.id) },
                    )
                }
                HomeManagementActions(model = model, compact = compact)
            }
            BottomNavigationScrollSpacer()
        }
    }
}

@Composable
private fun HomeManagementActions(model: TransitAppModel, compact: Boolean) {
    ActionButtons(compact) {
        Button(
            onClick = { model.beginCommuteSetup() },
            enabled = model.userData.places.isNotEmpty(),
            modifier = responsiveButtonModifier(compact),
        ) {
            ButtonLabel("Add commute")
        }
        OutlinedButton(
            onClick = { model.beginPlaceEditor() },
            modifier = responsiveButtonModifier(compact),
        ) {
            ButtonLabel("Add place")
        }
        OutlinedButton(
            onClick = { model.navigate(AppScreen.Places) },
            modifier = responsiveButtonModifier(compact),
        ) {
            ButtonLabel("Places")
        }
    }
}

@Composable
private fun ActiveWatchSection(model: TransitAppModel, state: WatchUiState, compact: Boolean) {
    var showMoreWindows by remember { mutableStateOf(false) }
    val visibleWindowCount = if (showMoreWindows) 5 else 2
    val canExpandWindows = state.groups.size > 2

    ActiveWatchHero(
        state = state,
        nowSecondsOfDay = model.nowSecondsOfDay,
        compact = compact,
        routeLabels = commuteRouteLabels(model, state.commute),
    ) {
        ActionButtons(compact) {
            Button(
                onClick = model::markLeaving,
                enabled = state.currentGroup != null && !state.silenced,
                modifier = responsiveButtonModifier(compact),
            ) {
                ButtonLabel("I'm leaving")
            }
            OutlinedButton(
                onClick = model::skipCurrentGroup,
                enabled = state.currentGroup != null && !state.silenced,
                modifier = responsiveButtonModifier(compact),
            ) {
                ButtonLabel("Skip this departure")
            }
            OutlinedButton(
                onClick = model::stopActiveSession,
                modifier = responsiveButtonModifier(compact),
            ) {
                ButtonLabel("Stop")
            }
        }
    }

    if (state.groups.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Next windows", style = MaterialTheme.typography.titleMedium)
            if (canExpandWindows) {
                TextButton(onClick = { showMoreWindows = !showMoreWindows }) {
                    ButtonLabel(if (showMoreWindows) "Show fewer" else "Show more")
                }
            }
        }
        state.groups.take(visibleWindowCount).forEach { group ->
            UpcomingWindowRow(group = group, compact = compact)
        }
    }
}

@Composable
private fun ActiveStatusPill() {
    val statusColors = leaveStatusColors()
    Surface(
        color = statusColors.signalContainer,
        contentColor = statusColors.onSignalContainer,
        shape = CircleShape,
        tonalElevation = 0.dp,
    ) {
        Text(
            "Watching now",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ScheduleOrActivePill(commute: SavedCommute, isActive: Boolean) {
    if (isActive) {
        ActiveStatusPill()
    } else {
        SchedulePill(commute)
    }
}

@Composable
private fun CommuteSummaryCard(
    model: TransitAppModel,
    commute: SavedCommute,
    compact: Boolean,
    isActive: Boolean,
    onAutoStartChange: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onStart: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = 520f, dampingRatio = 0.86f)),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = flatCardElevation(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        commuteOriginName(model, commute),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        model.stopName(commute.stopId),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            commuteOriginName(model, commute),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            model.stopName(commute.stopId),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    ScheduleOrActivePill(commute = commute, isActive = isActive)
                }
            }

            if (compact) {
                ScheduleOrActivePill(commute = commute, isActive = isActive)
            }

            RouteChipColumn(
                labels = commuteRouteLabels(model, commute),
                compact = compact,
                maxItems = 4,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            CommuteCardActions(
                compact = compact,
                autoStartEnabled = commute.autoStartEnabled,
                scheduleEnabled = commute.schedule != null,
                onAutoStartChange = onAutoStartChange,
                onDelete = onDelete,
                onEdit = onEdit,
                onStart = onStart,
                startEnabled = !isActive,
                startLabel = if (isActive) "Watching" else "Start",
            )
        }
    }
}

@Composable
private fun ActiveWatchHero(
    state: WatchUiState,
    nowSecondsOfDay: Int,
    compact: Boolean,
    routeLabels: List<String>,
    actions: @Composable () -> Unit,
) {
    val status = state.currentStatus
    val containerColor by animateColorAsState(
        targetValue = statusContainerColor(status),
        animationSpec = TweenSpec(durationMillis = 300),
        label = "watchHeroContainer",
    )
    val contentColor by animateColorAsState(
        targetValue = statusContentColor(status),
        animationSpec = TweenSpec(durationMillis = 300),
        label = "watchHeroContent",
    )
    val borderColor by animateColorAsState(
        targetValue = statusBorderColor(status),
        animationSpec = TweenSpec(durationMillis = 300),
        label = "watchHeroBorder",
    )
    val currentGroup = state.currentGroup
    val statusStyle = if (compact) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displaySmall
    val headline = state.leavingDepartureTimeMinutes
        ?.takeIf { state.silenced }
        ?.let { departureCountdownHeadline(departureTimeMinutes = it, nowSecondsOfDay = nowSecondsOfDay) }
        ?: statusHeadline(
            status = status,
            leaveAtMinutes = currentGroup?.windowOpenMinutes,
        )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = 450f, dampingRatio = 0.9f)),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        border = BorderStroke(2.dp, borderColor),
        elevation = flatCardElevation(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    headline,
                    style = statusStyle,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.stopName,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor.copy(alpha = 0.82f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            RouteChipColumn(
                labels = currentGroup?.let(::groupRouteLabels) ?: routeLabels,
                compact = compact,
                maxItems = 5,
            )

            currentGroup?.let { group ->
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WatchMetric("Leave window", "${formatMinutesOfDay(group.windowOpenMinutes)}-${formatMinutesOfDay(group.finalCallMinutes)}")
                        WatchMetric("Walk", "${state.walkingTimeMinutes} min from ${state.origin.name}")
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WatchMetric(
                            label = "Leave window",
                            value = "${formatMinutesOfDay(group.windowOpenMinutes)}-${formatMinutesOfDay(group.finalCallMinutes)}",
                            modifier = Modifier.weight(1f),
                        )
                        WatchMetric(
                            label = "Walk",
                            value = "${state.walkingTimeMinutes} min",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        "From ${state.origin.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.78f),
                    )
                }
                LeaveWindowProgress(
                    windowOpenMinutes = group.windowOpenMinutes,
                    finalCallMinutes = group.finalCallMinutes,
                    nowSecondsOfDay = nowSecondsOfDay,
                    status = status,
                )
            } ?: Text(
                "No upcoming departure window.",
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor.copy(alpha = 0.82f),
            )

            actions()
        }
    }
}

@Composable
private fun WatchMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LeaveWindowProgress(
    windowOpenMinutes: Int,
    finalCallMinutes: Int,
    nowSecondsOfDay: Int,
    status: WatchStatus?,
) {
    val statusColors = leaveStatusColors()
    val targetFillColor = when (status) {
        WatchStatus.FinalCall -> statusColors.finalCall
        WatchStatus.Missed -> MaterialTheme.colorScheme.error
        else -> statusColors.signal
    }
    val fillColor by animateColorAsState(
        targetValue = targetFillColor,
        animationSpec = TweenSpec(durationMillis = 300),
        label = "windowProgressColor",
    )
    val progress by animateFloatAsState(
        targetValue = leaveWindowProgressFraction(windowOpenMinutes, finalCallMinutes, nowSecondsOfDay),
        animationSpec = TweenSpec(durationMillis = 450),
        label = "windowProgress",
    )
    val waveAnimated = status == WatchStatus.LeaveNow || status == WatchStatus.FinalCall
    val waveTransition = rememberInfiniteTransition(label = "windowWave")
    val wavePhase by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (waveAnimated) (2f * PI.toFloat()) else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "windowWavePhase",
    )
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
        ) {
            val centerY = size.height / 2f
            val strokeWidth = 6.dp.toPx()
            drawLine(
                color = trackColor,
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            val progressWidth = (size.width * progress).coerceIn(0f, size.width)
            if (progressWidth > 0f) {
                val waveHeight = if (waveAnimated) 3.dp.toPx() else 0f
                val waveLength = 18.dp.toPx()
                val path = Path()
                val steps = 48
                for (step in 0..steps) {
                    val x = progressWidth * (step / steps.toFloat())
                    val y = centerY + sin(((x / waveLength) + wavePhase).toDouble()).toFloat() * waveHeight
                    if (step == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(
                    path = path,
                    color = fillColor,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                formatMinutesOfDay(windowOpenMinutes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatMinutesOfDay(finalCallMinutes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RouteChipColumn(labels: List<String>, compact: Boolean, maxItems: Int) {
    val visibleCount = if (compact) maxItems.coerceAtMost(3) else maxItems
    val visibleLabels = labels.take(visibleCount)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        visibleLabels.forEachIndexed { index, label ->
            RouteChip(label = label, accent = routeAccentColor(index))
        }
        if (labels.size > visibleCount) {
            RouteChip(label = "+${labels.size - visibleCount} more", accent = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun RouteChip(label: String, accent: Color) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SchedulePill(commute: SavedCommute) {
    val label = commute.schedule?.let { schedule ->
        "${schedule.days.joinToString { it.name.take(3) }} ${formatMinutesOfDay(schedule.startMinutes)}-${formatMinutesOfDay(schedule.endMinutes)}"
    } ?: "Manual"
    val enabled = commute.schedule != null && commute.autoStartEnabled
    Surface(
        color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = CircleShape,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun appBackgroundColor(status: WatchStatus?): Color =
    with(leaveStatusColors()) {
        when (status) {
            WatchStatus.LeaveNow -> signalBackground
            WatchStatus.FinalCall -> finalCallBackground
            WatchStatus.Missed -> missedBackground
            else -> MaterialTheme.colorScheme.background
        }
    }

@Composable
private fun statusContainerColor(status: WatchStatus?): Color =
    with(leaveStatusColors()) {
        when (status) {
            WatchStatus.GetReady -> routeContainer
            WatchStatus.LeaveNow -> signalContainer
            WatchStatus.FinalCall -> finalCallContainer
            WatchStatus.Missed -> missedContainer
            null -> MaterialTheme.colorScheme.surfaceVariant
        }
    }

@Composable
private fun statusContentColor(status: WatchStatus?): Color =
    with(leaveStatusColors()) {
        when (status) {
            WatchStatus.GetReady -> onRouteContainer
            WatchStatus.LeaveNow -> onSignalContainer
            WatchStatus.FinalCall -> onFinalCallContainer
            WatchStatus.Missed -> onMissedContainer
            null -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

@Composable
private fun statusBorderColor(status: WatchStatus?): Color =
    with(leaveStatusColors()) {
        when (status) {
            WatchStatus.GetReady -> route
            WatchStatus.LeaveNow -> signal
            WatchStatus.FinalCall -> finalCall
            WatchStatus.Missed -> MaterialTheme.colorScheme.error
            null -> MaterialTheme.colorScheme.outline
        }
    }

@Composable
private fun routeAccentColor(index: Int): Color =
    when (index.mod(4)) {
        0 -> MaterialTheme.colorScheme.primary
        1 -> MaterialTheme.colorScheme.secondary
        2 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

private fun leaveWindowProgressFraction(windowOpenMinutes: Int, finalCallMinutes: Int, nowSecondsOfDay: Int): Float {
    val openSeconds = windowOpenMinutes * 60
    val finalCallSeconds = finalCallMinutes * 60
    if (finalCallSeconds <= openSeconds) return 1f
    return ((nowSecondsOfDay - openSeconds).toFloat() / (finalCallSeconds - openSeconds).toFloat())
        .coerceIn(0f, 1f)
}

private fun commuteOriginName(model: TransitAppModel, commute: SavedCommute): String =
    model.userData.places.firstOrNull { it.id == commute.originPlaceId }?.name ?: "Unknown origin"

private fun commuteRouteLabels(model: TransitAppModel, commute: SavedCommute): List<String> =
    commute.selections.map { selection ->
        "${model.lineShortName(selection.lineId)} to ${model.directionHeadsign(selection.directionId)}"
    }

private fun groupRouteLabels(group: com.samex.kmt_hackathon.core.LeaveWindowGroup): List<String> =
    group.windows.map { window ->
        "${window.lineShortName} to ${window.headsign} ${formatMinutesOfDay(window.departureTimeMinutes)}"
    }

@Composable
private fun PlaceEditor(model: TransitAppModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Create saved place", style = MaterialTheme.typography.titleLarge)
        Text("Use a preset for the mock app or enter coordinates manually.")
        CompactAware { compact ->
            ActionButtons(compact) {
                model.presetPlaces.forEach { preset ->
                    OutlinedButton(
                        onClick = { model.applyPresetPlace(preset) },
                        modifier = responsiveButtonModifier(compact),
                    ) {
                        ButtonLabel(preset.name)
                    }
                }
            }
        }
        OutlinedTextField(
            value = model.placeDraft.name,
            onValueChange = { model.updatePlaceDraft(model.placeDraft.copy(name = it)) },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
        )
        CompactAware { compact ->
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlaceCoordinateFields(
                        model = model,
                        latitudeModifier = Modifier.fillMaxWidth(),
                        longitudeModifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlaceCoordinateFields(
                        model = model,
                        latitudeModifier = Modifier.weight(1f),
                        longitudeModifier = Modifier.weight(1f),
                    )
                }
            }
        }
        CompactAware { compact ->
            ActionButtons(compact) {
                Button(onClick = model::savePlace, modifier = responsiveButtonModifier(compact)) {
                    ButtonLabel("Save place")
                }
                if (!model.placeDraft.onboarding) {
                    OutlinedButton(
                        onClick = { model.navigate(AppScreen.Home) },
                        modifier = responsiveButtonModifier(compact),
                    ) {
                        ButtonLabel("Cancel")
                    }
                }
            }
        }
        BottomNavigationScrollSpacer()
    }
}

@Composable
private fun CommuteSetup(model: TransitAppModel) {
    val draft = model.commuteDraft
    val initialStep = remember {
        if (model.userData.places.size == 1 && draft.originPlaceId.isNotBlank()) {
            CommuteSetupStep.Stop
        } else {
            CommuteSetupStep.Origin
        }
    }
    var step by remember { mutableStateOf(initialStep) }

    CompactAware(modifier = Modifier.fillMaxSize()) { compact ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("New commute", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            CommuteSetupSnapshot(model, draft)
            SetupProgress(step)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = flatCardElevation(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(step.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    when (step) {
                        CommuteSetupStep.Origin -> OriginStep(model, draft)
                        CommuteSetupStep.Stop -> StopStep(model, draft, compact)
                        CommuteSetupStep.Lines -> LinesStep(model, draft, compact)
                        CommuteSetupStep.Timing -> TimingStep(model, draft, compact)
                        CommuteSetupStep.Review -> ReviewStep(model, draft)
                    }
                }
            }

            SetupNavigation(
                compact = compact,
                step = step,
                canContinue = canContinueSetupStep(step, draft),
                canSave = canSaveCommuteDraft(draft),
                onBack = { step = previousSetupStep(step) },
                onNext = { step = nextSetupStep(step) },
                onCancel = { model.navigate(AppScreen.Home) },
                onSave = model::saveCommute,
            )
            BottomNavigationScrollSpacer()
        }
    }
}

private enum class CommuteEditSection {
    Origin,
    Stop,
    Lines,
    Arrival,
    Schedule,
}

@Composable
private fun CommuteEditScreen(model: TransitAppModel) {
    val draft = model.commuteDraft
    var openSection by remember(draft.editingCommuteId) { mutableStateOf<CommuteEditSection?>(null) }

    CompactAware(modifier = Modifier.fillMaxSize()) { compact ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Edit commute", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            CommuteEditSummary(model, draft)

            CommuteEditSectionCard(
                title = "Origin",
                value = originName(model, draft),
                expanded = openSection == CommuteEditSection.Origin,
                onToggle = {
                    openSection = openSection.toggle(CommuteEditSection.Origin)
                },
                compact = compact,
            ) {
                OriginStep(model, draft)
            }

            CommuteEditSectionCard(
                title = "Stop",
                value = stopName(model, draft),
                expanded = openSection == CommuteEditSection.Stop,
                onToggle = {
                    openSection = openSection.toggle(CommuteEditSection.Stop)
                },
                compact = compact,
            ) {
                StopStep(model, draft, compact)
            }

            CommuteEditSectionCard(
                title = "Line",
                value = linesSummary(model, draft),
                expanded = openSection == CommuteEditSection.Lines,
                onToggle = {
                    openSection = openSection.toggle(CommuteEditSection.Lines)
                },
                compact = compact,
            ) {
                LinesStep(model, draft, compact)
            }

            CommuteEditSectionCard(
                title = "Leave timing",
                value = arrivalBufferSummary(model, draft),
                expanded = openSection == CommuteEditSection.Arrival,
                onToggle = {
                    openSection = openSection.toggle(CommuteEditSection.Arrival)
                },
                compact = compact,
            ) {
                ArrivalBufferEditor(model, draft, compact)
            }

            CommuteEditSectionCard(
                title = "Auto-start",
                value = scheduleSummary(draft),
                expanded = openSection == CommuteEditSection.Schedule,
                onToggle = {
                    openSection = openSection.toggle(CommuteEditSection.Schedule)
                },
                compact = compact,
            ) {
                ScheduleEditor(model, draft, compact)
            }

            ActionButtons(compact) {
                OutlinedButton(
                    onClick = { model.navigate(AppScreen.Home) },
                    modifier = responsiveButtonModifier(compact),
                ) {
                    ButtonLabel("Cancel")
                }
                Button(
                    onClick = model::saveCommute,
                    enabled = canSaveCommuteDraft(draft),
                    modifier = responsiveButtonModifier(compact),
                ) {
                    ButtonLabel("Save changes")
                }
            }
            BottomNavigationScrollSpacer()
        }
    }
}

private fun CommuteEditSection?.toggle(section: CommuteEditSection): CommuteEditSection? =
    if (this == section) null else section

@Composable
private fun CommuteEditSummary(model: TransitAppModel, draft: CommuteDraft) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "${originName(model, draft)} to ${stopName(model, draft)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                linesSummary(model, draft),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${arrivalBufferSummary(model, draft)} - ${scheduleSummary(draft)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CommuteEditSectionCard(
    title: String,
    value: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    compact: Boolean,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = 520f, dampingRatio = 0.86f)),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CommuteEditSectionText(title = title, value = value)
                    OutlinedButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                        ButtonLabel(if (expanded) "Done" else "Change")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CommuteEditSectionText(
                        title = title,
                        value = value,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = onToggle, modifier = Modifier.widthIn(min = 112.dp)) {
                        ButtonLabel(if (expanded) "Done" else "Change")
                    }
                }
            }

            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                content()
            }
        }
    }
}

@Composable
private fun CommuteEditSectionText(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private enum class CommuteSetupStep(val title: String) {
    Origin("Origin"),
    Stop("Stop"),
    Lines("Line"),
    Timing("Timing"),
    Review("Review"),
}

@Composable
private fun CommuteSetupSnapshot(model: TransitAppModel, draft: CommuteDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text("From ${originName(model, draft)}", style = MaterialTheme.typography.bodyMedium)
        Text("Stop ${stopName(model, draft)}", style = MaterialTheme.typography.bodyMedium)
        Text(
            "Line ${linesSummary(model, draft)}",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SetupProgress(step: CommuteSetupStep) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "${step.ordinal + 1} of ${CommuteSetupStep.entries.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        HorizontalDivider()
    }
}

@Composable
private fun OriginStep(model: TransitAppModel, draft: CommuteDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        model.userData.places.forEach { place ->
            SetupChoice(
                label = place.name,
                selected = draft.originPlaceId == place.id,
                onClick = { model.updateCommuteDraft(draft.copy(originPlaceId = place.id)) },
            )
        }
    }
}

@Composable
private fun StopStep(model: TransitAppModel, draft: CommuteDraft, compact: Boolean) {
    var query by remember { mutableStateOf("") }
    val stops = model.stops()
    val visibleStops = stops
        .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
        .sortedBy { it.name }
        .take(if (compact) 7 else 10)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Stop search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (visibleStops.isEmpty()) {
            Text("No matching stops.", style = MaterialTheme.typography.bodyMedium)
        } else {
            visibleStops.forEach { stop ->
                SetupChoice(
                    label = stop.name,
                    selected = draft.stopId == stop.id,
                    onClick = { model.selectStop(stop.id) },
                )
            }
        }
        if (visibleStops.size < stops.size) {
            Text(
                "${visibleStops.size} of ${stops.size} stops",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LinesStep(model: TransitAppModel, draft: CommuteDraft, compact: Boolean) {
    var query by remember(draft.stopId) { mutableStateOf("") }
    val directions = model.directionsForDraftStop()
    val visibleDirections = directions
        .filter { direction ->
            val label = lineDirectionLabel(model, direction)
            query.isBlank() || label.contains(query, ignoreCase = true)
        }
        .sortedBy { lineDirectionLabel(model, it) }
        .take(if (compact) 7 else 10)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (directions.size > 5 || query.isNotBlank()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Line search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (visibleDirections.isEmpty()) {
            Text("No lines for this stop.", style = MaterialTheme.typography.bodyMedium)
        } else {
            visibleDirections.forEach { direction ->
                val selection = CommuteLineSelection(direction.lineId, direction.id)
                val selected = selection in draft.selections
                SetupChoice(
                    label = lineDirectionLabel(model, direction),
                    selected = selected,
                    onClick = { model.selectLineDirection(direction) },
                )
            }
        }
        Text(
            draft.selections.firstOrNull()?.let { selection ->
                "Selected ${model.lineShortName(selection.lineId)} to ${model.directionHeadsign(selection.directionId)}"
            } ?: "Choose one line and direction",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun TimingStep(model: TransitAppModel, draft: CommuteDraft, compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ArrivalBufferEditor(model, draft, compact)
        HorizontalDivider()
        ScheduleEditor(model, draft, compact)
    }
}

@Composable
private fun ArrivalBufferEditor(model: TransitAppModel, draft: CommuteDraft, compact: Boolean) {
    val settings = model.userData.settings
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = draft.overrideArrivalBuffer,
                onCheckedChange = { model.updateCommuteDraft(draft.copy(overrideArrivalBuffer = it)) },
            )
            Spacer(Modifier.width(8.dp))
            Text("Custom arrival buffer")
        }
        Text(
            "Default ${settings.defaultArrivalBuffer.minEarlyMinutes}-${settings.defaultArrivalBuffer.maxEarlyMinutes} min early",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ArrivalBufferFields(
                    model = model,
                    draft = draft,
                    minModifier = Modifier.fillMaxWidth(),
                    maxModifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArrivalBufferFields(
                    model = model,
                    draft = draft,
                    minModifier = Modifier.weight(1f),
                    maxModifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ScheduleEditor(model: TransitAppModel, draft: CommuteDraft, compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = draft.scheduleEnabled,
                onCheckedChange = { model.updateCommuteDraft(draft.copy(scheduleEnabled = it)) },
            )
            Spacer(Modifier.width(8.dp))
            Text("Auto-start schedule")
        }
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScheduleTimeFields(
                    model = model,
                    draft = draft,
                    startModifier = Modifier.fillMaxWidth(),
                    endModifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScheduleTimeFields(
                    model = model,
                    draft = draft,
                    startModifier = Modifier.weight(1f),
                    endModifier = Modifier.weight(1f),
                )
            }
        }
        WeekdayGrid(model, draft, compact)
    }
}

@Composable
private fun ReviewStep(model: TransitAppModel, draft: CommuteDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ReviewLine("Origin", originName(model, draft))
        ReviewLine("Stop", stopName(model, draft))
        ReviewLine("Line", linesSummary(model, draft))
        ReviewLine("Arrival", arrivalBufferSummary(model, draft))
        ReviewLine("Schedule", scheduleSummary(draft))
    }
}

@Composable
private fun WeekdayGrid(model: TransitAppModel, draft: CommuteDraft, compact: Boolean) {
    val daysPerRow = if (compact) 3 else 7
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Weekday.entries.chunked(daysPerRow).forEach { rowDays ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowDays.forEach { day ->
                    val selected = day in draft.scheduleDays
                    val modifier = Modifier.weight(1f)
                    val onClick = {
                        val days = if (selected) draft.scheduleDays - day else draft.scheduleDays + day
                        model.updateCommuteDraft(draft.copy(scheduleDays = days))
                    }
                    if (selected) {
                        Button(onClick = onClick, enabled = draft.scheduleEnabled, modifier = modifier) {
                            ButtonLabel(day.name.take(3))
                        }
                    } else {
                        OutlinedButton(onClick = onClick, enabled = draft.scheduleEnabled, modifier = modifier) {
                            ButtonLabel(day.name.take(3))
                        }
                    }
                }
                repeat(daysPerRow - rowDays.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SetupChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    val content: @Composable () -> Unit = {
        Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
    if (selected) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            content()
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            content()
        }
    }
}

@Composable
private fun ReviewLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SetupNavigation(
    compact: Boolean,
    step: CommuteSetupStep,
    canContinue: Boolean,
    canSave: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    saveLabel: String = "Save commute",
) {
    ActionButtons(compact) {
        OutlinedButton(
            onClick = if (step == CommuteSetupStep.Origin) onCancel else onBack,
            modifier = responsiveButtonModifier(compact),
        ) {
            ButtonLabel(if (step == CommuteSetupStep.Origin) "Cancel" else "Back")
        }
        Button(
            onClick = if (step == CommuteSetupStep.Review) onSave else onNext,
            enabled = if (step == CommuteSetupStep.Review) canSave else canContinue,
            modifier = responsiveButtonModifier(compact),
        ) {
            ButtonLabel(if (step == CommuteSetupStep.Review) saveLabel else "Next")
        }
    }
}

private fun nextSetupStep(step: CommuteSetupStep): CommuteSetupStep =
    CommuteSetupStep.entries.getOrElse(step.ordinal + 1) { step }

private fun previousSetupStep(step: CommuteSetupStep): CommuteSetupStep =
    CommuteSetupStep.entries.getOrElse(step.ordinal - 1) { step }

private fun canContinueSetupStep(step: CommuteSetupStep, draft: CommuteDraft): Boolean =
    when (step) {
        CommuteSetupStep.Origin -> draft.originPlaceId.isNotBlank()
        CommuteSetupStep.Stop -> draft.stopId.isNotBlank()
        CommuteSetupStep.Lines -> draft.selections.size == 1
        CommuteSetupStep.Timing -> isTimingDraftValid(draft)
        CommuteSetupStep.Review -> canSaveCommuteDraft(draft)
    }

private fun canSaveCommuteDraft(draft: CommuteDraft): Boolean =
    draft.originPlaceId.isNotBlank() &&
            draft.stopId.isNotBlank() &&
            draft.selections.size == 1 &&
            isTimingDraftValid(draft)

private fun isTimingDraftValid(draft: CommuteDraft): Boolean {
    val bufferValid = if (draft.overrideArrivalBuffer) {
        val min = draft.minEarlyMinutes.toIntOrNull()
        val max = draft.maxEarlyMinutes.toIntOrNull()
        min != null && max != null && min >= 0 && max >= min
    } else {
        true
    }
    val scheduleValid = if (draft.scheduleEnabled) {
        val start = parseSetupMinutesOfDay(draft.scheduleStart)
        val end = parseSetupMinutesOfDay(draft.scheduleEnd)
        start != null && end != null && start < end && draft.scheduleDays.isNotEmpty()
    } else {
        true
    }
    return bufferValid && scheduleValid
}

private fun parseSetupMinutesOfDay(value: String): Int? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun originName(model: TransitAppModel, draft: CommuteDraft): String =
    model.userData.places.firstOrNull { it.id == draft.originPlaceId }?.name ?: "Not set"

private fun stopName(model: TransitAppModel, draft: CommuteDraft): String =
    draft.stopId.takeIf { it.isNotBlank() }?.let(model::stopName) ?: "Not set"

private fun linesSummary(model: TransitAppModel, draft: CommuteDraft): String =
    if (draft.selections.isEmpty()) {
        "Not set"
    } else {
        draft.selections.joinToString { selection ->
            "${model.lineShortName(selection.lineId)} to ${model.directionHeadsign(selection.directionId)}"
        }
    }

private fun lineDirectionLabel(model: TransitAppModel, direction: LineDirection): String =
    "${model.lineShortName(direction.lineId)} to ${direction.headsign}"

private fun arrivalBufferSummary(model: TransitAppModel, draft: CommuteDraft): String {
    val buffer = if (draft.overrideArrivalBuffer) {
        "${draft.minEarlyMinutes}-${draft.maxEarlyMinutes}"
    } else {
        val default = model.userData.settings.defaultArrivalBuffer
        "${default.minEarlyMinutes}-${default.maxEarlyMinutes}"
    }
    return "$buffer min early"
}

private fun scheduleSummary(draft: CommuteDraft): String =
    if (draft.scheduleEnabled) {
        "${
            draft.scheduleDays.sortedBy { it.ordinal }.joinToString { it.name.take(3) }
        } ${draft.scheduleStart}-${draft.scheduleEnd}"
    } else {
        "Manual start only"
    }

@Composable
private fun UpcomingWindowRow(group: com.samex.kmt_hackathon.core.LeaveWindowGroup, compact: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
    ) {
        if (compact) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RouteChipColumn(
                    labels = groupRouteLabels(group),
                    compact = true,
                    maxItems = 3,
                )
                Text(
                    "${formatMinutesOfDay(group.windowOpenMinutes)}-${formatMinutesOfDay(group.finalCallMinutes)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    RouteChipColumn(
                        labels = groupRouteLabels(group),
                        compact = false,
                        maxItems = 3,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "${formatMinutesOfDay(group.windowOpenMinutes)}-${formatMinutesOfDay(group.finalCallMinutes)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PlacesScreen(model: TransitAppModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CompactAware { compact ->
            ActionButtons(compact) {
                Button(
                    onClick = { model.beginPlaceEditor() },
                    modifier = responsiveButtonModifier(compact),
                ) {
                    ButtonLabel("Add place")
                }
                OutlinedButton(
                    onClick = { model.navigate(AppScreen.Home) },
                    modifier = responsiveButtonModifier(compact),
                ) {
                    ButtonLabel("Back")
                }
            }
        }
        model.userData.places.forEach { place ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = flatCardElevation(),
            ) {
                CompactAware { compact ->
                    if (compact) {
                        Column(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PlaceSummary(place.name, "${place.location.latitude}, ${place.location.longitude}")
                            OutlinedButton(
                                onClick = { model.deletePlace(place.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                ButtonLabel("Delete")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PlaceSummary(place.name, "${place.location.latitude}, ${place.location.longitude}")
                            OutlinedButton(onClick = { model.deletePlace(place.id) }) {
                                ButtonLabel("Delete")
                            }
                        }
                    }
                }
            }
        }
        BottomNavigationScrollSpacer()
    }
}

@Composable
private fun SettingsScreen(model: TransitAppModel, modifier: Modifier = Modifier) {
    val settings = model.userData.settings
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SettingsOverviewCard(
            earlyWindow = "${settings.defaultArrivalBuffer.minEarlyMinutes}-${settings.defaultArrivalBuffer.maxEarlyMinutes} min",
            walkingSpeed = "${settings.walkingSpeed.metersPerMinute.toInt()} m/min",
            colorTheme = settings.colorTheme,
            notificationStatus = model.notificationStatus,
        )

        SettingsPanel(
            title = "Arrival window",
            subtitle = "The leave window is calculated from how early you want to reach the stop.",
        ) {
            SettingStepperRow(
                label = "Minimum early",
                value = "${settings.defaultArrivalBuffer.minEarlyMinutes} min",
                description = "Closest acceptable arrival before departure.",
                onMinus = {
                    model.updateDefaultArrivalBuffer(
                        (settings.defaultArrivalBuffer.minEarlyMinutes - 1).coerceAtLeast(0),
                        settings.defaultArrivalBuffer.maxEarlyMinutes,
                    )
                },
                onPlus = {
                    model.updateDefaultArrivalBuffer(
                        settings.defaultArrivalBuffer.minEarlyMinutes + 1,
                        settings.defaultArrivalBuffer.maxEarlyMinutes.coerceAtLeast(settings.defaultArrivalBuffer.minEarlyMinutes + 1),
                    )
                },
            )
            SettingStepperRow(
                label = "Maximum early",
                value = "${settings.defaultArrivalBuffer.maxEarlyMinutes} min",
                description = "Earliest acceptable arrival before departure.",
                onMinus = {
                    model.updateDefaultArrivalBuffer(
                        settings.defaultArrivalBuffer.minEarlyMinutes,
                        (settings.defaultArrivalBuffer.maxEarlyMinutes - 1).coerceAtLeast(settings.defaultArrivalBuffer.minEarlyMinutes),
                    )
                },
                onPlus = {
                    model.updateDefaultArrivalBuffer(
                        settings.defaultArrivalBuffer.minEarlyMinutes,
                        settings.defaultArrivalBuffer.maxEarlyMinutes + 1,
                    )
                },
            )
        }
        SettingsPanel(
            title = "Walking pace",
            subtitle = "Used to estimate how long it takes to reach a stop from a saved place.",
        ) {
            SettingStepperRow(
                label = "Default pace",
                value = "${settings.walkingSpeed.metersPerMinute.toInt()} m/min",
                description = "Adjust in 5 meter/minute steps.",
                onMinus = { model.updateWalkingSpeed((settings.walkingSpeed.metersPerMinute - 5).coerceAtLeast(30.0)) },
                onPlus = { model.updateWalkingSpeed(settings.walkingSpeed.metersPerMinute + 5) },
            )
        }
        NotificationSettingsPanel(
            status = model.notificationStatus,
            onRequestPermission = model::requestNotificationPermission,
        )
        SettingsPanel(
            title = "Theme",
            subtitle = "Choose a color mood for the app.",
        ) {
            ThemePicker(
                selectedTheme = settings.colorTheme,
                onThemeSelected = model::updateColorTheme,
            )
        }
        BottomNavigationScrollSpacer()
    }
}

@Composable
private fun SettingsOverviewCard(
    earlyWindow: String,
    walkingSpeed: String,
    colorTheme: AppColorTheme,
    notificationStatus: NotificationPermissionStatus,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
        elevation = flatCardElevation(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Tune the timing defaults behind every leave window.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                ) {
                    Text(
                        "Defaults",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            CompactAware { compact ->
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsMetricPill("Window", earlyWindow)
                        SettingsMetricPill("Walk", walkingSpeed)
                        SettingsMetricPill("Theme", leaveThemeSpec(colorTheme).label)
                        SettingsMetricPill("Alerts", notificationStatus.name)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsMetricPill("Window", earlyWindow, modifier = Modifier.weight(1f))
                        SettingsMetricPill("Walk", walkingSpeed, modifier = Modifier.weight(1f))
                        SettingsMetricPill("Theme", leaveThemeSpec(colorTheme).label, modifier = Modifier.weight(1f))
                        SettingsMetricPill("Alerts", notificationStatus.name, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsMetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ThemePicker(
    selectedTheme: AppColorTheme,
    onThemeSelected: (AppColorTheme) -> Unit,
) {
    val specs = leaveThemeSpecs()
    CompactAware { compact ->
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                specs.chunked(2).forEach { rowSpecs ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowSpecs.forEach { spec ->
                            ThemeChoiceCard(
                                spec = spec,
                                selected = spec.theme == selectedTheme,
                                onClick = { onThemeSelected(spec.theme) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowSpecs.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                specs.forEach { spec ->
                    ThemeChoiceCard(
                        spec = spec,
                        selected = spec.theme == selectedTheme,
                        onClick = { onThemeSelected(spec.theme) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeChoiceCard(
    spec: LeaveThemeSpec,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) spec.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = TweenSpec(durationMillis = 220),
        label = "themeChoiceBorder",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) spec.colorScheme.primaryContainer.copy(alpha = 0.58f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        animationSpec = TweenSpec(durationMillis = 220),
        label = "themeChoiceContainer",
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ThemeSwatch(spec = spec, selected = selected)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    spec.label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    spec.description,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ThemeSwatch(spec: LeaveThemeSpec, selected: Boolean) {
    Box(modifier = Modifier.size(56.dp)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = spec.colorScheme.primaryContainer,
            shape = CircleShape,
            border = BorderStroke(1.dp, spec.colorScheme.primary.copy(alpha = 0.75f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    ThemeSwatchDot(color = spec.colorScheme.primary)
                    ThemeSwatchDot(color = spec.colorScheme.secondary)
                    ThemeSwatchDot(color = spec.colorScheme.tertiary)
                }
            }
        }
        if (selected) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp),
                color = spec.colorScheme.primary,
                contentColor = spec.colorScheme.onPrimary,
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface),
            ) {
                ThemeCheckMark()
            }
        }
    }
}

@Composable
private fun ThemeSwatchDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun ThemeCheckMark() {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(22.dp)) {
        val strokeWidth = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(size.width * 0.28f, size.height * 0.52f),
            end = Offset(size.width * 0.43f, size.height * 0.67f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.43f, size.height * 0.67f),
            end = Offset(size.width * 0.74f, size.height * 0.34f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = 520f, dampingRatio = 0.86f)),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun NotificationSettingsPanel(
    status: NotificationPermissionStatus,
    onRequestPermission: () -> Unit,
) {
    SettingsPanel(
        title = "Notifications",
        subtitle = "Alerts are only used for window open and final call timing.",
    ) {
        CompactAware { compact ->
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NotificationStatusSurface(status)
                    Button(
                        onClick = onRequestPermission,
                        enabled = status != NotificationPermissionStatus.Unsupported,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ButtonLabel(if (status == NotificationPermissionStatus.Granted) "Refresh status" else "Enable alerts")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        NotificationStatusSurface(status)
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = onRequestPermission,
                        enabled = status != NotificationPermissionStatus.Unsupported,
                    ) {
                        ButtonLabel(if (status == NotificationPermissionStatus.Granted) "Refresh status" else "Enable alerts")
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationStatusSurface(status: NotificationPermissionStatus) {
    val denied = status == NotificationPermissionStatus.Denied
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (denied) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (denied) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            1.dp,
            if (denied) MaterialTheme.colorScheme.error.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text("Current status", style = MaterialTheme.typography.labelMedium)
            Text(
                status.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PermissionCard(model: TransitAppModel) {
    if (model.notificationStatus == NotificationPermissionStatus.Granted) return
    val denied = model.notificationStatus == NotificationPermissionStatus.Denied
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (denied) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (denied) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        elevation = flatCardElevation(),
    ) {
        CompactAware { compact ->
            val buttonVisible = model.notificationStatus != NotificationPermissionStatus.Unsupported
            val message = if (model.notificationStatus == NotificationPermissionStatus.Unsupported) {
                "Notifications are unsupported on this platform. Foreground watching still works."
            } else {
                "Notifications are ${model.notificationStatus.name.lowercase()}. Foreground watching still works."
            }
            if (compact || !buttonVisible) {
                Column(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(message, style = MaterialTheme.typography.bodyLarge)
                    if (buttonVisible) {
                        Button(onClick = model::requestNotificationPermission, modifier = Modifier.fillMaxWidth()) {
                            ButtonLabel("Enable")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(12.dp))
                    Button(onClick = model::requestNotificationPermission) {
                        ButtonLabel("Enable")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplacementPrompt(onConfirm: () -> Unit, onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = flatCardElevation(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Another watch session is active.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            CompactAware { compact ->
                ActionButtons(compact) {
                    Button(onClick = onConfirm, modifier = responsiveButtonModifier(compact)) {
                        ButtonLabel("Replace")
                    }
                    OutlinedButton(onClick = onCancel, modifier = responsiveButtonModifier(compact)) {
                        ButtonLabel("Keep current")
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun Stepper(onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onMinus,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
        ) {
            StepperMark(isPlus = false)
        }
        Button(
            onClick = onPlus,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
        ) {
            StepperMark(isPlus = true)
        }
    }
}

@Composable
private fun StepperMark(isPlus: Boolean) {
    val color = LocalContentColor.current
    Canvas(modifier = Modifier.size(16.dp)) {
        val inset = 2.dp.toPx()
        val strokeWidth = 2.5.dp.toPx()
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        drawLine(
            color = color,
            start = Offset(inset, centerY),
            end = Offset(size.width - inset, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        if (isPlus) {
            drawLine(
                color = color,
                start = Offset(centerX, inset),
                end = Offset(centerX, size.height - inset),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun flatCardElevation() = CardDefaults.cardElevation(
    defaultElevation = 0.dp,
    pressedElevation = 0.dp,
    focusedElevation = 0.dp,
    hoveredElevation = 0.dp,
    draggedElevation = 0.dp,
    disabledElevation = 0.dp,
)

@Composable
private fun BottomNavigationScrollSpacer() {
    Spacer(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .height(16.dp),
    )
}

@Composable
private fun CompactAware(
    modifier: Modifier = Modifier.fillMaxWidth(),
    threshold: Dp = 520.dp,
    content: @Composable (compact: Boolean) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        content(maxWidth < threshold)
    }
}

@Composable
private fun ActionButtons(compact: Boolean, content: @Composable () -> Unit) {
    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

private fun responsiveButtonModifier(compact: Boolean): Modifier =
    if (compact) Modifier.fillMaxWidth() else Modifier.widthIn(min = 96.dp)

@Composable
private fun ButtonLabel(text: String) {
    Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun NavButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.widthIn(min = 72.dp)) {
        ButtonLabel(text)
    }
}

@Composable
private fun PlaceCoordinateFields(
    model: TransitAppModel,
    latitudeModifier: Modifier,
    longitudeModifier: Modifier,
) {
    OutlinedTextField(
        value = model.placeDraft.latitude,
        onValueChange = { model.updatePlaceDraft(model.placeDraft.copy(latitude = it)) },
        label = { Text("Latitude") },
        modifier = latitudeModifier,
    )
    OutlinedTextField(
        value = model.placeDraft.longitude,
        onValueChange = { model.updatePlaceDraft(model.placeDraft.copy(longitude = it)) },
        label = { Text("Longitude") },
        modifier = longitudeModifier,
    )
}

@Composable
private fun ArrivalBufferFields(
    model: TransitAppModel,
    draft: CommuteDraft,
    minModifier: Modifier,
    maxModifier: Modifier,
) {
    OutlinedTextField(
        value = draft.minEarlyMinutes,
        onValueChange = { model.updateCommuteDraft(draft.copy(minEarlyMinutes = it)) },
        label = { Text("At least early") },
        enabled = draft.overrideArrivalBuffer,
        modifier = minModifier,
    )
    OutlinedTextField(
        value = draft.maxEarlyMinutes,
        onValueChange = { model.updateCommuteDraft(draft.copy(maxEarlyMinutes = it)) },
        label = { Text("At most early") },
        enabled = draft.overrideArrivalBuffer,
        modifier = maxModifier,
    )
}

@Composable
private fun ScheduleTimeFields(
    model: TransitAppModel,
    draft: CommuteDraft,
    startModifier: Modifier,
    endModifier: Modifier,
) {
    OutlinedTextField(
        value = draft.scheduleStart,
        onValueChange = { model.updateCommuteDraft(draft.copy(scheduleStart = it)) },
        label = { Text("Start") },
        enabled = draft.scheduleEnabled,
        modifier = startModifier,
    )
    OutlinedTextField(
        value = draft.scheduleEnd,
        onValueChange = { model.updateCommuteDraft(draft.copy(scheduleEnd = it)) },
        label = { Text("End") },
        enabled = draft.scheduleEnabled,
        modifier = endModifier,
    )
}

@Composable
private fun CommuteCardActions(
    compact: Boolean,
    autoStartEnabled: Boolean,
    scheduleEnabled: Boolean,
    onAutoStartChange: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    startEnabled: Boolean = true,
    startLabel: String = "Start",
) {
    val autoStartControl: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Auto-start")
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = autoStartEnabled,
                onCheckedChange = { onAutoStartChange() },
                enabled = scheduleEnabled,
            )
        }
    }
    val actions: @Composable () -> Unit = {
        OutlinedButton(onClick = onEdit, modifier = responsiveButtonModifier(compact)) {
            ButtonLabel("Edit")
        }
        OutlinedButton(onClick = onDelete, modifier = responsiveButtonModifier(compact)) {
            ButtonLabel("Delete")
        }
        Button(
            onClick = onStart,
            enabled = startEnabled,
            modifier = responsiveButtonModifier(compact),
        ) {
            ButtonLabel(startLabel)
        }
    }

    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            autoStartControl()
            ActionButtons(compact = true) { actions() }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            autoStartControl()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                actions()
            }
        }
    }
}

@Composable
private fun PlaceSummary(name: String, locationText: String) {
    Column {
        Text(name, style = MaterialTheme.typography.titleMedium)
        Text(locationText)
    }
}

@Composable
private fun SettingStepperRow(
    label: String,
    value: String,
    description: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    CompactAware { compact ->
        val textBlock: @Composable (Modifier) -> Unit = { modifier ->
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val valuePill: @Composable () -> Unit = {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
            ) {
                Text(
                    value,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (compact) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            textBlock(Modifier.weight(1f))
                            Spacer(Modifier.width(10.dp))
                            valuePill()
                        }
                        Stepper(onMinus = onMinus, onPlus = onPlus)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        textBlock(Modifier.weight(1f))
                        Spacer(Modifier.width(12.dp))
                        valuePill()
                        Spacer(Modifier.width(12.dp))
                        Stepper(onMinus = onMinus, onPlus = onPlus)
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        elevation = flatCardElevation(),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun EmptyCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = flatCardElevation(),
    ) {
        Text(
            message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun statusHeadline(status: WatchStatus?, leaveAtMinutes: Int? = null): String {
    val leaveAtText = leaveAtMinutes?.let(::formatMinutesOfDay)
    return when (status) {
        WatchStatus.GetReady -> leaveAtText?.let { "Leave at $it" } ?: "Leave soon"
        WatchStatus.LeaveNow -> "Leave now"
        WatchStatus.FinalCall -> "Final call"
        WatchStatus.Missed -> leaveAtText?.let { "Next chance at $it" } ?: "Next chance"
        null -> leaveAtText?.let { "Leave at $it" } ?: "Watching"
    }
}

private fun departureCountdownHeadline(departureTimeMinutes: Int, nowSecondsOfDay: Int): String =
    "Departure in ${formatDepartureCountdown(departureTimeMinutes, nowSecondsOfDay)}"

private fun formatDepartureCountdown(departureTimeMinutes: Int, nowSecondsOfDay: Int): String {
    val departureSeconds = departureTimeMinutes * 60
    val remainingSeconds = (departureSeconds - nowSecondsOfDay).coerceAtLeast(0)
    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60
    return if (hours > 0) {
        "${hours}h ${minutes.toString().padStart(2, '0')}m"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
