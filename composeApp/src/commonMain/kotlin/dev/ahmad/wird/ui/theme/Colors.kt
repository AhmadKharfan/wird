package dev.ahmad.wird.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Colour tokens. Placeholders until the Claude Design board is extracted — extracting
 * those tokens is an architect task and is never delegated.
 *
 * Every value is named here and the schemes only map roles onto names, so a colour can never
 * appear in a scheme that is not a token.
 */
private val Sand = Color(0xFFF6F1E7)
private val Ink = Color(0xFF14202E)
private val Teal = Color(0xFF0F6E6E)
private val TealDark = Color(0xFF67D2CE)
private val Night = Color(0xFF101820)
private val NightSurface = Color(0xFF18232E)
private val Parchment = Color(0xFFF2EFE8)
private val White = Color(0xFFFFFFFF)
private val Danger = Color(0xFFB4232C)
private val DangerDark = Color(0xFFFFB4AB)

val WirdLightColors = lightColorScheme(
    primary = Teal,
    onPrimary = White,
    background = Sand,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    error = Danger,
    // The snackbar paints the inverse roles; left unset they fall back to the library's own
    // palette.
    inverseSurface = Ink,
    inverseOnSurface = Sand,
)

val WirdDarkColors = darkColorScheme(
    primary = TealDark,
    onPrimary = Night,
    background = Night,
    onBackground = Parchment,
    surface = NightSurface,
    onSurface = Parchment,
    error = DangerDark,
    inverseSurface = Parchment,
    inverseOnSurface = Night,
)
