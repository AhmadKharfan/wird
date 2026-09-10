package dev.ahmad.wird.domain.fake

import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.repository.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

/**
 * A real in-memory [HabitRepository] holding effective-dated revisions, so tests can put
 * a habit's history in and read back what was live on a given day.
 */
class FakeHabitRepository(initial: List<Habit> = emptyList()) : HabitRepository {

    private val stored = MutableStateFlow(initial)

    /** Every revision currently held, for assertions. */
    val habits: List<Habit> get() = stored.value

    override fun observeActiveHabits(): Flow<List<Habit>> =
        stored.map { all -> all.filter { it.active }.sortedBy { it.sortOrder } }

    override fun observeHabitsOn(day: LocalDate): Flow<List<Habit>> =
        stored.map { all -> all.filter { it.isLiveOn(day) }.sortedBy { it.sortOrder } }

    override suspend fun upsert(habit: Habit) {
        val previous = stored.value.firstOrNull { it.id == habit.id && it.retiredOn == null }
        val closed = previous?.copy(retiredOn = habit.effectiveFrom)
        stored.value = stored.value.filterNot { it === previous } + listOfNotNull(closed) + habit
    }

    override suspend fun reorder(habitIdsInOrder: List<String>) {
        stored.value = stored.value.map { habit ->
            val position = habitIdsInOrder.indexOf(habit.id)
            if (position < 0) habit else habit.copy(sortOrder = position)
        }
    }

    override suspend fun setActive(habitId: String, active: Boolean, asOf: LocalDate) {
        stored.value = stored.value.map { habit ->
            when {
                habit.id != habitId -> habit
                active -> habit.copy(retiredOn = null)
                habit.retiredOn == null -> habit.copy(retiredOn = asOf)
                else -> habit
            }
        }
    }
}
