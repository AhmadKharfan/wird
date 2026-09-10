package dev.ahmad.wird.ui

import dev.ahmad.wird.domain.fake.FakeSettingsRepository
import dev.ahmad.wird.domain.model.AppSettings
import dev.ahmad.wird.domain.model.ThemeMode
import dev.ahmad.wird.domain.usecase.ObserveSettingsUseCase
import dev.ahmad.wird.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The app shell decides which palette the whole app paints, from the theme mode in
 * settings — and applies a change live, not on the next launch.
 *
 * The state is display-ready: `null` means follow the system, `true` and `false` force a
 * palette. The domain's ThemeMode never reaches the composable.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(settings: FakeSettingsRepository) =
        AppViewModel(ObserveSettingsUseCase(settings))

    private fun settingsWith(mode: ThemeMode) =
        FakeSettingsRepository(AppSettings.DEFAULTS.copy(themeMode = mode))

    @Test
    fun followsTheSystemOnAFreshInstall() = runTest {
        assertNull(viewModel(FakeSettingsRepository()).state.value.forcedDarkTheme)
    }

    @Test
    fun forcesTheDarkPaletteWhenDarkIsChosen() = runTest {
        assertEquals(true, viewModel(settingsWith(ThemeMode.DARK)).state.value.forcedDarkTheme)
    }

    @Test
    fun forcesTheLightPaletteWhenLightIsChosen() = runTest {
        // Light has to be forced too, not left to the system: on a dark phone, choosing
        // light must still paint light.
        assertEquals(false, viewModel(settingsWith(ThemeMode.LIGHT)).state.value.forcedDarkTheme)
    }

    @Test
    fun appliesAThemeChangeWithoutARestart() = runTest {
        val settings = FakeSettingsRepository()
        val shell = viewModel(settings)
        val setThemeMode = SetThemeModeUseCase(settings)

        setThemeMode(ThemeMode.DARK)
        assertEquals(true, shell.state.value.forcedDarkTheme)

        setThemeMode(ThemeMode.SYSTEM)
        assertNull(shell.state.value.forcedDarkTheme)
    }

    @Test
    fun keepsFollowingTheSystemWhenSettingsCannotBeRead() = runTest {
        // A settings failure must not stop the app from painting; following the system is
        // the one choice that is never wrong for long.
        val settings = FakeSettingsRepository()
        settings.controls.failWith(IllegalStateException("settings unreadable"))

        assertNull(viewModel(settings).state.value.forcedDarkTheme)
    }
}
