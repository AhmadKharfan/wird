package dev.ahmad.wird.data.remote

import dev.ahmad.wird.domain.model.DayStats
import dev.ahmad.wird.domain.model.MemberWeek
import dev.ahmad.wird.domain.usecase.HistoryFixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * The stand-in for the circle backend, which lands in a later pack. The UI must not be able
 * to tell it from the real thing, so it behaves like a server with a cache: it holds one
 * circle, answers for the current Saturday week, and keeps its own notion of "last updated".
 *
 * Today in these tests is Thursday 15 January 2026, so the week began Saturday the 10th.
 */
class FakeCircleRepositoryTest {

    private val saturday = LocalDate(2026, 1, 10)
    private val nextSaturday = LocalDate(2026, 1, 17)

    private fun repository() = FakeCircleRepository(HistoryFixtures.clock, HistoryFixtures.zone)

    private fun List<MemberWeek>.totalsByName(): Map<String, Pair<Int, Int>> =
        associate { (if (it.isMe) "me" else it.member.displayName!!) to (it.points to it.maxPoints) }

    // --- the seeded circle -----------------------------------------------------------------

    @Test
    fun belongsToTheSeededCircle() = runTest {
        val circles = repository().observeMyCircles().first()

        assertEquals(listOf("حلقة الفجر"), circles.map { it.name })
        assertEquals(5, circles.single().memberCount)
    }

    @Test
    fun seedsTheFiveMembersWithTheirWeekSoFar() = runTest {
        // 83, 79, 70, 64 and 58 of 90 are 92%, 88%, 78%, 71% and 64% — the specified board.
        val circle = repository().observeMyCircles().first().single()

        val week = repository().observeWeek(circle.id, saturday).first()!!

        assertEquals(
            mapOf(
                "سليم" to (83 to 90),
                "ياسر" to (79 to 90),
                "me" to (70 to 90),
                "خالد" to (64 to 90),
                "معاذ" to (58 to 90),
            ),
            week.members.totalsByName(),
        )
    }

    @Test
    fun marksExactlyOneMemberAsMe() = runTest {
        val repository = repository()
        val circle = repository.observeMyCircles().first().single()

        val week = repository.observeWeek(circle.id, saturday).first()!!

        assertEquals(1, week.members.count { it.isMe })
    }

    @Test
    fun reportsAGroupStreakOfFour() = runTest {
        val repository = repository()
        val circle = repository.observeMyCircles().first().single()

        assertEquals(4, repository.observeWeek(circle.id, saturday).first()!!.groupStreak)
    }

    @Test
    fun saysWhenItWasLastRefreshed() = runTest {
        val repository = repository()
        val circle = repository.observeMyCircles().first().single()

        assertEquals(HistoryFixtures.now, repository.observeWeek(circle.id, saturday).first()!!.refreshedAt)
    }

    // --- the week runs Saturday to Friday, and resets ------------------------------------------

    @Test
    fun startsTheFollowingWeekAtZeroForEveryone() = runTest {
        // The visible reset: a new Saturday brings the same members back at nothing.
        val repository = repository()
        val circle = repository.observeMyCircles().first().single()

        val next = repository.observeWeek(circle.id, nextSaturday).first()!!

        assertEquals(5, next.members.size)
        assertEquals(setOf(0), next.members.map { it.points }.toSet())
    }

    @Test
    fun hasNothingCachedForAnEarlierWeek() = runTest {
        // Nothing cached is not an error; it is simply nothing to show.
        val repository = repository()
        val circle = repository.observeMyCircles().first().single()

        assertNull(repository.observeWeek(circle.id, LocalDate(2026, 1, 3)).first())
    }

    @Test
    fun hasNothingCachedForACircleItDoesNotKnow() = runTest {
        assertNull(repository().observeWeek("no-such-circle", saturday).first())
    }

    // --- joining, creating, leaving, publishing ---------------------------------------------------

    @Test
    fun joinsTheSeededCircleByItsInviteCode() = runTest {
        val repository = repository()
        val seeded = repository.observeMyCircles().first().single()
        repository.leave(seeded.id)

        val joined = repository.join(seeded.inviteCode)

        assertEquals(seeded.id, joined.id)
        assertEquals(listOf(seeded.id), repository.observeMyCircles().first().map { it.id })
    }

    @Test
    fun rejectsAnInviteCodeNobodyIssued() = runTest {
        assertFailsWith<IllegalArgumentException> { repository().join("NOT-A-CODE") }
    }

    @Test
    fun leavesACircle() = runTest {
        val repository = repository()
        val circle = repository.observeMyCircles().first().single()

        repository.leave(circle.id)

        assertEquals(emptyList(), repository.observeMyCircles().first())
    }

    @Test
    fun createsACircleWithTheUserAsItsOnlyMember() = runTest {
        val repository = repository()

        val created = repository.create("حلقة العشاء")
        val week = repository.observeWeek(created.id, saturday).first()!!

        assertEquals(1, created.memberCount)
        assertEquals(listOf(true), week.members.map { it.isMe })
        assertEquals(2, repository.observeMyCircles().first().size)
    }

    @Test
    fun countsMyPublishedDaysAsMyTotalsForThatWeek() = runTest {
        // Once the user publishes, their row is what they published, not the seed.
        val repository = repository()
        val circle = repository.observeMyCircles().first().single()

        repository.publishDay(circle.id, LocalDate(2026, 1, 14), DayStats(points = 12, maxPoints = 15))
        repository.publishDay(circle.id, LocalDate(2026, 1, 15), DayStats(points = 15, maxPoints = 15))

        val me = repository.observeWeek(circle.id, saturday).first()!!.members.single { it.isMe }
        assertEquals(27, me.points)
        assertEquals(30, me.maxPoints)
    }

    @Test
    fun replacesADayPublishedTwiceRatherThanCountingItTwice() = runTest {
        val repository = repository()
        val circle = repository.observeMyCircles().first().single()
        val day = LocalDate(2026, 1, 15)

        repository.publishDay(circle.id, day, DayStats(points = 5, maxPoints = 15))
        repository.publishDay(circle.id, day, DayStats(points = 15, maxPoints = 15))

        val me = repository.observeWeek(circle.id, saturday).first()!!.members.single { it.isMe }
        assertEquals(15, me.points)
        assertEquals(15, me.maxPoints)
    }
}
