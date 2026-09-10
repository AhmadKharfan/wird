package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.Badge
import dev.ahmad.wird.domain.model.BadgeKind
import dev.ahmad.wird.domain.model.DaySnapshot
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import dev.ahmad.wird.domain.usecase.HistoryFixtures.entry
import dev.ahmad.wird.domain.usecase.HistoryFixtures.habit
import dev.ahmad.wird.domain.util.plusDays
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Badges are permanent once earned and dated to the day they were earned, all computed from
 * local history. Saturday 10 January 2026 opens the week that ends Friday the 16th; 1 January
 * is a Thursday.
 */
class CalculateBadgesUseCaseTest {

    private val calculate = CalculateBadgesUseCase()

    private val jan1 = LocalDate(2026, 1, 1)
    private val saturday = LocalDate(2026, 1, 10)
    private val friday = LocalDate(2026, 1, 16)

    private val duha = habit("duha", effectiveFrom = LocalDate(2025, 1, 1))
    private val prayers = habit("five-prayers", HabitKind.COUNTER, target = 5, sortOrder = 1, effectiveFrom = LocalDate(2025, 1, 1))

    private fun days(from: LocalDate, count: Int): List<LocalDate> = List(count) { from.plusDays(it) }

    /** One snapshot per day, recording [valueOf] for every habit live that day (zero is left out). */
    private fun history(
        habits: List<Habit>,
        days: List<LocalDate>,
        valueOf: (Habit, LocalDate) -> Int,
    ): List<DaySnapshot> = days.map { day ->
        DaySnapshot(
            day = day,
            habits = habits,
            entries = habits
                .filter { it.isLiveOn(day) }
                .mapNotNull { habit -> valueOf(habit, day).takeIf { it > 0 }?.let { entry(habit.id, day, it) } },
        )
    }

    /** Duha done on exactly [done] out of [days]. */
    private fun duhaOn(days: List<LocalDate>, done: Set<LocalDate>) =
        history(listOf(duha), days) { _, day -> if (day in done) 1 else 0 }

    private fun List<Badge>.earnedOn(kind: BadgeKind): LocalDate? = single { it.kind == kind }.earnedOn

    // --- streak milestones ------------------------------------------------------------------

    @Test
    fun earnsTheSevenDayStreakOnTheSeventhCompleteDay() {
        val week = days(jan1, 7)

        assertEquals(LocalDate(2026, 1, 7), calculate(duhaOn(week, week.toSet())).earnedOn(BadgeKind.STREAK_7))
    }

    @Test
    fun doesNotEarnTheSevenDayStreakAfterSix() {
        val six = days(jan1, 6)

        assertNull(calculate(duhaOn(six, six.toSet())).earnedOn(BadgeKind.STREAK_7))
    }

    @Test
    fun keepsAStreakBadgeAfterTheRunBreaks() {
        // Earned is earned; missing the days after does not take it back.
        val span = days(jan1, 10)

        assertEquals(LocalDate(2026, 1, 7), calculate(duhaOn(span, days(jan1, 7).toSet())).earnedOn(BadgeKind.STREAK_7))
    }

    @Test
    fun earnsEachMilestoneOnTheDayTheRunReachesIt() {
        // Day 100 from 1 January is 10 April: January ends on day 31, February on 59, March on 90.
        val hundred = days(jan1, 100)
        val badges = calculate(duhaOn(hundred, hundred.toSet()))

        assertEquals(LocalDate(2026, 1, 7), badges.earnedOn(BadgeKind.STREAK_7))
        assertEquals(LocalDate(2026, 1, 30), badges.earnedOn(BadgeKind.STREAK_30))
        assertEquals(LocalDate(2026, 4, 10), badges.earnedOn(BadgeKind.STREAK_100))
    }

    @Test
    fun startsCountingAgainAfterAMissedDay() {
        // Six, a miss, six: never seven in a row.
        val span = days(jan1, 13)
        val done = span.toSet() - LocalDate(2026, 1, 7)

        assertNull(calculate(duhaOn(span, done)).earnedOn(BadgeKind.STREAK_7))
    }

