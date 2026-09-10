package dev.ahmad.wird

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.ahmad.wird.di.initKoin
import dev.ahmad.wird.ui.WirdApp
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin(platformModule = wasmPlatformModule())
    ComposeViewport(document.body!!) { WirdApp() }
}
