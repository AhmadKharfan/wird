package dev.ahmad.wird.domain.fake

import dev.ahmad.wird.domain.model.Entry
import dev.ahmad.wird.domain.repository.EntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * A real in-memory [EntryRepository], not a mock: it stores what it is told and reads it
 * back, so tests assert on stored state rather than on calls having been made.
 */
class FakeEntryRepository(initial: List<Entry> = emptyList()) : EntryRepository {

    private val stored = MutableStateFlow(initial)

    /** Everything currently held, for assertions. */
    val entries: List<Entry> get() = stored.value

    fun valueOf(habitId: String, day: LocalDate): Int =
        stored.value.firstOrNull { it.habitId == habitId && it.day == day }?.value ?: 0

    override fun observeDay(day: LocalDate): Flow<List<Entry>> =
        stored.map { all -> all.filter { it.day == day } }

    override fun observeRange(from: LocalDate, to: LocalDate): Flow<List<Entry>> =
        stored.map { all -> all.filter { it.day >= from && it.day <= to } }

    override suspend fun setValue(habitId: String, day: LocalDate, value: Int) {
        write(habitId, day, value)
    }

    override suspend fun toggle(habitId: String, day: LocalDate) {
        write(habitId, day, if (valueOf(habitId, day) == 0) 1 else 0)
    }

    private fun write(habitId: String, day: LocalDate, value: Int) {
        val entry = Entry(
            id = "$habitId@$day",
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