    @Test
    fun breaksARunAtAGapInHistory() {
        // A day missing from history is a day nothing is known about, so no run can pass
        // through it. Three days, a missing day, four days: never seven in a row, even though
        // every day that is present was complete.
        val present = days(jan1, 8) - LocalDate(2026, 1, 4)

        assertNull(calculate(duhaOn(present, present.toSet())).earnedOn(BadgeKind.STREAK_7))
    }

    @Test
    fun doesNotCountDaysBeforeTheRoutineExistedAsComplete() {
        // Duha starts on the 4th. The three days before it had nothing to complete, so the
        // run is seven days from the 4th, earned on the 10th.
        val late = habit("duha", effectiveFrom = LocalDate(2026, 1, 4))
        val span = days(jan1, 10)

        val badges = calculate(history(listOf(late), span) { habit, day -> if (habit.isLiveOn(day)) 1 else 0 })

        assertEquals(LocalDate(2026, 1, 10), badges.earnedOn(BadgeKind.STREAK_7))
    }

    // --- perfect weeks ---------------------------------------------------------------------------

    @Test
    fun earnsAPerfectWeekOnTheFridayOfACompleteSaturdayWeek() {
        val week = days(saturday, 7)

        assertEquals(friday, calculate(duhaOn(week, week.toSet())).earnedOn(BadgeKind.PERFECT_WEEK))
    }

    @Test
    fun doesNotCallSevenDaysAcrossTwoWeeksAPerfectWeek() {
        // Wednesday the 14th to Tuesday the 20th is seven complete days — a seven-day streak,
        // but no Saturday-to-Friday week is whole.
        val span = days(saturday, 11)
        val done = days(LocalDate(2026, 1, 14), 7).toSet()
        val badges = calculate(duhaOn(span, done))

        assertNull(badges.earnedOn(BadgeKind.PERFECT_WEEK))
        assertEquals(LocalDate(2026, 1, 20), badges.earnedOn(BadgeKind.STREAK_7))
    }

    @Test
    fun doesNotCallAWeekStillInProgressPerfect() {
        // Saturday to Thursday complete, and history ends on Thursday: Friday has not happened.
        val soFar = days(saturday, 6)

        assertNull(calculate(duhaOn(soFar, soFar.toSet())).earnedOn(BadgeKind.PERFECT_WEEK))
    }

    // --- habit consistency -------------------------------------------------------------------------

    private val fromJan1 = habit("duha", effectiveFrom = jan1)

    private fun consistency(badges: List<Badge>): Map<String?, LocalDate?> =
        badges.filter { it.kind == BadgeKind.HABIT_CONSISTENCY }.associate { it.habitId to it.earnedOn }

    @Test
    fun earnsConsistencyWhenAHabitIsKeptOnNinetyPercentOfThirtyDays() {
        // 27 of 30 is exactly 90%.
        val month = days(jan1, 30)
        val missed = setOf(LocalDate(2026, 1, 5), LocalDate(2026, 1, 15), LocalDate(2026, 1, 25))

        val badges = calculate(history(listOf(fromJan1), month) { _, day -> if (day in missed) 0 else 1 })

        assertEquals(mapOf<String?, LocalDate?>("duha" to LocalDate(2026, 1, 30)), consistency(badges))
    }

    @Test
    fun doesNotEarnConsistencyJustUnderNinetyPercent() {
        // 26 of 30 is 86.7%.
        val month = days(jan1, 30)
        val missed = setOf(5, 12, 19, 26).map { LocalDate(2026, 1, it) }.toSet()

        val badges = calculate(history(listOf(fromJan1), month) { _, day -> if (day in missed) 0 else 1 })

        assertEquals(mapOf<String?, LocalDate?>("duha" to null), consistency(badges))
    }

    @Test
    fun needsAFullThirtyDaysOfAHabitsLife() {
        // Ten out of ten is perfect, but it is not thirty days of consistency.
        val ten = days(jan1, 10)

        val badges = calculate(history(listOf(fromJan1), ten) { _, _ -> 1 })

        assertEquals(mapOf<String?, LocalDate?>("duha" to null), consistency(badges))
    }

