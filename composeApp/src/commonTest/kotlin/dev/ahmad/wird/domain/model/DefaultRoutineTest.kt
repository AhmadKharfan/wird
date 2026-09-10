package dev.ahmad.wird.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The routine a fresh install starts with. Its shape is a product specification, so the
 * order and the daily maximum are asserted literally — a future edit to either should
 * have to come here and say so deliberately.
 */
class DefaultRoutineTest {

    private val seedDay = LocalDate(2026, 1, 15)

    @Test
    fun addsUpToFifteenPointsADay() {
        // Five prayers plus ten single-point habits. This is the number the whole
        // scoring surface is calibrated against.
        val snapshot = DaySnapshot(
            day = seedDay,
            habits = DefaultRoutine.habitsFrom(seedDay),
            entries = emptyList(),
        )

        assertEquals(15, snapshot.scheduledHabits.sumOf { it.target })
    }

    @Test
    fun listsElevenHabitsInTheSpecifiedOrder() {
        val names = DefaultRoutine.habitsFrom(seedDay).map { it.name }

        assertEquals(
            listOf(
                "الصلوات الخمس",
                "صلاة الضحى",
                "صلاة الوتر",
                "سنن الرواتب",
                "جزء قرآن",
                "أذكار الصباح",
                "أذكار المساء",
                "أذكار النوم",
                "أذكار الاستيقاظ",
                "سورة الملك",
                "مئة صلاة على النبي ﷺ",
            ),
            names,
        )
    }

    @Test
    fun countsTheFivePrayersAndNothingElse() {
        // The only counter in the routine, and the only habit worth more than a point.
        val counters = DefaultRoutine.habitsFrom(seedDay).filter { it.kind == HabitKind.COUNTER }

        assertEquals(1, counters.size)
        assertEquals("الصلوات الخمس", counters.single().name)
        assertEquals(5, counters.single().target)
    }

    @Test
    fun makesEveryOtherHabitWorthASinglePoint() {
        val bools = DefaultRoutine.habitsFrom(seedDay).filter { it.kind == HabitKind.BOOL }

        assertEquals(10, bools.size)
        assertTrue(bools.all { it.target == 1 })
    }

    @Test
    fun numbersSortOrderFromZeroWithoutGaps() {
        // DaySnapshot orders by sortOrder, so a duplicate or a gap would reorder or
        // destabilise the Today list.
        val sortOrders = DefaultRoutine.habitsFrom(seedDay).map { it.sortOrder }

        assertEquals(List(11) { it }, sortOrders)
    }

    @Test
    fun givesEveryHabitADistinctIdAndIcon() {
        val habits = DefaultRoutine.habitsFrom(seedDay)

        assertEquals(11, habits.map { it.id }.toSet().size)
        assertEquals(11, habits.map { it.iconKey }.toSet().size)
    }

    @Test
    fun startsEveryHabitOnTheDayItIsSeeded() {
        // Seeding on the 15th must not make the routine retroactively expected on the
        // 14th, which would show a fresh install a missed day it never had.
        val habits = DefaultRoutine.habitsFrom(seedDay)

        assertTrue(habits.all { it.effectiveFrom == seedDay })
        assertTrue(habits.all { it.retiredOn == null })
        assertTrue(habits.none { it.isLiveOn(LocalDate(2026, 1, 14)) })
    }
}
