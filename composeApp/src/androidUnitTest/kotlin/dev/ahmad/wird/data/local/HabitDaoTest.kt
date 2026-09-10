package dev.ahmad.wird.data.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The effective-dating queries are the whole reason this DAO is not trivial. A habit lives
 * as overlapping-free revisions sharing one `habitId`, and "which habits were in force on
 * day N" is a range predicate that is easy to get wrong at both ends — `effectiveFrom` is
 * inclusive, `retiredOn` is exclusive.
 */
class HabitDaoTest {

    private val database = createTestDatabase()
    private val dao get() = database.habitDao()

    @AfterTest
    fun closeDatabase() = database.close()

    private fun habit(
        revisionId: String,
        habitId: String = "duha",
        sortOrder: Int = 0,
        effectiveFrom: Long = 10,
        retiredOn: Long? = null,
        target: Int = 1,
        name: String = habitId,
    ) = HabitEntity(
        revisionId = revisionId,
        habitId = habitId,
        name = name,
        kind = "BOOL",
        target = target,
        iconKey = habitId,
        sortOrder = sortOrder,
        effectiveFromEpochDay = effectiveFrom,
        retiredOnEpochDay = retiredOn,
        updatedAt = 1_768_000_000_000,
    )

    // --- observeOn: the day-level window ---------------------------------------------

    @Test
    fun leavesOutAHabitThatHadNotStartedOnThatDay() = runTest {
        dao.insert(habit("r1", effectiveFrom = 10))

        assertEquals(emptyList(), dao.observeOn(9).first())
    }

    @Test
    fun includesAHabitOnItsFirstDay() = runTest {
        // effectiveFrom is inclusive; a `<` here would make day one unscoreable.
        dao.insert(habit("r1", effectiveFrom = 10))

        assertEquals(listOf("r1"), dao.observeOn(10).first().map { it.revisionId })
    }

    @Test
    fun leavesOutAHabitOnTheDayItIsRetired() = runTest {
        // retiredOn is exclusive: retiring today drops it from today, not tomorrow.
        dao.insert(habit("r1", effectiveFrom = 10, retiredOn = 15))

        assertEquals(emptyList(), dao.observeOn(15).first())
    }

    @Test
    fun includesAHabitOnItsLastLiveDay() = runTest {
        dao.insert(habit("r1", effectiveFrom = 10, retiredOn = 15))

        assertEquals(listOf("r1"), dao.observeOn(14).first().map { it.revisionId })
    }

    @Test
    fun returnsOnlyTheRevisionLiveOnThatDay() = runTest {
        // A target edit leaves two revisions under one habitId. Returning both would
        // double the habit's contribution to the day's maximum.
        dao.insert(habit("old", effectiveFrom = 10, retiredOn = 12, target = 3))
        dao.insert(habit("new", effectiveFrom = 12, target = 5))

        assertEquals(listOf(3), dao.observeOn(11).first().map { it.target })
        assertEquals(listOf(5), dao.observeOn(12).first().map { it.target })
    }

    @Test
    fun ordersBySortOrder() = runTest {
        dao.insert(habit("c", habitId = "third", sortOrder = 2))
        dao.insert(habit("a", habitId = "first", sortOrder = 0))
        dao.insert(habit("b", habitId = "second", sortOrder = 1))

        assertEquals(listOf("first", "second", "third"), dao.observeOn(10).first().map { it.habitId })
    }

    // --- observeIn: the range window ----------------------------------------------------

    @Test
    fun includesARevisionThatOverlapsOnlyTheStartOfTheRange() = runTest {
        // Retired on day 11, so it was live on day 10 — the range's first day.
        dao.insert(habit("r1", effectiveFrom = 5, retiredOn = 11))

        assertEquals(listOf("r1"), dao.observeIn(10, 20).first().map { it.revisionId })
    }

    @Test
    fun includesARevisionThatOverlapsOnlyTheEndOfTheRange() = runTest {
        dao.insert(habit("r1", effectiveFrom = 20))

        assertEquals(listOf("r1"), dao.observeIn(10, 20).first().map { it.revisionId })
    }

    @Test
    fun leavesOutARevisionRetiredBeforeTheRangeBegins() = runTest {
        // Retired on day 10 means its last live day was 9, before the range.
        dao.insert(habit("r1", effectiveFrom = 5, retiredOn = 10))

        assertEquals(emptyList(), dao.observeIn(10, 20).first().map { it.revisionId })
    }

