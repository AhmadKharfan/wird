package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.PrivacyMode
import dev.ahmad.wird.domain.repository.SettingsRepository

/**
 * Chooses one of the three privacy options, optionally with a nickname.
 *
 * The nickname is remembered across modes: passing none keeps the one already given, so a
 * user who goes to points-only and back is not asked to type it again. Passing a blank one
 * clears it. The nickname mode itself cannot be chosen without a nickname — AppSettings
 * refuses that state — and because the check runs inside the read-modify-write, a refused
 * choice leaves the stored settings exactly as they were.
 */
class SetPrivacyModeUseCase(
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(mode: PrivacyMode, nickname: String? = null) {
        val given = nickname?.trim()

        settings.update { current ->
            val kept = when {
                given == null -> current.nickname
                given.isEmpty() -> null
                else -> given
            }
            current.copy(privacyMode = mode, nickname = kept)
        }
    }
}
