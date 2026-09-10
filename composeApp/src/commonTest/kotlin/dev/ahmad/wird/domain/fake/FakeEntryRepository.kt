package dev.ahmad.wird.domain.fake

import dev.ahmad.wird.domain.model.Entry
import dev.ahmad.wird.domain.repository.EntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * A real in-memory [EntryRepository], not a mock: it stores what it is told and reads it
 * back, so tests assert on stored state rather than on calls having been made.
 *
 * Use [controls] to make it slow or make it fail. Like the real repository it keeps one
 * entry per habit per day, and an id that survives edits.
 */
class FakeEntryRepository(
    initial: List<Entry> = emptyList(),
    val controls: FakeControls = FakeControls(),
) : EntryRepository {

    private val stored = MutableStateFlow(initial)

    /** Everything currently held, for assertions. */
    val entries: List<Entry> get() = stored.value

    fun valueOf(habitId: String, day: LocalDate): Int =
        stored.value.firstOrNull { it.habitId == habitId && it.day == day }?.value ?: 0

    override fun observeDay(day: LocalDate): Flow<List<Entry>> =
        stored.onStart { controls.gate() }.map { all -> all.filter { it.day == day } }

    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<Entry>> =
        stored.onStart { controls.gate() }
            .map { all -> all.filter { it.day >= from && it.day <= to } }

    override suspend fun setValue(habitId: String, day: LocalDate, value: Int) {
        controls.gate()
        write(habitId, day, value)
    }

    override suspend fun toggle(habitId: String, day: LocalDate) {
        controls.gate()
        write(habitId, day, if (valueOf(habitId, day) == 0) 1 else 0)
    }

    private fun write(habitId: String, day: LocalDate, value: Int) {
        val existing = stored.value.firstOrNull { it.habitId == habitId && it.day == day }
        val entry = Entry(
            // Reuses the stored id, as the real repository does, so a test cannot pass
            // here and then fail against Room.
            id = existing?.id ?: "$habitId@$day",
            habitId = habitId,
            day = day,
            value = value,
            updatedAt = WRITTEN_AT,
        )
        stored.value = stored.value.filterNot { it.habitId == habitId && it.day == day } + entry
    }

    private companion object {
        /** Fixed so a written entry compares equal across runs. */
        val WRITTEN_AT = Instant.fromEpochSeconds(1_768_000_000)
    }
}
