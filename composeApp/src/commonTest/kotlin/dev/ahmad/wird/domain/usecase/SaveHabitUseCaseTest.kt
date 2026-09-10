package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeHabitRepository
import dev.ahmad.wird.domain.model.HabitDraft
import dev.ahmad.wird.domain.model.HabitKind
import dev.ahmad.wird.domain.usecase.HistoryFixtures.habit
import dev.ahmad.wird.domain.usecase.HistoryFixtures.today
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Saving is what the habit edit sheet calls, and it is where the two directions of a habit
 * edit meet. Kind and target score, so they change **from today** and never reach back.
 * Name and icon are presentational, so they change **everywhere**. An edit that mixed the
 * two up would either rescore the past or leave history under a name the user abandoned.
 */
class SaveHabitUseCaseTest {

    private val yesterday = LocalDate(2026, 1, 14)
    private val longAgo = LocalDate(2026, 1, 1)

    private var nextId = 0
    private val ids = { "new-${nextId++}" }

    private fun useCase(habits: FakeHabitRepository) =
        SaveHabitUseCase(habits, HistoryFixtures.clock, HistoryFixtures.zone, ids)

    private fun newDraft(
        name: String = "ورد الليل",
        kind: HabitKind = HabitKind.BOOL,
        target: Int = 1,
        iconKey: String = "moon",
    ) = HabitDraft(id = null, name = name, kind = kind, target = target, iconKey = iconKey)

    /** A counter that has existed since the 1st with a target of 3. */
    private fun existingCounter() = habit(
        id = "prayers",
        kind = HabitKind.COUNTER,
        target = 3,
        effectiveFrom = longAgo,
    )

    private fun editOf(
        id: String = "prayers",
        name: String = "prayers",
        kind: HabitKind = HabitKind.COUNTER,
        target: Int = 3,
        iconKey: String = "prayers",
    ) = HabitDraft(id = id, name = name, kind = kind, target = target, iconKey = iconKey)

    // --- adding ---------------------------------------------------------------------------

    @Test
    fun addsANewHabitUnderAFreshIdAndReturnsIt() = runTest {
        val habits = FakeHabitRepository()

        val id = useCase(habits)(newDraft())

        assertEquals("new-0", id)
        assertEquals(listOf("new-0"), habits.habits.map { it.id })
    }

    @Test
    fun startsANewHabitTodayRatherThanInThePast() = runTest {
        // Starting it any earlier would show a fresh habit a history of missed days.
        val habits = FakeHabitRepository()

        useCase(habits)(newDraft())

        assertEquals(1, habits.observeHabitsOn(today).first().size)
        assertEquals(emptyList(), habits.observeHabitsOn(yesterday).first())
    }

    @Test
    fun appendsANewHabitAfterTheExistingOnes() = runTest {
        val habits = FakeHabitRepository(
            listOf(habit("a", sortOrder = 0), habit("b", sortOrder = 1)),
        )

        useCase(habits)(newDraft())

        assertEquals(listOf("a", "b", "new-0"), habits.observeActiveHabits().first().map { it.id })
    }

    @Test
    fun givesTheVeryFirstHabitTheFirstPlace() = runTest {
        val habits = FakeHabitRepository()

        useCase(habits)(newDraft())

        assertEquals(0, habits.habits.single().sortOrder)
    }

    @Test
    fun trimsSurroundingWhitespaceFromTheName() = runTest {
        val habits = FakeHabitRepository()

        useCase(habits)(newDraft(name = "  ورد الليل  "))

        assertEquals("ورد الليل", habits.habits.single().name)
    }

    @Test
    fun rejectsABlankNameAndWritesNothing() = runTest {
        val habits = FakeHabitRepository()

        assertFailsWith<IllegalArgumentException> { useCase(habits)(newDraft(name = "   ")) }

        assertEquals(emptyList(), habits.habits)
    }

    @Test
    fun forcesABoolHabitsTargetToOne() = runTest {
        // Switching the sheet from counter to done/not-done can leave the old counter
        // target behind. A BOOL worth five points would quietly reweight the whole day.
        val habits = FakeHabitRepository()

        useCase(habits)(newDraft(kind = HabitKind.BOOL, target = 5))

        assertEquals(1, habits.habits.single().target)
    }

    @Test
    fun keepsACountersTarget() = runTest {
        val habits = FakeHabitRepository()

        useCase(habits)(newDraft(kind = HabitKind.COUNTER, target = 33))

        assertEquals(33, habits.habits.single().target)
    }

