package dev.ahmad.wird.data.mapper

import dev.ahmad.wird.data.local.EntryEntity
import dev.ahmad.wird.domain.model.Entry
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/** entity -> domain without changing the client-generated entry identity. */
fun EntryEntity.toDomain(): Entry = Entry(
    id = id,
    habitId = habitId,
    day = LocalDate.fromEpochDays(epochDay),
    value = value,
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

/** domain -> entity. */
fun Entry.toEntity(): EntryEntity = EntryEntity(
    id = id,
    habitId = habitId,
    epochDay = day.toEpochDays(),
    value = value,
    updatedAt = updatedAt.toEpochMilliseconds(),
)
