package dev.ahmad.wird.domain.repository

import dev.ahmad.wird.domain.model.Prayer
import dev.ahmad.wird.domain.model.PrayerRecord
import dev.ahmad.wird.domain.model.PrayerStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Local-first: every write completes against local storage. Nothing here returns a
 * network result, and no implementation may await one before returning.
 */
interface PrayerRecordRepository {

    fun observeRecords(date: LocalDate): Flow<List<PrayerRecord>>

    suspend fun record(date: LocalDate, prayer: Prayer, status: PrayerStatus)
}
