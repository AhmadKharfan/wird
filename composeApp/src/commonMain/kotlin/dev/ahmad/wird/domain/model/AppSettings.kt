package dev.ahmad.wird.domain.model

/**
 * Everything the user can change about how the app behaves.
 *
 * [nickname] is remembered independently of [privacyMode], so switching to points-only and
 * back does not make the user type it again. The nickname mode cannot be chosen without one,
 * though: a circle would then show the user under no name while telling them they chose to
 * be shown by one.
 *
 * [onboardingCompleted] records that the user finished onboarding, whichever routine they
 * chose. It is what keeps a user who chose to start empty from being handed the default
 * routine on the next launch.
 */
data class AppSettings(
    val themeMode: ThemeMode,
    val numeralSystem: NumeralSystem,
    val location: Coordinates?,
    val calculationMethod: CalculationMethod,
    val privacyMode: PrivacyMode,
    val nickname: String? = null,
    val onboardingCompleted: Boolean = false,
) {
    init {
        require(privacyMode != PrivacyMode.NICKNAME || !nickname.isNullOrBlank()) {
            "the nickname privacy mode needs a nickname to show"
        }
    }

    companion object {
        /**
         * What a fresh install uses.
         *
         * [location] is null until the user provides one; prayer times cannot be computed
         * before then, and the UI says so rather than guessing a city. Numerals are western,
         * with Arabic-Indic a setting away.
         *
         * Privacy starts at points-only. The nickname mode has no nickname yet, and sharing
         * nothing would hide circles from a user who has not been offered them — while in
         * practice nothing is shared at all until the user joins a circle.
         */
        val DEFAULTS = AppSettings(
            themeMode = ThemeMode.SYSTEM,
            numeralSystem = NumeralSystem.WESTERN,
            location = null,
            calculationMethod = CalculationMethod.UMM_AL_QURA,
            privacyMode = PrivacyMode.POINTS_ONLY,
            nickname = null,
            onboardingCompleted = false,
        )
    }
}

/** Which palette to paint, independent of the host locale. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Which digits every number in the app is rendered with. */
enum class NumeralSystem { WESTERN, ARABIC_INDIC }

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

/** How much of the user's practice a circle may see — the three choices as the product names them. */
enum class PrivacyMode(
    /**
     * Whether the circle tab exists at all. Sharing nothing means comparing yourself only
     * to yourself, so the tab is removed rather than greyed out: there is nobody to show.
     */
    val showsCircles: Boolean,
) {
    /** Share points only: the day's score, under no name. */
    POINTS_ONLY(showsCircles = true),

    /** Use a nickname: the day's score, under a name the user chose. */
    NICKNAME(showsCircles = true),

    /** Don't share — compare me to myself. Nothing leaves the device and circles are hidden. */
    PRIVATE(showsCircles = false),
}
