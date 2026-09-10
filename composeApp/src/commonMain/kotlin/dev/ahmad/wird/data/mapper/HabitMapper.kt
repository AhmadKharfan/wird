package dev.ahmad.wird.data.mapper

import dev.ahmad.wird.data.local.HabitEntity
import dev.ahmad.wird.domain.model.Habit
import dev.ahmad.wird.domain.model.HabitKind
import kotlinx.datetime.LocalDate

/** entity -> domain, preserving the stable habit identity across revisions. */
fun HabitEntity.toDomain(): Habit {
    // Inferring from target avoids constructing an invalid BOOL habit when newer data has an unknown kind.
    val mappedKind = runCatching { HabitKind.valueOf(kind) }
        .getOrDefault(if (target == 1) HabitKind.BOOL else HabitKind.COUNTER)

    return Habit(
        // All revisions represent one domain habit, so the stable habitId is used instead of revisionId.
        id = habitId,
        name = name,
        kind = mappedKind,
        target = target,
        iconKey = iconKey,
        sortOrder = sortOrder,
        effectiveFrom = LocalDate.fromEpochDays(effectiveFromEpochDay),
        retiredOn = retiredOnEpochDay?.let(LocalDate::fromEpochDays),
    )
}

/** domain -> entity, with storage-only revision metadata supplied by the caller. */
fun Habit.toEntity(revisionId: String, updatedAt: Long): HabitEntity = HabitEntity(
    revisionId = revisionId,
    habitId = id,
    name = name,
    kind = kind.name,
    target = target,
    iconKey = iconKey,
    sortOrder = sortOrder,
    effectiveFromEpochDay = effectiveFrom.toEpochDays(),
    retiredOnEpochDay = retiredOn?.toEpochDays(),
    updatedAt = updatedAt,
)
