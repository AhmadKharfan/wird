package dev.ahmad.wird.domain.repository

import dev.ahmad.wird.domain.model.CalculationMethod
import dev.ahmad.wird.domain.model.Coordinates
import dev.ahmad.wird.domain.model.PrayerTime
import kotlinx.datetime.LocalDate

interface PrayerTimesRepository {

    /** The five prayers on [date] at [coordinates], under the twilight convention [method]. */
    suspend fun timesFor(date: LocalDate, coordinates: Coordinates, method: CalculationMethod): List<PrayerTime>
}