    @Test
    fun needsThirtyDaysInARowOfAHabitsLife() {
        // Deactivated on the 21st and back from the 26th: twenty live days, a stretch when the
        // habit did not exist, then eleven more. Thirty-one live days in all, every one kept,
        // but never thirty in a row — so no thirty-day consistency yet.
        val before = habit("duha", effectiveFrom = jan1, retiredOn = LocalDate(2026, 1, 21))
        val after = habit("duha", effectiveFrom = LocalDate(2026, 1, 26))
        val throughFeb5 = days(jan1, 36)

        val badges = calculate(history(listOf(before, after), throughFeb5) { habit, day -> if (habit.isLiveOn(day)) 1 else 0 })

        assertEquals(mapOf<String?, LocalDate?>("duha" to null), consistency(badges))
    }

    @Test
    fun judgesConsistencyHabitByHabit() {
        val a = habit("a", effectiveFrom = jan1)
        val b = habit("b", sortOrder = 1, effectiveFrom = jan1)
        val month = days(jan1, 30)

        val badges = calculate(
            history(listOf(a, b), month) { habit, day ->
                val n = day.day
                when (habit.id) {
                    "a" -> if (n % 10 == 0) 0 else 1   // missed 10, 20, 30: 27 of 30
                    else -> if (n % 3 == 0) 0 else 1   // missed every third day: 20 of 30
                }
            },
        )

        assertEquals(mapOf<String?, LocalDate?>("a" to LocalDate(2026, 1, 30), "b" to null), consistency(badges))
    }

    @Test
    fun keepsConsistencyEarnedInAnEarlierStretch() {
        // A perfect January, then nothing in February: the badge from 30 January stays.
        val twoMonths = days(jan1, 59)

        val badges = calculate(history(listOf(fromJan1), twoMonths) { _, day -> if (day.month == Month.JANUARY) 1 else 0 })

        assertEquals(mapOf<String?, LocalDate?>("duha" to LocalDate(2026, 1, 30)), consistency(badges))
    }

    // --- five prayers ----------------------------------------------------------------------------

    @Test
    fun earnsFivePrayersWhenAllFiveAreKeptEveryDayOfASaturdayWeek() {
        val week = days(saturday, 7)

        val badges = calculate(history(listOf(prayers), week) { _, _ -> 5 })

        assertEquals(friday, badges.earnedOn(BadgeKind.FIVE_PRAYERS_WEEK))
    }

    @Test
    fun doesNotEarnFivePrayersIfOneDayHadFour() {
        val week = days(saturday, 7)

        val badges = calculate(history(listOf(prayers), week) { _, day -> if (day == LocalDate(2026, 1, 13)) 4 else 5 })

        assertNull(badges.earnedOn(BadgeKind.FIVE_PRAYERS_WEEK))
    }

    @Test
    fun countsTheFivePrayersEvenWhenTheRestOfTheRoutineIsMissed() {
        // Five prayers is its own achievement; it does not need a perfect day.
        val week = days(saturday, 7)

        val badges = calculate(history(listOf(prayers, duha), week) { habit, _ -> if (habit.id == "five-prayers") 5 else 0 })

        assertEquals(friday, badges.earnedOn(BadgeKind.FIVE_PRAYERS_WEEK))
        assertNull(badges.earnedOn(BadgeKind.PERFECT_WEEK))
    }

    @Test
    fun neverEarnsFivePrayersWithoutTheFivePrayersHabit() {
        val week = days(saturday, 7)

        assertNull(calculate(duhaOn(week, week.toSet())).earnedOn(BadgeKind.FIVE_PRAYERS_WEEK))
    }

    // --- the list itself -----------------------------------------------------------------------------

    @Test
    fun listsEveryRoutineBadgeLockedBeforeAnythingIsEarned() {
        val badges = calculate(emptyList())

        assertEquals(
            listOf(
                BadgeKind.STREAK_7,
                BadgeKind.STREAK_30,
                BadgeKind.STREAK_100,
                BadgeKind.PERFECT_WEEK,
                BadgeKind.FIVE_PRAYERS_WEEK,
            ),
            badges.map { it.kind },
        )
        assertEquals(setOf<LocalDate?>(null), badges.map { it.earnedOn }.toSet())
    }

    @Test
    fun readsHistoryInWhateverOrderItArrives() {
        val week = days(saturday, 7)
        val ordered = duhaOn(week, week.toSet())

        assertEquals(calculate(ordered), calculate(ordered.reversed()))
    }
}
