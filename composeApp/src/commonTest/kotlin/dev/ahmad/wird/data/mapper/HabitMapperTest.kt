package dev.ahmad.wird.data.mapper

import dev.ahmad.wird.data.local.HabitEntity
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The habit boundary is the awkward one. Storage keeps revisions keyed by a surrogate
 * `revisionId`; the domain knows only the stable `habitId` and calls it `id`. So the
 * mapping is not symmetric — going out to storage needs a revision identity the domain
 * does not carry.
 */
class HabitMapperTest {

    private val effectiveFrom = LocalDate(2026, 1, 10)
    private val retiredOn = LocalDate(2026, 1, 20)
    private val writtenAt = 1_768_000_000_000L

    private fun entity(
        kind: String = "BOOL",
        target: Int = 1,
        retiredOnEpochDay: Long? = null,
    ) = HabitEntity(
        revisionId = "rev-1",
        habitId = "duha",
        name = "صلاة الضحى",
        kind = kind,
        target = target,
        iconKey = "duha",
        sortOrder = 3,
        effectiveFromEpochDay = effectiveFrom.toEpochDays(),
        retiredOnEpochDay = retiredOnEpochDay,
        updatedAt = writtenAt,
    )

    private fun domain(
        kind: HabitKind = HabitKind.BOOL,
        target: Int = 1,
        retired: LocalDate? = null,
    ) = Habit(
        id = "duha",
        name = "صلاة الضحى",
        kind = kind,
        target = target,
        iconKey = "duha",
        sortOrder = 3,
        effectiveFrom = effectiveFrom,
        retiredOn = retired,
    )

    // --- entity to domain ------------------------------------------------------------

    @Test
    fun mapsTheStableHabitIdOntoTheDomainId() {
        // Not revisionId. The domain treats every revision of a habit as the same habit.
        assertEquals("duha", entity().toDomain().id)
    }

    @Test
    fun mapsEpochDaysBackToCalendarDates() {
        val mapped = entity(retiredOnEpochDay = retiredOn.toEpochDays()).toDomain()

        assertEquals(effectiveFrom, mapped.effectiveFrom)
        assertEquals(retiredOn, mapped.retiredOn)
    }

    @Test
    fun readsAnOpenRevisionAsNotRetired() {
        assertNull(entity(retiredOnEpochDay = null).toDomain().retiredOn)
    }

    @Test
    fun readsACounterWithItsTarget() {
        val mapped = entity(kind = "COUNTER", target = 5).toDomain()

        assertEquals(HabitKind.COUNTER, mapped.kind)
        assertEquals(5, mapped.target)
    }

    // --- unknown enum values ------------------------------------------------------------

    @Test
    fun readsAnUnknownKindWithATargetOfOneAsBool() {
        // A kind written by a newer version has to resolve to something. Inferring from
        // the target keeps the result self-consistent: Habit rejects a BOOL whose target
        // is not 1, so a blind fallback to BOOL would throw on exactly this data.
        val mapped = entity(kind = "SOMETHING_FROM_A_FUTURE_VERSION", target = 1).toDomain()

        assertEquals(HabitKind.BOOL, mapped.kind)
        assertEquals(1, mapped.target)
    }

    @Test
    fun readsAnUnknownKindWithABiggerTargetAsCounter() {
        val mapped = entity(kind = "SOMETHING_FROM_A_FUTURE_VERSION", target = 5).toDomain()

        assertEquals(HabitKind.COUNTER, mapped.kind)
        assertEquals(5, mapped.target)
    }

    // --- domain to entity ---------------------------------------------------------------

    @Test
    fun carriesTheGivenRevisionIdAndTimestampOutToStorage() {
        // The domain has neither, so both are supplied at the boundary.
        val mapped = domain().toEntity(revisionId = "rev-9", updatedAt = 42)

        assertEquals("rev-9", mapped.revisionId)
        assertEquals(42, mapped.updatedAt)
        assertEquals("duha", mapped.habitId)
    }

    @Test
    fun writesDatesAsEpochDays() {
        val mapped = domain(retired = retiredOn).toEntity(revisionId = "rev-9", updatedAt = 42)

        assertEquals(effectiveFrom.toEpochDays(), mapped.effectiveFromEpochDay)
        assertEquals(retiredOn.toEpochDays(), mapped.retiredOnEpochDay)
    }

    @Test
    fun writesAnUnretiredHabitAsNull() {
        assertNull(domain().toEntity(revisionId = "rev-9", updatedAt = 42).retiredOnEpochDay)
    }

    // --- round trips -----------------------------------------------------------------------

    @Test
    fun survivesAnEntityToDomainToEntityRoundTrip() {
        val original = entity(kind = "COUNTER", target = 5, retiredOnEpochDay = retiredOn.toEpochDays())

        val round = original.toDomain().toEntity(revisionId = original.revisionId, updatedAt = original.updatedAt)

        assertEquals(original, round)
    }

    @Test
    fun survivesADomainToEntityToDomainRoundTrip() {
        val original = domain(kind = HabitKind.COUNTER, target = 5, retired = retiredOn)

        assertEquals(original, original.toEntity(revisionId = "rev-1", updatedAt = writtenAt).toDomain())
    }
}
