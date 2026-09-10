package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeSettingsRepository
import dev.ahmad.wird.domain.model.AppSettings
import dev.ahmad.wird.domain.model.CalculationMethod
import dev.ahmad.wird.domain.model.Coordinates
import dev.ahmad.wird.domain.model.PrayerTime
import dev.ahmad.wird.domain.repository.PrayerTimesRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The calculation method is a setting, so the times are asked for under whatever the user
 * chose — never a method fixed somewhere below the setting.
 */
class GetPrayerTimesUseCaseTest {

    /** Records the method it was asked for; the calculation itself is tested in data. */
    private class RecordingPrayerTimes : PrayerTimesRepository {
        var askedWith: CalculationMethod? = null

        override suspend fun timesFor(
            date: LocalDate,
            coordinates: Coordinates,
            method: CalculationMethod,
        ): List<PrayerTime> {
            askedWith = method
            return emptyList()
        }
    }

    private val makkah = Coordinates(latitude = 21.4225, longitude = 39.8262)
    private val day = LocalDate(2026, 1, 15)

    @Test
    fun asksForTimesUnderTheMethodChosenInSettings() = runTest {
        val prayerTimes = RecordingPrayerTimes()
        val settings = FakeSettingsRepository(AppSettings.DEFAULTS.copy(calculationMethod = CalculationMethod.EGYPTIAN))

        GetPrayerTimesUseCase(prayerTimes, settings)(day, makkah)

        assertEquals(CalculationMethod.EGYPTIAN, prayerTimes.askedWith)
    }

    @Test
    fun asksUnderUmmAlQuraOnAFreshInstall() = runTest {
        val prayerTimes = RecordingPrayerTimes()

        GetPrayerTimesUseCase(prayerTimes, FakeSettingsRepository())(day, makkah)

        assertEquals(CalculationMethod.UMM_AL_QURA, prayerTimes.askedWith)
    }
}
