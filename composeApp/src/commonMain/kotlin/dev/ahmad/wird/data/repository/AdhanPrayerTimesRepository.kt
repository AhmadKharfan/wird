package dev.ahmad.wird.data.repository

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import dev.ahmad.wird.domain.model.Coordinates
import dev.ahmad.wird.domain.model.Prayer
import dev.ahmad.wird.domain.model.PrayerTime
import dev.ahmad.wird.domain.repository.PrayerTimesRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import com.batoulapps.adhan2.Coordinates as AdhanCoordinates

class AdhanPrayerTimesRepository : PrayerTimesRepository {

    override suspend fun timesFor(date: LocalDate, coordinates: Coordinates): List<PrayerTime> {
        val times = PrayerTimes(
            coordinates = AdhanCoordinates(coordinates.latitude, coordinates.longitude),
            dateComponents = DateComponents(date.year, date.month.number, date.day),
            calculationParameters = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters,
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
