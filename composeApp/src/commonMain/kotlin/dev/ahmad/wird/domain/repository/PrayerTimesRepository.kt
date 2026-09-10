package dev.ahmad.wird.domain.repository

import dev.ahmad.wird.domain.model.Coordinates
import dev.ahmad.wird.domain.model.PrayerTime
import kotlinx.datetime.LocalDate

interface PrayerTimesRepository {

    suspend fun timesFor(date: LocalDate, coordinates: Coordinates): List<PrayerTime>
}
