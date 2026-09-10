package dev.ahmad.wird.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmad.wird.ui.navigation.WirdNavGraph
import dev.ahmad.wird.ui.theme.WirdTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WirdApp() {
    val shell = koinViewModel<AppViewModel>()
    val state by shell.state.collectAsStateWithLifecycle()

    WirdTheme(darkTheme = state.forcedDarkTheme ?: isSystemInDarkTheme()) {
        WirdNavGraph()
    }
}
