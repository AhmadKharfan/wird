package dev.ahmad.wird.data.repository

import dev.ahmad.wird.domain.model.CalculationMethod
import dev.ahmad.wird.domain.model.Coordinates
import dev.ahmad.wird.domain.model.Prayer
import dev.ahmad.wird.domain.model.PrayerTime
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

/**
 * The calculation method the user picks in settings has to reach the calculation. Makkah on
 * Thursday 15 January 2026, outside Ramadan.
 */
class AdhanPrayerTimesRepositoryTest {

    private val repository = AdhanPrayerTimesRepository()
    private val makkah = Coordinates(latitude = 21.4225, longitude = 39.8262)
    private val day = LocalDate(2026, 1, 15)

    private fun List<PrayerTime>.at(prayer: Prayer) = single { it.prayer == prayer }.dueAt

    @Test
    fun keepsUmmAlQurasFixedNinetyMinutesFromMaghribToIsha() = runTest {
        // Umm al-Qura sets Isha by a fixed interval, not a twilight angle. Any angle-based
        // method gives some other gap, so this fails if the method is not the one asked for.
        val times = repository.timesFor(day, makkah, CalculationMethod.UMM_AL_QURA)

        assertEquals(90.minutes, times.at(Prayer.ISHA) - times.at(Prayer.MAGHRIB))
    }

    @Test
    fun putsFajrEarlierUnderADeeperTwilightAngle() = runTest {
        // Egyptian takes Fajr at 19.5 degrees below the horizon, Muslim World League at 18.
        val egyptian = repository.timesFor(day, makkah, CalculationMethod.EGYPTIAN).at(Prayer.FAJR)
        val muslimWorldLeague = repository.timesFor(day, makkah, CalculationMethod.MUSLIM_WORLD_LEAGUE).at(Prayer.FAJR)

        assertTrue(egyptian < muslimWorldLeague, "egyptian $egyptian, muslim world league $muslimWorldLeague")
    }

    @Test
    fun answersWithTheFivePrayersForEveryMethodSettingsOffers() = runTest {
        CalculationMethod.entries.forEach { method ->
            assertEquals(
                listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA),
                repository.timesFor(day, makkah, method).map { it.prayer },
                "for $method",
            )
        }
    }
}
