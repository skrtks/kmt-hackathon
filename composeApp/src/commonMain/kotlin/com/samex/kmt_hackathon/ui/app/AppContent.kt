package com.samex.kmt_hackathon.ui.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.samex.kmt_hackathon.core.AppScreen
import com.samex.kmt_hackathon.core.PlatformServices
import com.samex.kmt_hackathon.core.TransitAppModel
import com.samex.kmt_hackathon.core.formatMinutesOfDay
import com.samex.kmt_hackathon.ui.components.*
import com.samex.kmt_hackathon.ui.activewatch.*
import com.samex.kmt_hackathon.ui.home.*
import com.samex.kmt_hackathon.ui.places.*
import com.samex.kmt_hackathon.ui.commute.*
import com.samex.kmt_hackathon.ui.settings.*

@Composable
internal fun AppContent(model: TransitAppModel) {
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
internal fun Header(model: TransitAppModel) {
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
internal fun AnimatedScreenContent(model: TransitAppModel, modifier: Modifier = Modifier) {
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
internal fun HeaderTitle(model: TransitAppModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "Leave",
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
