package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.PrivacyMode
import dev.ahmad.wird.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Whether the circle tab exists right now.
 *
 * Choosing "compare me to myself" removes the tab rather than greying it out — the rule
 * itself is [PrivacyMode.showsCircles]. This makes it live, so the tab goes the moment the
 * choice is made, and only announces actual changes: a theme or numeral change must not make
 * the navigation rebuild its tabs.
 */
class ObserveCircleTabVisibleUseCase(
    private val settings: SettingsRepository,
) {
    operator fun invoke(): Flow<Boolean> =
        settings.observeSettings()
            .map { it.privacyMode.showsCircles }
            .distinctUntilChanged()
}
