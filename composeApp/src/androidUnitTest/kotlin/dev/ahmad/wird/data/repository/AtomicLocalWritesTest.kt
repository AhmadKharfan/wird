package dev.ahmad.wird.data.repository

import dev.ahmad.wird.data.local.OutboxDao
import dev.ahmad.wird.data.local.OutboxEntity
import dev.ahmad.wird.data.local.RoomLocalTransaction
import dev.ahmad.wird.data.local.createTestDatabase
import dev.ahmad.wird.domain.model.ThemeMode
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A local write and its outbox record land together or not at all. The outbox is the log a
 * sync layer will replay; a row with no record would never reach the server, and a record
 * with no row would replay something that never happened.
 *
 * Each test makes the outbox fail the way a full disk would, and checks that the row it
 * belonged to was not left behind.
 */
class AtomicLocalWritesTest {

    private val database = createTestDatabase()
    private val clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(1_000)
    }
    private var nextId = 0
    private val ids = { "id-${nextId++}" }
    private val day = LocalDate(2026, 1, 15)

    private val failingOutbox = OutboxWriter(
        dao = object : OutboxDao by database.outboxDao() {
            override suspend fun append(record: OutboxEntity) {
                throw IllegalStateException("disk full")
            }
        },
        clock = clock,
        newId = ids,
    )
    private val transaction = RoomLocalTransaction(database)

    @AfterTest
    fun closeDatabase() = database.close()

    @Test
    fun leavesNoEntryBehindWhenItsOutboxRecordCannotBeWritten() = runTest {
        val entries = EntryRepositoryImpl(database.entryDao(), failingOutbox, transaction, clock, ids)

        assertFailsWith<IllegalStateException> { entries.setValue("duha", day, 1) }

        assertEquals(emptyList(), database.entryDao().getDay(day.toEpochDays()))
    }

    @Test
    fun leavesAToggleUndoneWhenItsOutboxRecordCannotBeWritten() = runTest {
        val entries = EntryRepositoryImpl(database.entryDao(), failingOutbox, transaction, clock, ids)

        assertFailsWith<IllegalStateException> { entries.toggle("duha", day) }

        assertEquals(emptyList(), database.entryDao().getDay(day.toEpochDays()))
    }

    @Test
    fun leavesSettingsUnchangedWhenTheirOutboxRecordCannotBeWritten() = runTest {
        val settings = SettingsRepositoryImpl(database.settingsDao(), failingOutbox, transaction, clock)

        assertFailsWith<IllegalStateException> { settings.update { it.copy(themeMode = ThemeMode.DARK) } }

        assertNull(database.settingsDao().get())
    }
}
