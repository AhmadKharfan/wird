package dev.ahmad.wird.data.repository

import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import dev.ahmad.wird.domain.model.CalculationMethod
import dev.ahmad.wird.domain.model.Coordinates
import dev.ahmad.wird.domain.model.Prayer
import dev.ahmad.wird.domain.model.PrayerTime
import dev.ahmad.wird.domain.repository.PrayerTimesRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import com.batoulapps.adhan2.CalculationMethod as AdhanMethod
import com.batoulapps.adhan2.Coordinates as AdhanCoordinates

class AdhanPrayerTimesRepository : PrayerTimesRepository {

    override suspend fun timesFor(date: LocalDate, coordinates: Coordinates, method: CalculationMethod): List<PrayerTime> {
        val times = PrayerTimes(
            coordinates = AdhanCoordinates(coordinates.latitude, coordinates.longitude),
            dateComponents = DateComponents(date.year, date.month.number, date.day),
            calculationParameters = method.toAdhan().parameters,
        )
        return listOf(
            PrayerTime(Prayer.FAJR, times.fajr),
            PrayerTime(Prayer.DHUHR, times.dhuhr),
            PrayerTime(Prayer.ASR, times.asr),
            PrayerTime(Prayer.MAGHRIB, times.maghrib),
            PrayerTime(Prayer.ISHA, times.isha),
        )
    }
}

/** Spelled out rather than matched by name, so renaming either side is a compile error. */
private fun CalculationMethod.toAdhan(): AdhanMethod = when (this) {
    CalculationMethod.UMM_AL_QURA -> AdhanMethod.UMM_AL_QURA
    CalculationMethod.MUSLIM_WORLD_LEAGUE -> AdhanMethod.MUSLIM_WORLD_LEAGUE
    CalculationMethod.EGYPTIAN -> AdhanMethod.EGYPTIAN
    CalculationMethod.KARACHI -> AdhanMethod.KARACHI
    CalculationMethod.DUBAI -> AdhanMethod.DUBAI
    CalculationMethod.QATAR -> AdhanMethod.QATAR
    CalculationMethod.KUWAIT -> AdhanMethod.KUWAIT
    CalculationMethod.SINGAPORE -> AdhanMethod.SINGAPORE
    CalculationMethod.TURKEY -> AdhanMethod.TURKEY
    CalculationMethod.MOON_SIGHTING_COMMITTEE -> AdhanMethod.MOON_SIGHTING_COMMITTEE
    CalculationMethod.NORTH_AMERICA -> AdhanMethod.NORTH_AMERICA
}
