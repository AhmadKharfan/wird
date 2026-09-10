package dev.ahmad.wird.data.mapper

import dev.ahmad.wird.data.local.SettingsEntity
import dev.ahmad.wird.domain.model.AppSettings
import dev.ahmad.wird.domain.model.CalculationMethod
import dev.ahmad.wird.domain.model.Coordinates
import dev.ahmad.wird.domain.model.NumeralSystem
import dev.ahmad.wird.domain.model.PrivacyMode
import dev.ahmad.wird.domain.model.ThemeMode

/** entity -> domain, degrading unknown stored choices to fresh-install defaults. */
fun SettingsEntity.toDomain(): AppSettings {
    val storedNickname = nickname?.takeIf { it.isNotBlank() }
    val storedMode = runCatching { PrivacyMode.valueOf(privacyMode) }
        .getOrDefault(AppSettings.DEFAULTS.privacyMode)

    return AppSettings(
        themeMode = runCatching { ThemeMode.valueOf(themeMode) }
            .getOrDefault(AppSettings.DEFAULTS.themeMode),
        numeralSystem = runCatching { NumeralSystem.valueOf(numeralSystem) }
            .getOrDefault(AppSettings.DEFAULTS.numeralSystem),
        // Both coordinates are required because a half-written pair does not identify a valid location.
        location = if (latitude != null && longitude != null) Coordinates(latitude, longitude) else null,
        calculationMethod = runCatching { CalculationMethod.valueOf(calculationMethod) }
            .getOrDefault(AppSettings.DEFAULTS.calculationMethod),
        // Storage cannot enforce that the nickname mode carries a nickname, and AppSettings
        // refuses it without one. Reading it as points-only keeps the promise the user can
        // still see — their name is not shown — instead of failing every settings read.
        privacyMode = if (storedMode == PrivacyMode.NICKNAME && storedNickname == null) {
            PrivacyMode.POINTS_ONLY
        } else {
            storedMode
        },
        nickname = storedNickname,
    )
}

/** domain -> singleton settings entity with caller-supplied write metadata. */
fun AppSettings.toEntity(updatedAt: Long): SettingsEntity = SettingsEntity(
    id = SettingsEntity.SINGLETON_ID,
    themeMode = themeMode.name,
    numeralSystem = numeralSystem.name,
    latitude = location?.latitude,
    longitude = location?.longitude,
    calculationMethod = calculationMethod.name,
    privacyMode = privacyMode.name,
    nickname = nickname,
    updatedAt = updatedAt,
)
