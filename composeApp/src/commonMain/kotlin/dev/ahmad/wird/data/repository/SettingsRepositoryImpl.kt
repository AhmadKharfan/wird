package dev.ahmad.wird.data.repository

import dev.ahmad.wird.data.local.SettingsDao
import dev.ahmad.wird.data.mapper.toDomain
import dev.ahmad.wird.data.mapper.toEntity
import dev.ahmad.wird.domain.model.AppSettings
import dev.ahmad.wird.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.time.Clock

/**
 * Local-first settings, with the absent row treated as [AppSettings.DEFAULTS] rather than
 * as nothing.
 *
 * That substitution is what lets every caller — including the transform passed to
 * [update] — work with real settings without first checking whether a fresh install has
 * saved any yet.
 */
class SettingsRepositoryImpl(
    private val settings: SettingsDao,
    private val outbox: OutboxWriter,
    private val clock: Clock,
    private val newId: () -> String,
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> =
        settings.observe()
            .seededWith { settings.get() }
            .map { row -> row?.toDomain() ?: AppSettings.DEFAULTS }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        // Read-modify-write, so two settings changed at once cannot clobber each other.
        val current = settings.get()?.toDomain() ?: AppSettings.DEFAULTS
        val updated = transform(current)
        val writtenAt = clock.now().toEpochMilliseconds()

        settings.upsert(updated.toEntity(updatedAt = writtenAt))

        outbox.record(
            entityType = OutboxWriter.TYPE_SETTINGS,
            // There is only ever one settings row, so its identity is its type.
            entityId = OutboxWriter.TYPE_SETTINGS,
            op = OutboxWriter.OP_UPSERT,
            payload = buildJsonObject {
                put("themeMode", JsonPrimitive(updated.themeMode.name))
                put("numeralSystem", JsonPrimitive(updated.numeralSystem.name))
                put("latitude", JsonPrimitive(updated.location?.latitude))
                put("longitude", JsonPrimitive(updated.location?.longitude))
                put("calculationMethod", JsonPrimitive(updated.calculationMethod.name))
                put("privacyMode", JsonPrimitive(updated.privacyMode.name))
                put("nickname", JsonPrimitive(updated.nickname))
                put("onboardingCompleted", JsonPrimitive(updated.onboardingCompleted))
                put("updatedAt", JsonPrimitive(writtenAt))
            },
        )
    }
}
