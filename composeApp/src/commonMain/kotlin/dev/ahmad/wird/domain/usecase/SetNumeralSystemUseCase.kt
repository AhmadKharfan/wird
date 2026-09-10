package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.NumeralSystem
import dev.ahmad.wird.domain.repository.SettingsRepository

/**
 * Chooses western or Arabic-Indic digits for every number in the app.
 *
 * Only the choice lives here. Rendering a number in the chosen digits is display work, and
 * happens in one formatter in the UI layer, because the domain carries no display text.
 */
class SetNumeralSystemUseCase(
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(system: NumeralSystem) {
        settings.update { it.copy(numeralSystem = system) }
    }
}
