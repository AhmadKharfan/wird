package dev.ahmad.wird.data.mapper

import dev.ahmad.wird.data.local.SettingsEntity
import dev.ahmad.wird.domain.model.AppSettings
import dev.ahmad.wird.domain.model.CalculationMethod
import dev.ahmad.wird.domain.model.Coordinates
import dev.ahmad.wird.domain.model.NumeralSystem
import dev.ahmad.wird.domain.model.PrivacyMode
import dev.ahmad.wird.domain.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Settings is where a stored string can be a value this build has never heard of, because a
 * user can install an older version over a newer one. Every unknown resolves to the value a
 * fresh install would use, so the app is always in a state it understands.
 */
class SettingsMapperTest {

    private fun entity(
        themeMode: String = "DARK",
        numeralSystem: String = "ARABIC_INDIC",
        latitude: Double? = 21.4225,
        longitude: Double? = 39.8262,
        calculationMethod: String = "EGYPTIAN",
        privacyMode: String = "POINTS_ONLY",
        nickname: String? = null,
        onboardingCompleted: Boolean = false,
    ) = SettingsEntity(
        themeMode = themeMode,
        numeralSystem = numeralSystem,
        latitude = latitude,
        longitude = longitude,
        calculationMethod = calculationMethod,
        privacyMode = privacyMode,
        nickname = nickname,
        onboardingCompleted = onboardingCompleted,
        updatedAt = 1_768_000_000_000,
    )

    private val domain = AppSettings(
        themeMode = ThemeMode.DARK,
        numeralSystem = NumeralSystem.ARABIC_INDIC,
        location = Coordinates(latitude = 21.4225, longitude = 39.8262),
        calculationMethod = CalculationMethod.EGYPTIAN,
        privacyMode = PrivacyMode.POINTS_ONLY,
        nickname = null,
    )

    @Test
    fun readsEveryStoredChoiceBack() {
        assertEquals(domain, entity().toDomain())
    }

    // --- location -------------------------------------------------------------------------

    @Test
    fun readsAnAbsentLocationAsNull() {
        assertNull(entity(latitude = null, longitude = null).toDomain().location)
    }

    @Test
    fun readsAHalfWrittenLocationAsAbsent() {
        // A latitude with no longitude is not a place. Treating it as one would put the user
        // on the prime meridian and compute prayer times for the wrong city.
        assertNull(entity(latitude = 21.4225, longitude = null).toDomain().location)
        assertNull(entity(latitude = null, longitude = 39.8262).toDomain().location)
    }

    @Test
    fun writesAnAbsentLocationAsTwoNulls() {
        val mapped = domain.copy(location = null).toEntity(updatedAt = 1)

        assertNull(mapped.latitude)
        assertNull(mapped.longitude)
    }

    // --- the nickname -------------------------------------------------------------------------

    @Test
    fun carriesANicknameBothWays() {
        val named = domain.copy(privacyMode = PrivacyMode.NICKNAME, nickname = "أبو عمر")

        assertEquals(named, named.toEntity(updatedAt = 1).toDomain())
    }

    @Test
    fun readsTheNicknameModeWithNoNicknameAsPointsOnly() {
        // Storage cannot enforce the pairing, and AppSettings refuses the nickname mode
        // without a nickname. Reading it back as points-only keeps the promise the user can
        // still see — their name is not shown — instead of failing every settings read.
        val read = entity(privacyMode = "NICKNAME", nickname = null).toDomain()

        assertEquals(PrivacyMode.POINTS_ONLY, read.privacyMode)
    }

    @Test
    fun readsABlankNicknameAsNoNickname() {
        assertNull(entity(nickname = "   ").toDomain().nickname)
    }

    // --- onboarding ---------------------------------------------------------------------------

    @Test
    fun carriesACompletedOnboardingBothWays() {
        val onboarded = domain.copy(onboardingCompleted = true)

        assertEquals(onboarded, onboarded.toEntity(updatedAt = 1).toDomain())
    }

    @Test
    fun readsAStoredCompletedOnboarding() {
        assertEquals(true, entity(onboardingCompleted = true).toDomain().onboardingCompleted)
    }

    // --- unknown enum values ----------------------------------------------------------------

    @Test
    fun fallsBackToTheDefaultThemeForAnUnknownValue() {
        assertEquals(AppSettings.DEFAULTS.themeMode, entity(themeMode = "SOLARIZED").toDomain().themeMode)
    }

    @Test
    fun fallsBackToTheDefaultNumeralsForAnUnknownValue() {
        assertEquals(AppSettings.DEFAULTS.numeralSystem, entity(numeralSystem = "ROMAN").toDomain().numeralSystem)
    }

    @Test
    fun fallsBackToTheDefaultCalculationMethodForAnUnknownValue() {
        assertEquals(
            AppSettings.DEFAULTS.calculationMethod,
            entity(calculationMethod = "SOMETHING_NEW").toDomain().calculationMethod,
        )
    }

    @Test
    fun fallsBackToTheDefaultPrivacyModeForAnUnknownValue() {
        assertEquals(
            AppSettings.DEFAULTS.privacyMode,
            entity(privacyMode = "SOMETHING_NEW").toDomain().privacyMode,
        )
    }

    // --- round trips ------------------------------------------------------------------------

    @Test
    fun survivesAnEntityToDomainToEntityRoundTrip() {
        val original = entity()

        assertEquals(original, original.toDomain().toEntity(updatedAt = original.updatedAt))
    }

    @Test
    fun survivesADomainToEntityToDomainRoundTrip() {
        assertEquals(domain, domain.toEntity(updatedAt = 1).toDomain())
    }

    @Test
    fun survivesARoundTripWithNoLocation() {
        val noLocation = domain.copy(location = null)

        assertEquals(noLocation, noLocation.toEntity(updatedAt = 1).toDomain())
    }

    @Test
    fun writesToTheSingletonRow() {
        // Anything else would append a second settings row on the next save.
        assertEquals(SettingsEntity.SINGLETON_ID, domain.toEntity(updatedAt = 1).id)
    }
}
