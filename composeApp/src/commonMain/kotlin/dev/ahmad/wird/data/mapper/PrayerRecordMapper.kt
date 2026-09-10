package dev.ahmad.wird.data.mapper

import dev.ahmad.wird.data.local.PrayerRecordEntity
import dev.ahmad.wird.domain.model.Prayer
import dev.ahmad.wird.domain.model.PrayerRecord
import dev.ahmad.wird.domain.model.PrayerStatus
import kotlinx.datetime.LocalDate

/** entity -> domain. Unknown enum names degrade to NOT_RECORDED rather than throwing. */
fun PrayerRecordEntity.toDomain(): PrayerRecord = PrayerRecord(
    date = LocalDate.fromEpochDays(epochDay),
    prayer = Prayer.valueOf(prayer),
    status = runCatching { PrayerStatus.valueOf(status) }.getOrDefault(PrayerStatus.NOT_RECORDED),
)

/** domain -> entity. */
fun PrayerRecord.toEntity(): PrayerRecordEntity = PrayerRecordEntity(
    id = entityId(date, prayer),
    epochDay = date.toEpochDays(),
    prayer = prayer.name,
    status = status.name,
)

fun entityId(date: LocalDate, prayer: Prayer): String = "${date.toEpochDays()}:${prayer.name}"
