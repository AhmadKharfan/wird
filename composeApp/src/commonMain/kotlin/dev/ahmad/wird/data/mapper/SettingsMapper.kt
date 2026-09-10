package dev.ahmad.wird.data.mapper

import dev.ahmad.wird.data.local.SettingsEntity
import dev.ahmad.wird.domain.model.AppSettings
import dev.ahmad.wird.domain.model.CalculationMethod
import dev.ahmad.wird.domain.model.Coordinates
import dev.ahmad.wird.domain.model.NumeralSystem
import dev.ahmad.wird.domain.model.PrivacyMode
import dev.ahmad.wird.domain.model.ThemeMode

/** entity -> domain, degrading unknown stored choices to fresh-install defaults. */
fun SettingsEntity.toDomain(): AppSettings = AppSettings(
    themeMode = runCatching { ThemeMode.valueOf(themeMode) }
        .getOrDefault(AppSettings.DEFAULTS.themeMode),
    numeralSystem = runCatching { NumeralSystem.valueOf(numeralSystem) }
        .getOrDefault(AppSettings.DEFAULTS.numeralSystem),
    // Both coordinates are required because a half-written pair does not identify a valid location.
    location = if (latitude != null && longitude != null) Coordinates(latitude, longitude) else null,
    calculationMethod = runCatching { CalculationMethod.valueOf(calculationMethod) }
        .getOrDefault(AppSettings.DEFAULTS.calculationMethod),
    privacyMode = runCatching { PrivacyMode.valueOf(privacyMode) }
        .getOrDefault(AppSettings.DEFAULTS.privacyMode),
)

/** domain -> singleton settings entity with caller-supplied write metadata. */
fun AppSettings.toEntity(updatedAt: Long): SettingsEntity = SettingsEntity(
    id = SettingsEntity.SINGLETON_ID,
    themeMode = themeMode.name,
    numeralSystem = numeralSystem.name,
    latitude = location?.latitude,
    longitude = location?.longitude,
    calculationMethod = calculationMethod.name,
    privacyMode = privacyMode.name,
    updatedAt = updatedAt,
)
