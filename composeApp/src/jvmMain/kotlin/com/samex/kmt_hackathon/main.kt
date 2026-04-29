package com.samex.kmt_hackathon

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kmt_hackathon.composeapp.generated.resources.Res
import kmt_hackathon.composeapp.generated.resources.leave_icon
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    Window(
        icon = painterResource(Res.drawable.leave_icon),
        onCloseRequest = ::exitApplication,
        title = "Leave",
    ) {
        App()
    }
}
