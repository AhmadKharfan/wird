package dev.ahmad.wird.domain.repository

import dev.ahmad.wird.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/** Local-first: settings are stored on the device and every write returns immediately. */
interface SettingsRepository {

    /**
     * The current settings, starting with [AppSettings.DEFAULTS] on a fresh install.
     * Never empty and never null.
     */
    fun observeSettings(): Flow<AppSettings>

    /**
     * Applies [transform] to the current settings and stores the result.
     *
     * Read-modify-write rather than a setter per field, so two settings changed at once
     * cannot clobber each other, and adding a field does not widen this interface.
     */
    suspend fun update(transform: (AppSettings) -> AppSettings)
}
