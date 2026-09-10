package dev.ahmad.wird.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Wird is an Arabic product. Layout direction is forced RTL app-wide rather than
 * inherited from the host locale, so the layout is identical on an English phone
 * and in an English browser.
 *
 * Note: the CMP skill's theming example uses `dynamicColorScheme` with
 * `Build.VERSION` and `LocalContext`; that is Android-only and cannot live in
 * commonMain, so Wird uses fixed token schemes instead.
 */
@Composable
fun WirdTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (darkTheme) WirdDarkColors else WirdLightColors,
            content = content,
        )
    }
}
