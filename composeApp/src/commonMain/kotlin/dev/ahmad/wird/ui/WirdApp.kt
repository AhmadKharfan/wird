package dev.ahmad.wird.ui

import androidx.compose.runtime.Composable
import dev.ahmad.wird.ui.navigation.WirdNavGraph
import dev.ahmad.wird.ui.theme.WirdTheme

@Composable
fun WirdApp() {
    WirdTheme {
        WirdNavGraph()
    }
}
