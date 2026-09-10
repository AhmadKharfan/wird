package dev.ahmad.wird.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colour tokens. Placeholders until the Claude Design board is extracted — extracting
 * those tokens is an architect task and is never delegated.
 */
private val Sand = Color(0xFFF6F1E7)
private val Ink = Color(0xFF14202E)
private val Teal = Color(0xFF0F6E6E)
private val TealDark = Color(0xFF67D2CE)
private val Night = Color(0xFF101820)
private val Parchment = Color(0xFFF2EFE8)
private val Danger = Color(0xFFB4232C)
private val DangerDark = Color(0xFFFFB4AB)

val WirdLightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    background = Sand,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    error = Danger,
)

val WirdDarkColors = darkColorScheme(
    primary = TealDark,
    onPrimary = Night,
    background = Night,
    onBackground = Parchment,
    surface = Color(0xFF18232E),
    onSurface = Parchment,
    error = DangerDark,
)
