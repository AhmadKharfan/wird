package dev.ahmad.wird.domain.fake

import dev.ahmad.wird.domain.model.AppSettings
import dev.ahmad.wird.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onStart

/**
 * A real in-memory [SettingsRepository], not a mock.
 *
 * Starts at [AppSettings.DEFAULTS] exactly as the real one does for a fresh install, so a
 * ViewModel test never has to seed settings before it can read them.
 */
class FakeSettingsRepository(
    initial: AppSettings = AppSettings.DEFAULTS,
    val controls: FakeControls = FakeControls(),
) : SettingsRepository {

    private val stored = MutableStateFlow(initial)

    /** What is currently held, for assertions. */
    val settings: AppSettings get() = stored.value

    override fun observeSettings(): Flow<AppSettings> = stored.onStart { controls.gate() }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        controls.gate()
        stored.value = transform(stored.value)
    }
}
