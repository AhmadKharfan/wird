package dev.ahmad.wird.data.mapper

import dev.ahmad.wird.data.local.PrayerRecordEntity
import dev.ahmad.wird.domain.model.Prayer
import dev.ahmad.wird.domain.model.PrayerRecord
import dev.ahmad.wird.domain.model.PrayerStatus
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PrayerRecordMapperTest {

    private val date = LocalDate(2026, 9, 10)

    @Test
    fun domainToEntityAndBackIsLossless() {
        val original = PrayerRecord(date, Prayer.ASR, PrayerStatus.LATE)

        assertEquals(original, original.toEntity().toDomain())
    }

    @Test
    fun entityIdIsStablePerDayAndPrayer() {
        val first = PrayerRecord(date, Prayer.FAJR, PrayerStatus.ON_TIME).toEntity()
        val second = PrayerRecord(date, Prayer.FAJR, PrayerStatus.MISSED).toEntity()

        // Same key, so a re-record replaces rather than duplicates.
        assertEquals(first.id, second.id)
    }

    @Test
    fun unknownStatusDegradesToNotRecorded() {
        val entity = PrayerRecordEntity(
            id = entityId(date, Prayer.ISHA),
            epochDay = date.toEpochDays(),
            prayer = Prayer.ISHA.name,
            status = "SOMETHING_FROM_A_FUTURE_VERSION",
        )

        assertEquals(PrayerStatus.NOT_RECORDED, entity.toDomain().status)
    }
}
