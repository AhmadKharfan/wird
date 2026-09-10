package dev.ahmad.wird.domain.fake

import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.LocalDate

/**
 * A real in-memory [HabitRepository] holding effective-dated revisions, so tests can put a
 * habit's history in and read back what was live on a given day.
 *
 * Use [controls] to make it slow or make it fail. Revision handling mirrors the real
 * repository: an edit closes the open revision and opens a new one rather than mutating a
 * row, so a test that passes here means the same thing against Room.
 */
class FakeHabitRepository(
    initial: List<Habit> = emptyList(),
    val controls: FakeControls = FakeControls(),
) : HabitRepository {

    private val stored = MutableStateFlow(initial)

    /** Every revision currently held, for assertions. */
    val habits: List<Habit> get() = stored.value

    override fun observeActiveHabits(): Flow<List<Habit>> =
        stored.onStart { controls.gate() }
            .map { all -> all.filter { it.active }.sortedBy { it.sortOrder } }

    override fun observeHabitsOn(day: LocalDate): Flow<List<Habit>> =
        stored.onStart { controls.gate() }
            .map { all -> all.filter { it.isLiveOn(day) }.sortedBy { it.sortOrder } }

    override fun observeHabitsIn(from: LocalDate, to: LocalDate): Flow<List<Habit>> =
        stored.onStart { controls.gate() }
            .map { all ->
                all.filter { habit ->
                    // Overlaps the range: started by the last day, not retired before the first.
                    habit.effectiveFrom <= to && (habit.retiredOn == null || habit.retiredOn > from)
                }.sortedBy { it.sortOrder }
            }

    override suspend fun allRevisions(): List<Habit> {
        controls.gate()
        return stored.value
    }

    override suspend fun hasAnyHabit(): Boolean {
        controls.gate()
        return stored.value.isNotEmpty()
    }

    override suspend fun upsert(habit: Habit) {
        controls.gate()
        stored.value = stored.value.withUpserted(habit)
    }

    override suspend fun upsertAll(revisions: List<Habit>) {
        controls.gate()
        // One assignment, so a failure leaves nothing half-written — as the real one's
        // transaction does.
        stored.value = revisions.fold(stored.value) { all, habit -> all.withUpserted(habit) }
    }

    private fun List<Habit>.withUpserted(habit: Habit): List<Habit> {
        val previous = firstOrNull { it.id == habit.id && it.retiredOn == null }
        val closed = previous?.copy(retiredOn = habit.effectiveFrom)
        return filterNot { it === previous } + listOfNotNull(closed) + habit
    }

    override suspend fun reorder(habitIdsInOrder: List<String>) {
        controls.gate()
        stored.value = stored.value.map { habit ->
            val position = habitIdsInOrder.indexOf(habit.id)
            if (position < 0) habit else habit.copy(sortOrder = position)
        }
    }

    override suspend fun updateAppearance(habitId: String, name: String, iconKey: String) {
        require(name.isNotBlank()) { "habit name must not be blank" }
        controls.gate()
        stored.value = stored.value.map { habit ->
            if (habit.id == habitId) habit.copy(name = name, iconKey = iconKey) else habit
        }
    }

    override suspend fun setActive(habitId: String, active: Boolean, asOf: LocalDate) {
        controls.gate()
        if (!active) {
            stored.value = stored.value.map { habit ->
                if (habit.id == habitId && habit.retiredOn == null) habit.copy(retiredOn = asOf) else habit
            }
            return
        }

        // As the real repository: the newest revision comes back from asOf, and the days the
        // habit was off stay off.
        val latest = stored.value.filter { it.id == habitId }.maxByOrNull { it.effectiveFrom } ?: return
        val retiredOn = latest.retiredOn ?: return
        stored.value = if (asOf <= retiredOn) {
            stored.value.map { if (it === latest) it.copy(retiredOn = null) else it }
        } else {
            stored.value + latest.copy(effectiveFrom = asOf, retiredOn = null)
        }
    }
}
