package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.ThemeMode
import dev.ahmad.wird.domain.repository.SettingsRepository

/**
 * Chooses system, light or dark.
 *
 * Written through the repository's read-modify-write, so changing the theme cannot clobber a
 * numeral or privacy change made a moment earlier.
 */
class SetThemeModeUseCase(
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(mode: ThemeMode) {
        settings.update { it.copy(themeMode = mode) }
    }
}
