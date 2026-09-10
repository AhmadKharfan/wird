package dev.ahmad.wird.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A habit is effective-dated. That is the single mechanism behind two product rules:
 * changing a target must not rescore past days, and deactivating a habit must drop it
 * from today's maximum while leaving its history intact. Both reduce to [Habit.isLiveOn].
 */
class HabitTest {

    private val jan10 = LocalDate(2026, 1, 10)
    private val jan15 = LocalDate(2026, 1, 15)
    private val jan20 = LocalDate(2026, 1, 20)

    private fun habit(
        effectiveFrom: LocalDate = jan10,
        retiredOn: LocalDate? = null,
        kind: HabitKind = HabitKind.BOOL,
        target: Int = 1,
        name: String = "جزء قرآن",
        sortOrder: Int = 0,
        id: String = "quran-juz",
    ) = Habit(
        id = id,
        name = name,
        kind = kind,
        target = target,
        iconKey = "book",
        sortOrder = sortOrder,
        effectiveFrom = effectiveFrom,
        retiredOn = retiredOn,
    )

    // --- isLiveOn: the lower bound ------------------------------------------------

    @Test
    fun isNotLiveTheDayBeforeItStarts() {
        // Breaks if effectiveFrom is ignored: a habit added today would inflate the
        // maximum of every day that came before it.
        assertFalse(habit(effectiveFrom = jan10).isLiveOn(LocalDate(2026, 1, 9)))
    }

    @Test
    fun isLiveOnTheDayItStarts() {
        // Breaks on a `>` instead of `>=`: the first day would be unscoreable.
        assertTrue(habit(effectiveFrom = jan10).isLiveOn(jan10))
    }

    @Test
    fun isLiveLongAfterItStartsWhenNeverRetired() {
        assertTrue(habit(effectiveFrom = jan10, retiredOn = null).isLiveOn(LocalDate(2030, 6, 1)))
    }

    // --- isLiveOn: the upper bound, which is the deactivation rule -----------------

    @Test
    fun isNotLiveOnTheDayItIsRetired() {
        // retiredOn is exclusive: deactivating today must remove the habit from
        // *today's* maximum, not from tomorrow's.
        assertFalse(habit(effectiveFrom = jan10, retiredOn = jan15).isLiveOn(jan15))
    }

    @Test
    fun isStillLiveTheDayBeforeItIsRetired() {
        // Breaks if retirement is applied retroactively: past days would silently
        // lose the habit from their maximum and every old ratio would jump.
        assertTrue(habit(effectiveFrom = jan10, retiredOn = jan15).isLiveOn(LocalDate(2026, 1, 14)))
    }

    @Test
    fun isNotLiveLongAfterItIsRetired() {
        assertFalse(habit(effectiveFrom = jan10, retiredOn = jan15).isLiveOn(jan20))
    }

    @Test
    fun isLiveOnNoDayAtAllWhenRetiredOnTheDayItStarted() {
        val sameDay = habit(effectiveFrom = jan10, retiredOn = jan10)

        assertFalse(sameDay.isLiveOn(LocalDate(2026, 1, 9)))
        assertFalse(sameDay.isLiveOn(jan10))
        assertFalse(sameDay.isLiveOn(jan15))
    }

    // --- active is derived, so it can never disagree with retiredOn ----------------

    @Test
    fun isActiveWhileNotRetired() {
        assertTrue(habit(retiredOn = null).active)
    }

    @Test
    fun isInactiveOnceRetired() {
        assertFalse(habit(retiredOn = jan15).active)
    }

    // --- construction invariants ---------------------------------------------------

    @Test
    fun rejectsABlankName() {
        assertFailsWith<IllegalArgumentException> { habit(name = "   ") }
    }

    @Test
    fun rejectsABlankId() {
        assertFailsWith<IllegalArgumentException> { habit(id = "  ") }
    }

    @Test
    fun rejectsATargetBelowOne() {
        assertFailsWith<IllegalArgumentException> { habit(kind = HabitKind.COUNTER, target = 0) }
    }

    @Test
    fun rejectsABoolHabitWhoseTargetIsNotOne() {
        // A BOOL habit is done or not done; a target of 5 would make it worth five
        // points and quietly reweight the whole day.
        assertFailsWith<IllegalArgumentException> { habit(kind = HabitKind.BOOL, target = 5) }
    }

    @Test
    fun rejectsANegativeSortOrder() {
        assertFailsWith<IllegalArgumentException> { habit(sortOrder = -1) }
    }

    @Test
    fun rejectsRetirementBeforeItStarted() {
        assertFailsWith<IllegalArgumentException> {
            habit(effectiveFrom = jan15, retiredOn = jan10)
        }
    }

    @Test
    fun acceptsACounterHabitWithATargetAboveOne() {
        assertEquals(5, habit(kind = HabitKind.COUNTER, target = 5).target)
    }
}
