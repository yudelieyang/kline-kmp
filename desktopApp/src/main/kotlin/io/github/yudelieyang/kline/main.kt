package io.github.yudelieyang.kline

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Kline_KMP_App",
        icon = painterResource("app_icon.png"),
        state = rememberWindowState(size = DpSize(480.dp, 900.dp)),
    ) {
        App()
    }
}
