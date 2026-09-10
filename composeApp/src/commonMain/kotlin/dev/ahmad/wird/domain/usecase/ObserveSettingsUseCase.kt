package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.AppSettings
import dev.ahmad.wird.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

/**
 * The current settings, live. Starts at [AppSettings.DEFAULTS] on a fresh install.
 *
 * A pass-through, kept as one because ViewModels call use cases only, never a repository.
 */
class ObserveSettingsUseCase(
    private val settings: SettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> = settings.observeSettings()
}
