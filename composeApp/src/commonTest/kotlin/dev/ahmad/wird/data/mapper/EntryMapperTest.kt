package dev.ahmad.wird.data.mapper

import dev.ahmad.wird.data.local.EntryEntity
import dev.ahmad.wird.domain.model.Entry
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class EntryMapperTest {

    private val day = LocalDate(2026, 1, 15)
    private val writtenAt = Instant.fromEpochMilliseconds(1_768_000_000_000)

    private val entity = EntryEntity(
        id = "entry-1",
        habitId = "prayers",
        epochDay = day.toEpochDays(),
        value = 3,
        updatedAt = writtenAt.toEpochMilliseconds(),
    )

    private val domain = Entry(
        id = "entry-1",
        habitId = "prayers",
        day = day,
        value = 3,
        updatedAt = writtenAt,
    )

    @Test
    fun mapsStoredEpochDaysBackToTheCalendarDay() {
        assertEquals(day, entity.toDomain().day)
    }

    @Test
    fun mapsStoredMillisecondsBackToAnInstant() {
        assertEquals(writtenAt, entity.toDomain().updatedAt)
    }

    @Test
    fun keepsTheStoredIdSoTheRowKeepsOneIdentity() {
        // The whole point of the client-generated key: the id that comes out is the id
        // that goes back in, so a row is never renamed by being edited.
        assertEquals("entry-1", entity.toDomain().id)
        assertEquals("entry-1", domain.toEntity().id)
    }

    @Test
    fun writesTheCalendarDayAsEpochDays() {
        assertEquals(day.toEpochDays(), domain.toEntity().epochDay)
    }

    @Test
    fun keepsAZeroValueRatherThanTreatingItAsAbsent() {
        // Zero is "explicitly not done" and must survive the boundary intact.
        assertEquals(0, domain.copy(value = 0).toEntity().value)
        assertEquals(0, entity.copy(value = 0).toDomain().value)
    }

    @Test
    fun survivesAnEntityToDomainToEntityRoundTrip() {
        assertEquals(entity, entity.toDomain().toEntity())
    }

    @Test
    fun survivesADomainToEntityToDomainRoundTrip() {
        assertEquals(domain, domain.toEntity().toDomain())
    }
}
