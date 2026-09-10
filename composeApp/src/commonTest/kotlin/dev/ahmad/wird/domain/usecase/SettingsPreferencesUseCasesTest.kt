package dev.ahmad.wird.domain.usecase

import dev.ahmad.wird.domain.fake.FakeSettingsRepository
import dev.ahmad.wird.domain.model.AppSettings
import dev.ahmad.wird.domain.model.NumeralSystem
import dev.ahmad.wird.domain.model.PrivacyMode
import dev.ahmad.wird.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The preference use cases the settings screen calls. Theme and numerals are one-field
 * writes; privacy is where the rules sit, because the nickname mode needs a nickname and a
 * nickname, once given, is remembered across modes.
 */
class SettingsPreferencesUseCasesTest {

    // --- reading ------------------------------------------------------------------------

    @Test
    fun readsTheDefaultsOnAFreshInstall() = runTest {
        assertEquals(AppSettings.DEFAULTS, ObserveSettingsUseCase(FakeSettingsRepository())().first())
    }

    // --- theme and numerals ---------------------------------------------------------------

    @Test
    fun keepsTheChosenThemeMode() = runTest {
        val settings = FakeSettingsRepository()

        SetThemeModeUseCase(settings)(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, settings.settings.themeMode)
    }

    @Test
    fun keepsTheChosenNumeralSystem() = runTest {
        val settings = FakeSettingsRepository()

        SetNumeralSystemUseCase(settings)(NumeralSystem.ARABIC_INDIC)

        assertEquals(NumeralSystem.ARABIC_INDIC, settings.settings.numeralSystem)
    }

    @Test
    fun changesOnlyTheFieldItIsAbout() = runTest {
        // Two settings changed in a row must not clobber each other.
        val settings = FakeSettingsRepository()

        SetThemeModeUseCase(settings)(ThemeMode.DARK)
        SetNumeralSystemUseCase(settings)(NumeralSystem.ARABIC_INDIC)

        assertEquals(ThemeMode.DARK, settings.settings.themeMode)
        assertEquals(NumeralSystem.ARABIC_INDIC, settings.settings.numeralSystem)
    }

    // --- privacy ----------------------------------------------------------------------------

    @Test
    fun switchesToPointsOnlyWithoutNeedingANickname() = runTest {
        val settings = FakeSettingsRepository(AppSettings.DEFAULTS.copy(privacyMode = PrivacyMode.PRIVATE))

        SetPrivacyModeUseCase(settings)(PrivacyMode.POINTS_ONLY)

        assertEquals(PrivacyMode.POINTS_ONLY, settings.settings.privacyMode)
    }

    @Test
    fun choosesTheNicknameModeWithATrimmedNickname() = runTest {
        val settings = FakeSettingsRepository()

        SetPrivacyModeUseCase(settings)(PrivacyMode.NICKNAME, nickname = "  أبو عمر  ")

        assertEquals(PrivacyMode.NICKNAME, settings.settings.privacyMode)
        assertEquals("أبو عمر", settings.settings.nickname)
    }

    @Test
    fun rejectsTheNicknameModeWithNoNicknameAndChangesNothing() = runTest {
        val settings = FakeSettingsRepository()

        assertFailsWith<IllegalArgumentException> {
            SetPrivacyModeUseCase(settings)(PrivacyMode.NICKNAME, nickname = "   ")
        }

        assertEquals(AppSettings.DEFAULTS, settings.settings)
    }

    @Test
    fun returnsToTheNicknameModeUsingTheRememberedNickname() = runTest {
        // Choosing points-only and then the nickname again must not make the user retype
        // the name they already gave.
        val settings = FakeSettingsRepository()
        val set = SetPrivacyModeUseCase(settings)
        set(PrivacyMode.NICKNAME, nickname = "أبو عمر")
        set(PrivacyMode.POINTS_ONLY)

        set(PrivacyMode.NICKNAME)

        assertEquals(PrivacyMode.NICKNAME, settings.settings.privacyMode)
        assertEquals("أبو عمر", settings.settings.nickname)
    }

    @Test
    fun keepsTheNicknameRememberedWhileSharingNothing() = runTest {
        val settings = FakeSettingsRepository()
        val set = SetPrivacyModeUseCase(settings)
        set(PrivacyMode.NICKNAME, nickname = "أبو عمر")

        set(PrivacyMode.PRIVATE)

        assertEquals("أبو عمر", settings.settings.nickname)
    }

    // --- the circle tab ------------------------------------------------------------------------

    @Test
    fun showsTheCircleTabWhileSomethingIsShared() = runTest {
        assertTrue(ObserveCircleTabVisibleUseCase(FakeSettingsRepository())().first())
    }

    @Test
    fun removesTheCircleTabTheMomentTheUserStopsSharing() = runTest {
        // Live, not on the next launch: choosing "compare me to myself" takes the tab away
        // while the settings screen is still open.
        val settings = FakeSettingsRepository()
        val seen = mutableListOf<Boolean>()
        val collecting = launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveCircleTabVisibleUseCase(settings)().toList(seen)
        }

        SetPrivacyModeUseCase(settings)(PrivacyMode.PRIVATE)
        collecting.cancel()

        assertEquals(listOf(true, false), seen)
    }

    @Test
    fun doesNotReannounceTheTabWhenAnUnrelatedSettingChanges() = runTest {
        // A theme or numeral change must not make the navigation rebuild its tabs.
        val settings = FakeSettingsRepository()
        val seen = mutableListOf<Boolean>()
        val collecting = launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveCircleTabVisibleUseCase(settings)().toList(seen)
        }

        SetThemeModeUseCase(settings)(ThemeMode.DARK)
        SetNumeralSystemUseCase(settings)(NumeralSystem.ARABIC_INDIC)
        collecting.cancel()

        assertEquals(listOf(true), seen)
    }
}
