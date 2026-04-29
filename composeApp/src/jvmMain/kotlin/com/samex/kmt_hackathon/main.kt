package com.samex.kmt_hackathon

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Leave",
    ) {
        App()
    }
}
