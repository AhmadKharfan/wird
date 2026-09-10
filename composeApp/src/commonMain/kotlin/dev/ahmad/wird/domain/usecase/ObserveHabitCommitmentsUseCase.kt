package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitCommitment
import dev.ahmad.wird.domain.repository.EntryRepository
import dev.ahmad.wird.domain.repository.HabitRepository
import dev.ahmad.wird.domain.util.plusDays
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * How faithfully each habit was kept between two days, **weakest first**.
 *
 * The order is the product decision: the screen should read as a plan rather than a trophy
 * case, so the habit most in need of attention is the one at the top. Ties break on the
 * habit's own display order, so a routine where several habits sit at the same ratio does
 * not reshuffle itself between emissions.
 *
 * A habit that was revised inside the window has more than one revision live across it.
 * Each revision is measured over its own days and the results are added, so a target change
 * does not shorten the habit's history — and the [HabitCommitment.habit] reported is the
 * latest revision, because the bar is labelled with the target the user has now.
 */
class ObserveHabitCommitmentsUseCase(
    private val habits: HabitRepository,
    private val entries: EntryRepository,
) {
    operator fun invoke(from: LocalDate, to: LocalDate): Flow<List<HabitCommitment>> {
        require(from <= to) { "a window must not end before it starts: $from to $to" }

        val window = generateSequence(from) { it.plusDays(1) }.takeWhile { it <= to }.toList()

        return observeSnapshots(window, habits, entries).map { snapshots ->
            snapshots
                .flatMap { it.scheduledHabits }
                .groupBy { it.id }
                .map { (_, revisions) -> commitmentFor(revisions, snapshots) }
                .sortedWith(compareBy({ it.ratio }, { it.habit.sortOrder }))
        }
    }

    private fun commitmentFor(
        revisions: List<Habit>,
        snapshots: List<dev.ahmad.wird.domain.model.DaySnapshot>,
    ): HabitCommitment {
        val distinct = revisions.distinct()
        val current = distinct.maxBy { it.effectiveFrom }

        var elapsed = 0
        var completed = 0
        distinct.forEach { revision ->
            snapshots.filter { revision.isLiveOn(it.day) }.forEach { snapshot ->
                elapsed++
                if (snapshot.valueFor(revision.id) >= revision.target) completed++
            }
        }

        return HabitCommitment(habit = current, completedDays = completed, elapsedDays = elapsed)
    }
}