    @Test
    fun rejectsACounterTargetBelowOneAndWritesNothing() = runTest {
        val habits = FakeHabitRepository()

        assertFailsWith<IllegalArgumentException> {
            useCase(habits)(newDraft(kind = HabitKind.COUNTER, target = 0))
        }

        assertEquals(emptyList(), habits.habits)
    }

    // --- editing what scores: from today onward -------------------------------------------

    @Test
    fun changesATargetFromTodayAndLeavesThePastAlone() = runTest {
        val habits = FakeHabitRepository(listOf(existingCounter()))

        useCase(habits)(editOf(target = 5))

        assertEquals(3, habits.observeHabitsOn(yesterday).first().single().target)
        assertEquals(5, habits.observeHabitsOn(today).first().single().target)
    }

    @Test
    fun changesAKindFromTodayAndLeavesThePastAlone() = runTest {
        val habits = FakeHabitRepository(listOf(habit("duha", effectiveFrom = longAgo)))

        useCase(habits)(editOf(id = "duha", name = "duha", kind = HabitKind.COUNTER, target = 4, iconKey = "duha"))

        assertEquals(HabitKind.BOOL, habits.observeHabitsOn(yesterday).first().single().kind)
        assertEquals(HabitKind.COUNTER, habits.observeHabitsOn(today).first().single().kind)
    }

    @Test
    fun keepsItsPlaceInTheListWhenItsTargetChanges() = runTest {
        val habits = FakeHabitRepository(
            listOf(habit("first", sortOrder = 0), existingCounter().copy(sortOrder = 1)),
        )

        useCase(habits)(editOf(target = 5))

        assertEquals(listOf("first", "prayers"), habits.observeActiveHabits().first().map { it.id })
    }

    @Test
    fun leavesTodayOnTheLatestTargetWhenEditedTwiceInOneDay() = runTest {
        val habits = FakeHabitRepository(listOf(existingCounter()))
        val save = useCase(habits)

        save(editOf(target = 5))
        save(editOf(target = 7))

        assertEquals(7, habits.observeHabitsOn(today).first().single().target)
        assertEquals(3, habits.observeHabitsOn(yesterday).first().single().target)
    }

    // --- editing what does not score: everywhere ---------------------------------------------

    @Test
    fun renamesAcrossHistoryWithoutOpeningARevision() = runTest {
        val habits = FakeHabitRepository(listOf(existingCounter()))

        useCase(habits)(editOf(name = "الصلوات الخمس"))

        assertEquals(1, habits.habits.size)
        assertEquals("الصلوات الخمس", habits.observeHabitsOn(yesterday).first().single().name)
    }

    @Test
    fun appliesARenameEverywhereAndATargetChangeFromTodayInOneSave() = runTest {
        val habits = FakeHabitRepository(listOf(existingCounter()))

        useCase(habits)(editOf(name = "الصلوات الخمس", target = 5))

        val past = habits.observeHabitsOn(yesterday).first().single()
        val now = habits.observeHabitsOn(today).first().single()
        assertEquals("الصلوات الخمس", past.name)
        assertEquals(3, past.target)
        assertEquals("الصلوات الخمس", now.name)
        assertEquals(5, now.target)
    }

    @Test
    fun writesNothingWhenNothingChanged() = runTest {
        // Saving an untouched sheet must not mint a revision; history would fill with
        // empty edits.
        val habits = FakeHabitRepository(listOf(existingCounter()))

        useCase(habits)(editOf())

        assertEquals(listOf(existingCounter()), habits.habits)
    }

    @Test
    fun returnsTheIdOfTheHabitItEdited() = runTest {
        val habits = FakeHabitRepository(listOf(existingCounter()))

        assertEquals("prayers", useCase(habits)(editOf(target = 5)))
    }

    @Test
    fun rejectsEditingAHabitThatIsNoLongerActive() = runTest {
        val habits = FakeHabitRepository(listOf(existingCounter().copy(retiredOn = yesterday)))

        assertFailsWith<IllegalArgumentException> { useCase(habits)(editOf(target = 5)) }

        assertTrue(habits.habits.all { it.target == 3 })
    }

    @Test
    fun editsAHabitThatStartsAfterTodayWithoutMakingItStartEarlier() = runTest {
        // Flying west moves today backwards, so a habit created "today" before the flight can
        // start after the new today. Opening the edit from today would begin the new revision
        // before the one it replaces — and the replaced one would end before it began.
        val tomorrow = LocalDate(2026, 1, 16)
        val habits = FakeHabitRepository(listOf(existingCounter().copy(effectiveFrom = tomorrow)))

        useCase(habits)(editOf(target = 5))

        assertEquals(emptyList(), habits.observeHabitsOn(today).first())
        assertEquals(5, habits.observeHabitsOn(tomorrow).first().single().target)
    }
}
