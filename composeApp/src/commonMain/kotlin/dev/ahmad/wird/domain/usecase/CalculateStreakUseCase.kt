package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.StreakInfo
import dev.ahmad.wird.domain.util.minusDays
import dev.ahmad.wird.domain.util.plusDays
import kotlinx.datetime.LocalDate

/**
 * Works out the run of complete days ending now, and the best run on record.
 *
 * It takes the set of days that were completed rather than the days themselves, because
 * deciding whether a day was complete is already
 * [CalculateDayStatsUseCase]'s job — doing it again here would be a second definition of
 * "finished" that could drift from the first.
 *
 * The rule that shapes everything below: **an unfinished today does not break the run.**
 * The day is not over, so the walk anchors at yesterday instead, and only a second
 * unfinished day actually ends the streak. That is why [current] cannot simply be read off
 * the end of the longest run.
 */
class CalculateStreakUseCase {

    operator fun invoke(today: LocalDate, completeDays: Set<LocalDate>): StreakInfo {
        if (completeDays.isEmpty()) return StreakInfo.NONE

        return StreakInfo(
            current = currentRun(today, completeDays),
            longest = longestRun(completeDays),
            lastCompleteDay = completeDays.max(),
        )
    }

    /**
     * Walks backwards from today, or from yesterday when today is not finished yet.
     *
     * Because the walk only ever goes backwards, a day recorded *ahead* of today — which
     * happens when the user travels west and today moves backwards under them — is never
     * visited, and so cannot inflate the run they are currently on.
     */
    private fun currentRun(today: LocalDate, completeDays: Set<LocalDate>): Int {
        var cursor = if (today in completeDays) today else today.minusDays(1)
        var run = 0
        while (cursor in completeDays) {
            run++
            cursor = cursor.minusDays(1)
        }
        return run
    }

    /**
     * The longest stretch of consecutive days in the set.
     *
     * Days ahead of today are counted here: they were genuinely completed, and a record
     * should not disappear because the user changed timezone.
     */
    private fun longestRun(completeDays: Set<LocalDate>): Int {
        val ordered = completeDays.sorted()
        var longest = 1
        var run = 1
        for (index in 1 until ordered.size) {
            run = if (ordered[index - 1].plusDays(1) == ordered[index]) run + 1 else 1
            if (run > longest) longest = run
        }
        return longest
    }
}
