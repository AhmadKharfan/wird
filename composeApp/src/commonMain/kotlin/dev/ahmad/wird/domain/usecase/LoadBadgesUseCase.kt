package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.Badge
import dev.ahmad.wird.domain.model.DaySnapshot
import dev.ahmad.wird.domain.repository.EntryRepository
import dev.ahmad.wird.domain.repository.HabitRepository
import dev.ahmad.wird.domain.util.plusDays
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Reads the whole local history and works out every badge from it.
 *
 * History runs from the day the first habit began up to **today**, one snapshot per calendar
 * day. Days recorded ahead of today — which flying west can leave behind — are not history
 * yet, so they cannot earn anything. Each snapshot is handed every revision and lets itself
 * narrow them to the ones live that day, as every period view does.
 */
class LoadBadgesUseCase(
    private val habits: HabitRepository,
    private val entries: EntryRepository,
    private val clock: Clock,
    private val zone: TimeZone,
    private val calculateBadges: CalculateBadgesUseCase,
) {
    suspend operator fun invoke(): List<Badge> {
        val today = clock.now().toLocalDateTime(zone).date
        val revisions = habits.allRevisions()
        val first = revisions.minOfOrNull { it.effectiveFrom }
        if (first == null || first > today) return calculateBadges(emptyList())

        val recorded = entries.allEntries().groupBy { it.day }
        val history = generateSequence(first) { it.plusDays(1) }
            .takeWhile { it <= today }
            .map { day -> DaySnapshot(day = day, habits = revisions, entries = recorded[day].orEmpty()) }
            .toList()

        return calculateBadges(history)
    }
}
