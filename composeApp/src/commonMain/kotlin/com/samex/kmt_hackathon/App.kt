package com.samex.kmt_hackathon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.samex.kmt_hackathon.core.MockTransitRepository
import com.samex.kmt_hackathon.core.PlatformServices
import com.samex.kmt_hackathon.core.TransitAppModel
import com.samex.kmt_hackathon.core.UserDataRepository
import com.samex.kmt_hackathon.ui.activewatch.WatchSessionHaptics
import com.samex.kmt_hackathon.ui.app.AppContent
import kotlinx.coroutines.delay

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
    WatchSessionHaptics(model)

    LeaveTheme(theme = model.userData.settings.colorTheme) {
        AppContent(model)
    }
}
