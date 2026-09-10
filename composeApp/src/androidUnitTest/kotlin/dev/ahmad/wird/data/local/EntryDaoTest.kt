package dev.ahmad.wird.data.local

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The unique index on `(habitId, epochDay)` is what enforces one entry per habit per day,
 * and the UUID primary key is what lets a row keep one identity for its whole life. Those
 * two facts pull in opposite directions on a re-record, so that is what most of this tests.
 */
class EntryDaoTest {

    private val database = createTestDatabase()
    private val dao get() = database.entryDao()

    @AfterTest
    fun closeDatabase() = database.close()

    private fun entry(
        id: String,
        habitId: String = "duha",
        epochDay: Long = 10,
        value: Int = 1,
        updatedAt: Long = 1_768_000_000_000,
    ) = EntryEntity(
        id = id,
        habitId = habitId,
        epochDay = epochDay,
        value = value,
        updatedAt = updatedAt,
    )

    @Test
    fun readsBackAStoredEntry() = runTest {
        dao.insert(entry("e1", value = 3))

        assertEquals(3, dao.observeDay(10).first().single().value)
    }

    @Test
    fun readsAnEmptyDayAsAnEmptyList() = runTest {
        // Never null, never a failure: an untouched day is simply empty.
        assertEquals(emptyList(), dao.observeDay(10).first())
    }

    @Test
    fun keepsDaysApart() = runTest {
        dao.insert(entry("e1", epochDay = 10))
        dao.insert(entry("e2", habitId = "witr", epochDay = 11))

        assertEquals(listOf("e1"), dao.observeDay(10).first().map { it.id })
    }

    @Test
    fun replacesRatherThanDuplicatingWhenTheSameRowIsRewritten() = runTest {
        dao.insert(entry("e1", value = 1))
        dao.insert(entry("e1", value = 4))

        val stored = dao.observeDay(10).first()
        assertEquals(1, stored.size)
        assertEquals(4, stored.single().value)
    }

    @Test
    fun replacesThroughTheUniqueIndexAndAdoptsTheNewRowsId() = runTest {
        // Two different ids, one habit, one day. The unique index forces a replacement,
        // and SQLite does it by DELETE-then-INSERT — so the surviving row carries the
        // *new* id and the original identity is gone.
        //
        // That is the hazard the repository has to work around: minting a fresh UUID on
        // every write would give a row a new identity each time it changed, which is
        // exactly what the client-generated key exists to prevent. Repositories must
        // `find` first and reuse the stored id.
        dao.insert(entry("original", value = 1))
        dao.insert(entry("replacement", value = 4))

        val stored = dao.observeDay(10).first()
        assertEquals(1, stored.size)
        assertEquals("replacement", stored.single().id)
        assertEquals(4, stored.single().value)
    }

    @Test
    fun findsAnExistingEntryByItsNaturalKey() = runTest {
        // This is what lets a repository reuse an existing row's id instead of minting a
        // new one, which is what keeps the id stable for sync.
        dao.insert(entry("e1", value = 2))

        assertEquals("e1", dao.find("duha", 10)?.id)
    }

    @Test
    fun findsNothingForAHabitWithNoEntryThatDay() = runTest {
        assertNull(dao.find("duha", 10))
    }

    @Test
    fun stampsWhateverUpdatedAtItIsGiven() = runTest {
        dao.insert(entry("e1", updatedAt = 42))

        assertEquals(42, dao.observeDay(10).first().single().updatedAt)
    }

    // --- ranges ------------------------------------------------------------------------

    @Test
    fun readsARangeInclusiveOfBothEnds() = runTest {
        dao.insert(entry("before", epochDay = 9))
        dao.insert(entry("start", habitId = "a", epochDay = 10))
        dao.insert(entry("middle", habitId = "b", epochDay = 15))
        dao.insert(entry("end", habitId = "c", epochDay = 20))
        dao.insert(entry("after", habitId = "d", epochDay = 21))

        assertEquals(
            setOf("start", "middle", "end"),
            dao.observeRange(10, 20).first().map { it.id }.toSet(),
        )
    }

    @Test
    fun readsASingleDayRange() = runTest {
        dao.insert(entry("e1", epochDay = 10))

        assertEquals(listOf("e1"), dao.observeRange(10, 10).first().map { it.id })
    }

    @Test
    fun readsAnEmptyRangeAsAnEmptyList() = runTest {
        assertEquals(emptyList(), dao.observeRange(10, 20).first())
    }

    @Test
    fun readsBackAWriteOnTheNextRead() = runTest {
        assertEquals(emptyList(), dao.observeDay(10).first())

        dao.insert(entry("e1"))

        assertEquals(listOf("e1"), dao.observeDay(10).first().map { it.id })
    }

    @Test
    fun reachesACollectorThatWasAlreadyListening() = runTest {
        // The UI holds a long-lived collector and never refreshes by hand, so a write has
        // to reach a subscription that already exists — not merely show up on the next
        // read. If the flow never emitted, this would hang rather than pass.
        val listening = async { dao.observeDay(10).first { it.isNotEmpty() } }

        dao.insert(entry("e1"))

        assertEquals(listOf("e1"), listening.await().map { it.id })
    }
}
