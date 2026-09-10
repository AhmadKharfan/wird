package dev.ahmad.wird.ui

import androidx.compose.runtime.Immutable
import dev.ahmad.wird.domain.model.ThemeMode
import dev.ahmad.wird.domain.usecase.ObserveSettingsUseCase
import dev.ahmad.wird.ui.base.BaseViewModel

/**
 * What the app shell needs to paint every screen. Display-ready: the domain's ThemeMode
 * never reaches a composable.
 */
@Immutable
data class AppUiState(
    /** `null` follows the system; `true` or `false` forces that palette. */
    val forcedDarkTheme: Boolean? = null,
)

/**
 * Decides which palette the whole app paints, from the theme mode in settings, and applies a
 * change live — choosing dark on the settings screen repaints the app there and then, not on
 * the next launch.
 *
 * It has no one-shot effects, hence [Nothing].
 */
class AppViewModel(
    observeSettings: ObserveSettingsUseCase,
) : BaseViewModel<AppUiState, Nothing>(AppUiState()) {

    init {
        collectFlow(
            flow = observeSettings(),
            onEach = { settings ->
                updateState { it.copy(forcedDarkTheme = settings.themeMode.forcedDarkTheme()) }
            },
            // Keep following the system. A settings failure must not stop the app from
            // painting, and the system palette is the one choice that is never wrong for long.
            onError = { },
        )
    }
}

/** Light is forced as well as dark: on a dark phone, choosing light must still paint light. */
private fun ThemeMode.forcedDarkTheme(): Boolean? = when (this) {
    ThemeMode.SYSTEM -> null
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
