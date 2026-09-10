package dev.ahmad.wird.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class EntryTest {

    private fun entry(
        value: Int = 1,
        id: String = "entry-1",
        habitId: String = "quran-juz",
    ) = Entry(
        id = id,
        habitId = habitId,
        day = LocalDate(2026, 1, 10),
        value = value,
        updatedAt = Instant.fromEpochSeconds(1_767_000_000),
    )

    @Test
    fun rejectsANegativeValue() {
        // A negative count would subtract from the day's points and could push a
        // ratio below zero.
        assertFailsWith<IllegalArgumentException> { entry(value = -1) }
    }

    @Test
    fun acceptsAValueOfZero() {
        // Untapping a habit records zero rather than deleting the row, so zero is
        // a legitimate stored value.
        assertEquals(0, entry(value = 0).value)
    }

    @Test
    fun rejectsABlankId() {
        assertFailsWith<IllegalArgumentException> { entry(id = " ") }
    }

    @Test
    fun rejectsABlankHabitId() {
        // An entry that belongs to no habit can never be scored, so it must not exist.
        assertFailsWith<IllegalArgumentException> { entry(habitId = "") }
    }
}
