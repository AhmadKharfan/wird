package dev.ahmad.wird.data.repository

import dev.ahmad.wird.data.local.createTestDatabase
import dev.ahmad.wird.domain.model.AppSettings
import dev.ahmad.wird.domain.model.Coordinates
import dev.ahmad.wird.domain.model.NumeralSystem
import dev.ahmad.wird.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class SettingsRepositoryImplTest {

    private val database = createTestDatabase()

    private var now = Instant.fromEpochMilliseconds(1_000)
    private val clock = object : Clock {
        override fun now(): Instant = now
    }

    private var nextId = 0
    private val ids = { "id-${nextId++}" }

    private val outbox = OutboxWriter(database.outboxDao(), clock, ids)
    private val repository = SettingsRepositoryImpl(database.settingsDao(), outbox, clock, ids)

    @AfterTest
    fun closeDatabase() = database.close()

    @Test
    fun readsTheDefaultsBeforeAnythingIsEverSaved() = runTest {
        // A fresh install has no settings row, and the flow must still produce a usable
        // value rather than null or nothing at all.
        assertEquals(AppSettings.DEFAULTS, repository.observeSettings().first())
    }

    @Test
    fun appliesATransformAndKeepsIt() = runTest {
        repository.update { it.copy(themeMode = ThemeMode.DARK) }

        assertEquals(ThemeMode.DARK, repository.observeSettings().first().themeMode)
    }

    @Test
    fun appliesATransformOnTopOfTheDefaultsOnAFreshInstall() = runTest {
        // The transform is handed the defaults, so a caller never has to check whether a
        // row exists yet.
        repository.update { it.copy(numeralSystem = NumeralSystem.WESTERN) }

        val stored = repository.observeSettings().first()
        assertEquals(NumeralSystem.WESTERN, stored.numeralSystem)
        assertEquals(AppSettings.DEFAULTS.themeMode, stored.themeMode)
    }

    @Test
    fun appliesTwoTransformsInSequenceWithoutLosingTheFirst() = runTest {
        // Read-modify-write is the reason this takes a transform rather than a setter per
        // field: two changes must not clobber each other.
        repository.update { it.copy(themeMode = ThemeMode.DARK) }
        repository.update { it.copy(numeralSystem = NumeralSystem.WESTERN) }

        val stored = repository.observeSettings().first()
        assertEquals(ThemeMode.DARK, stored.themeMode)
        assertEquals(NumeralSystem.WESTERN, stored.numeralSystem)
    }

    @Test
    fun keepsALocationOnceItIsSet() = runTest {
        repository.update { it.copy(location = Coordinates(21.4225, 39.8262)) }

        assertEquals(Coordinates(21.4225, 39.8262), repository.observeSettings().first().location)
    }

    @Test
    fun clearsALocationBackToAbsent() = runTest {
        repository.update { it.copy(location = Coordinates(21.4225, 39.8262)) }

        repository.update { it.copy(location = null) }

        assertEquals(null, repository.observeSettings().first().location)
    }

    @Test
    fun keepsExactlyOneSettingsRowHoweverOftenItChanges() = runTest {
        repository.update { it.copy(themeMode = ThemeMode.DARK) }
        repository.update { it.copy(themeMode = ThemeMode.LIGHT) }
        repository.update { it.copy(themeMode = ThemeMode.SYSTEM) }

        assertEquals(1, database.settingsDao().countRows())
    }

    @Test
    fun emitsStoredSettingsOnAFirstCollectionWithNoWriteToProvokeIt() = runTest {
        repository.update { it.copy(themeMode = ThemeMode.DARK) }

        val freshRepository = SettingsRepositoryImpl(database.settingsDao(), outbox, clock, ids)

        assertEquals(ThemeMode.DARK, freshRepository.observeSettings().first().themeMode)
    }

    @Test
    fun queuesEverySettingsChange() = runTest {
        repository.update { it.copy(themeMode = ThemeMode.DARK) }
        repository.update { it.copy(numeralSystem = NumeralSystem.WESTERN) }

        assertEquals(2, database.outboxDao().count())
    }

    @Test
    fun queuesSettingsUnderTheSingletonIdentity() = runTest {
        repository.update { it.copy(themeMode = ThemeMode.DARK) }

        val queued = database.outboxDao().peek(1).single()

        assertEquals("settings", queued.entityType)
        assertEquals("settings", queued.entityId)
    }
}
