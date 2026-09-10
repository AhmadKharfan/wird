package dev.ahmad.wird.domain.fake

import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The fake has to rename the way the real repository does — across every revision, never
 * opening a new one — or a ViewModel test could pass here and fail against Room.
 */
class FakeHabitAppearanceTest {

    private fun revision(target: Int, from: LocalDate, retiredOn: LocalDate? = null) = Habit(
        id = "prayers",
        name = "الصلوات",
        kind = HabitKind.COUNTER,
        target = target,
        iconKey = "mosque",
        sortOrder = 0,
        effectiveFrom = from,
        retiredOn = retiredOn,
    )

    private val revisions = listOf(
        revision(target = 3, from = LocalDate(2026, 1, 10), retiredOn = LocalDate(2026, 1, 15)),
        revision(target = 5, from = LocalDate(2026, 1, 15)),
    )

    @Test
    fun renamesEveryRevisionWithoutAddingOne() = runTest {
        val habits = FakeHabitRepository(revisions)

        habits.updateAppearance("prayers", name = "الصلوات الخمس", iconKey = "crescent")

        assertEquals(2, habits.habits.size)
        assertEquals(setOf("الصلوات الخمس"), habits.habits.map { it.name }.toSet())
        assertEquals(setOf("crescent"), habits.habits.map { it.iconKey }.toSet())
        assertEquals(listOf(3, 5), habits.habits.map { it.target }.sorted())
    }

    @Test
    fun rejectsABlankNameLikeTheRealOne() = runTest {
        val habits = FakeHabitRepository(revisions)

        assertFailsWith<IllegalArgumentException> {
            habits.updateAppearance("prayers", name = "", iconKey = "mosque")
        }
    }

    @Test
    fun failsWhenToldTo() = runTest {
        val habits = FakeHabitRepository(revisions)
        habits.controls.failWith(IllegalStateException("disk is full"))

        assertFailsWith<IllegalStateException> {
            habits.updateAppearance("prayers", name = "الصلوات الخمس", iconKey = "mosque")
        }
        assertEquals(setOf("الصلوات"), habits.habits.map { it.name }.toSet())
    }
}
