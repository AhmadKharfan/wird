package dev.ahmad.wird.domain.model

/** Everything the user can change about how the app behaves. */
data class AppSettings(
    val themeMode: ThemeMode,
    val numeralSystem: NumeralSystem,
    val location: Coordinates?,
    val calculationMethod: CalculationMethod,
    val privacyMode: PrivacyMode,
) {
    companion object {
        /**
         * What a fresh install uses. [location] is null until the user provides one;
         * prayer times cannot be computed before then, and the UI says so rather than
         * guessing a city.
         */
        val DEFAULTS = AppSettings(
            themeMode = ThemeMode.SYSTEM,
            numeralSystem = NumeralSystem.ARABIC_INDIC,
            location = null,
            calculationMethod = CalculationMethod.UMM_AL_QURA,
            privacyMode = PrivacyMode.OPEN,
        )
    }
}

/** Which palette to paint, independent of the host locale. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Which digits to render. The product is Arabic, so Arabic-Indic is the default. */
enum class NumeralSystem { ARABIC_INDIC, WESTERN }

/**
 * Which convention fixes the twilight angles used for Fajr and Isha. These are domain
 * names; the data layer maps them onto whatever the prayer-time library calls them.
 */
enum class CalculationMethod {
    UMM_AL_QURA,
    MUSLIM_WORLD_LEAGUE,
    EGYPTIAN,
    KARACHI,
    DUBAI,
    QATAR,
    KUWAIT,
    SINGAPORE,
    TURKEY,
    TEHRAN,
    MOON_SIGHTING_COMMITTEE,
    NORTH_AMERICA,
}

/** How much of the user's practice a circle may see. */
enum class PrivacyMode {
    /** Name and daily ratio are both shared. */
    OPEN,

    /** The ratio is shared, the name is not. */
    ANONYMOUS,

    /** Nothing is shared; the user still sees others. */
    PRIVATE,
}
