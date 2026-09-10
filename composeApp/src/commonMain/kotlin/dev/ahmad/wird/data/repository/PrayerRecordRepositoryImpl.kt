package dev.ahmad.wird.data.repository

import dev.ahmad.wird.data.local.PrayerRecordDao
import dev.ahmad.wird.data.mapper.toDomain
import dev.ahmad.wird.data.mapper.toEntity
import dev.ahmad.wird.domain.model.Prayer
import dev.ahmad.wird.domain.model.PrayerRecord
import dev.ahmad.wird.domain.model.PrayerStatus
import dev.ahmad.wird.domain.repository.PrayerRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class PrayerRecordRepositoryImpl(
    private val dao: PrayerRecordDao,
) : PrayerRecordRepository {

    override fun observeRecords(date: LocalDate): Flow<List<PrayerRecord>> =
        dao.observeByDay(date.toEpochDays()).map { entities -> entities.map { it.toDomain() } }

    /** Local-first: this returns once the local write lands. No network in this path. */
    override suspend fun record(date: LocalDate, prayer: Prayer, status: PrayerStatus) {
        dao.upsert(PrayerRecord(date, prayer, status).toEntity())
    }
}
