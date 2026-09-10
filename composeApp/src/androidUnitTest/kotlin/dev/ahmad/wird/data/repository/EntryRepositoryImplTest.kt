package dev.ahmad.wird.data.repository

import dev.ahmad.wird.data.local.RoomLocalTransaction
import dev.ahmad.wird.data.local.createTestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class EntryRepositoryImplTest {

    private val database = createTestDatabase()
    private val day = LocalDate(2026, 1, 15)

    private var now = Instant.fromEpochMilliseconds(1_000)
    private val clock = object : Clock {
        override fun now(): Instant = now
    }

    private var nextId = 0
    private val ids = { "id-${nextId++}" }

    private val outbox = OutboxWriter(database.outboxDao(), clock, ids)
    private val repository = EntryRepositoryImpl(database.entryDao(), outbox, RoomLocalTransaction(database), clock, ids)

    @AfterTest
    fun closeDatabase() = database.close()

    // --- writing ------------------------------------------------------------------------

    @Test
    fun storesAValueAndReadsItBack() = runTest {
        repository.setValue("prayers", day, 3)

        assertEquals(3, repository.observeDay(day).first().single().value)
    }

    @Test
    fun stampsTheWriteTime() = runTest {
        now = Instant.fromEpochMilliseconds(555)

        repository.setValue("prayers", day, 1)

        assertEquals(now, repository.observeDay(day).first().single().updatedAt)
    }

    @Test
    fun keepsOneRowPerHabitPerDay() = runTest {
        repository.setValue("prayers", day, 1)
        repository.setValue("prayers", day, 4)

        assertEquals(1, repository.observeDay(day).first().size)
        assertEquals(4, repository.observeDay(day).first().single().value)
    }

    @Test
    fun keepsAnEntrysIdStableAcrossEdits() = runTest {
        // The reason the primary key is client-generated at all. Replacing through the
        // unique index adopts the new row's id, so a repository that minted a fresh id on
        // every write would rename the row each time it changed — and a future sync layer
        // would see one row as many.
        repository.setValue("prayers", day, 1)
        val originalId = repository.observeDay(day).first().single().id

        repository.setValue("prayers", day, 4)

        assertEquals(originalId, repository.observeDay(day).first().single().id)
    }

    @Test
    fun givesDifferentHabitsDifferentIds() = runTest {
        repository.setValue("prayers", day, 1)
        repository.setValue("duha", day, 1)

        val ids = repository.observeDay(day).first().map { it.id }
        assertEquals(2, ids.toSet().size)
    }

    @Test
    fun givesTheSameHabitOnDifferentDaysDifferentIds() = runTest {
        repository.setValue("prayers", day, 1)
        repository.setValue("prayers", LocalDate(2026, 1, 16), 1)

        val first = repository.observeDay(day).first().single().id
        val second = repository.observeDay(LocalDate(2026, 1, 16)).first().single().id
        assertNotEquals(first, second)
    }

    // --- toggle --------------------------------------------------------------------------

    @Test
    fun togglesAnAbsentEntryToOne() = runTest {
        repository.toggle("duha", day)

        assertEquals(1, repository.observeDay(day).first().single().value)
    }

    @Test
    fun togglesAStoredOneBackToZero() = runTest {
        repository.toggle("duha", day)
        repository.toggle("duha", day)

        // Zero is stored, not deleted: "explicitly not done" stays distinct from
        // "never opened".
        assertEquals(1, repository.observeDay(day).first().size)
        assertEquals(0, repository.observeDay(day).first().single().value)
    }

    @Test
    fun togglesANonZeroCounterBackToZero() = runTest {
        repository.setValue("prayers", day, 3)

        repository.toggle("prayers", day)

        assertEquals(0, repository.observeDay(day).first().single().value)
    }

    @Test
    fun keepsTheIdStableAcrossAToggle() = runTest {
        repository.toggle("duha", day)
        val originalId = repository.observeDay(day).first().single().id

        repository.toggle("duha", day)

        assertEquals(originalId, repository.observeDay(day).first().single().id)
    }

    // --- reading -------------------------------------------------------------------------

    @Test
    fun readsAnUntouchedDayAsEmpty() = runTest {
        assertEquals(emptyList(), repository.observeDay(day).first())
    }

    @Test
    fun emitsStoredDataOnAFirstCollectionWithNoWriteToProvokeIt() = runTest {
        // Pins the contract: a first collection returns what is stored, with nothing
        // written to provoke it.
        //
        // Be aware of what this test can and cannot catch. The defect it was written for
        // is wasmJs-only — in the browser Room's flow delivered no initial value, so a
        // reload showed an empty routine until the user tapped something. On the JVM Room
        // emits initially anyway, so deleting `seededWith` from the repository still
        // passes this test. Verified by mutation, not assumed. The seed's real proof has
        // to come from the browser build, once a screen consumes these repositories.
        repository.setValue("prayers", day, 3)

        val freshRepository = EntryRepositoryImpl(database.entryDao(), outbox, RoomLocalTransaction(database), clock, ids)

        assertEquals(3, freshRepository.observeDay(day).first().single().value)
    }

    @Test
    fun readsARangeInclusiveOfBothEnds() = runTest {
        repository.setValue("a", LocalDate(2026, 1, 9), 1)
        repository.setValue("b", LocalDate(2026, 1, 10), 1)
        repository.setValue("c", LocalDate(2026, 1, 20), 1)
        repository.setValue("d", LocalDate(2026, 1, 21), 1)

        val within = repository.observeRange(LocalDate(2026, 1, 10), LocalDate(2026, 1, 20)).first()

        assertEquals(setOf("b", "c"), within.map { it.habitId }.toSet())
    }

    // --- the outbox -------------------------------------------------------------------------

    @Test
    fun queuesEveryWriteForTheSyncLayerThatDoesNotExistYet() = runTest {
        repository.setValue("prayers", day, 1)
        repository.toggle("duha", day)

        assertEquals(2, database.outboxDao().count())
    }

    @Test
    fun queuesEachEditSeparatelyRatherThanCollapsingThem() = runTest {
        // The outbox is an ordered log of intent. Two edits are two facts, and collapsing
        // them would lose the order a server needs to replay.
        repository.setValue("prayers", day, 1)
        repository.setValue("prayers", day, 4)

        assertEquals(2, database.outboxDao().count())
    }

    @Test
    fun queuesTheEntryIdAndTypeSoARecordCanBeFoundAgain() = runTest {
        repository.setValue("prayers", day, 1)
        val storedId = repository.observeDay(day).first().single().id

        val queued = database.outboxDao().peek(1).single()

        assertEquals("entry", queued.entityType)
        assertEquals(storedId, queued.entityId)
        assertEquals(0, queued.attempts)
    }

    @Test
    fun queuesThePayloadAsItWasAtWriteTime() = runTest {
        // Captured now, not re-read at drain time — otherwise the queue would send
        // whatever the row says later rather than what actually happened.
        repository.setValue("prayers", day, 3)

        val payload = database.outboxDao().peek(1).single().payload

        assertTrue(payload.contains("\"value\":3"), "payload was: $payload")
        assertTrue(payload.contains("\"habitId\":\"prayers\""), "payload was: $payload")
    }
}