    @Test
    fun leavesOutARevisionStartingAfterTheRangeEnds() = runTest {
        dao.insert(habit("r1", effectiveFrom = 21))

        assertEquals(emptyList(), dao.observeIn(10, 20).first().map { it.revisionId })
    }

    @Test
    fun returnsEveryRevisionOverlappingTheRange() = runTest {
        // A month view needs both sides of a mid-range target edit in one read.
        dao.insert(habit("old", effectiveFrom = 5, retiredOn = 15, target = 3))
        dao.insert(habit("new", effectiveFrom = 15, target = 5))

        assertEquals(setOf("old", "new"), dao.observeIn(10, 20).first().map { it.revisionId }.toSet())
    }

    // --- observeActive ---------------------------------------------------------------------

    @Test
    fun treatsAnUnretiredRevisionAsActive() = runTest {
        dao.insert(habit("r1"))

        assertEquals(listOf("r1"), dao.observeActive().first().map { it.revisionId })
    }

    @Test
    fun treatsARetiredRevisionAsInactive() = runTest {
        dao.insert(habit("r1", retiredOn = 15))

        assertEquals(emptyList(), dao.observeActive().first())
    }

    // --- writes ------------------------------------------------------------------------------

    @Test
    fun reportsWhetherAnyHabitHasEverExisted() = runTest {
        assertFalse(dao.hasAny())

        dao.insert(habit("r1", retiredOn = 15))

        // Retired, so not active — but it has existed, which is what seeding asks.
        assertTrue(dao.hasAny())
    }

    @Test
    fun findsTheOpenRevisionOfAHabit() = runTest {
        dao.insert(habit("old", effectiveFrom = 10, retiredOn = 12))
        dao.insert(habit("new", effectiveFrom = 12))

        assertEquals("new", dao.currentRevision("duha")?.revisionId)
    }

    @Test
    fun findsNoOpenRevisionOnceAHabitIsRetired() = runTest {
        dao.insert(habit("r1", retiredOn = 15))

        assertNull(dao.currentRevision("duha"))
    }

    @Test
    fun retiresTheOpenRevisionOnly() = runTest {
        dao.insert(habit("old", effectiveFrom = 10, retiredOn = 12))
        dao.insert(habit("new", effectiveFrom = 12))

        dao.retire("duha", 20)

        val byId = dao.observeIn(0, 100).first().associateBy { it.revisionId }
        assertEquals(12, byId.getValue("old").retiredOnEpochDay)
        assertEquals(20, byId.getValue("new").retiredOnEpochDay)
    }

    @Test
    fun findsTheNewestRevisionWhetherRetiredOrNot() = runTest {
        dao.insert(habit("old", effectiveFrom = 10, retiredOn = 12))
        dao.insert(habit("new", effectiveFrom = 12, retiredOn = 15))

        assertEquals("new", dao.latestRevision("duha")?.revisionId)
    }

    @Test
    fun reopensOneRevisionAndLeavesEveryOtherBoundary() = runTest {
        // Every other retirement date is history. Clearing them all would leave two
        // revisions live on the same day.
        dao.insert(habit("old", effectiveFrom = 10, retiredOn = 12))
        dao.insert(habit("new", effectiveFrom = 12, retiredOn = 15))

        dao.reopen("new")

        val byId = dao.observeIn(0, 100).first().associateBy { it.revisionId }
        assertEquals(12, byId.getValue("old").retiredOnEpochDay)
        assertNull(byId.getValue("new").retiredOnEpochDay)
    }

    @Test
    fun renumbersEveryRevisionOfAHabitWhenReordering() = runTest {
        // Order is presentational, so it applies to history too — otherwise a past day
        // would render in a different order from today.
        dao.insert(habit("old", effectiveFrom = 10, retiredOn = 12, sortOrder = 0))
        dao.insert(habit("new", effectiveFrom = 12, sortOrder = 0))

        dao.setSortOrder("duha", 7)

        assertTrue(dao.observeIn(0, 100).first().all { it.sortOrder == 7 })
    }

    @Test
    fun readsBackAWriteOnTheNextRead() = runTest {
        // The UI reacts to writes with no manual refresh; that is the flow's job.
        assertEquals(emptyList(), dao.observeOn(10).first())

        dao.insert(habit("r1"))

        assertEquals(listOf("r1"), dao.observeOn(10).first().map { it.revisionId })
    }
}
