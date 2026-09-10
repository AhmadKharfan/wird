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
 * Settings is where a stored string can be a value this build has never heard of, because
 * a user can install an older version over a newer one. Every unknown resolves to the same
 * value a fresh install would use, so the app is always in a state it understands.
 */
class SettingsMapperTest {

    private fun entity(
        themeMode: String = "DARK",
        numeralSystem: String = "WESTERN",
        latitude: Double? = 21.4225,
        longitude: Double? = 39.8262,
        calculationMethod: String = "EGYPTIAN",
        privacyMode: String = "ANONYMOUS",
    ) = SettingsEntity(
        themeMode = themeMode,
        numeralSystem = numeralSystem,
        latitude = latitude,
        longitude = longitude,
        calculationMethod = calculationMethod,
        privacyMode = privacyMode,
        updatedAt = 1_768_000_000_000,
    )

    private val domain = AppSettings(
        themeMode = ThemeMode.DARK,
        numeralSystem = NumeralSystem.WESTERN,
        location = Coordinates(latitude = 21.4225, longitude = 39.8262),
        calculationMethod = CalculationMethod.EGYPTIAN,
        privacyMode = PrivacyMode.ANONYMOUS,
    )

    @Test
    fun readsEveryStoredChoiceBack() {
        assertEquals(domain, entity().toDomain())
    }

    // --- location ------------------------------------------------------------------------

    @Test
    fun readsAnAbsentLocationAsNull() {
        assertNull(entity(latitude = null, longitude = null).toDomain().location)
    }

    @Test
    fun readsAHalfWrittenLocationAsAbsent() {
        // A latitude with no longitude is not a place. Treating it as one would put the
        // user on the prime meridian and compute prayer times for the wrong city.
        assertNull(entity(latitude = 21.4225, longitude = null).toDomain().location)
        assertNull(entity(latitude = null, longitude = 39.8262).toDomain().location)
    }

    @Test
    fun writesAnAbsentLocationAsTwoNulls() {
        val mapped = domain.copy(location = null).toEntity(updatedAt = 1)

        assertNull(mapped.latitude)
        assertNull(mapped.longitude)
    }

    // --- unknown enum values ---------------------------------------------------------------

    @Test
    fun fallsBackToTheDefaultThemeForAnUnknownValue() {
        assertEquals(
            AppSettings.DEFAULTS.themeMode,
            entity(themeMode = "SOLARIZED").toDomain().themeMode,
        )
    }

    @Test
    fun fallsBackToTheDefaultNumeralsForAnUnknownValue() {
        assertEquals(
            AppSettings.DEFAULTS.numeralSystem,
            entity(numeralSystem = "ROMAN").toDomain().numeralSystem,
        )
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
        // Falling back to the most private option would be a different, defensible rule —
        // but OPEN is what a fresh install uses, and one rule everywhere is easier to
        // reason about than a per-field policy.
        assertEquals(
            AppSettings.DEFAULTS.privacyMode,
            entity(privacyMode = "SOMETHING_NEW").toDomain().privacyMode,
        )
    }

    // --- round trips ---------------------------------------------------------------------------

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
