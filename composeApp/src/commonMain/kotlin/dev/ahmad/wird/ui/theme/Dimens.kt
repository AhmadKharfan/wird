package dev.ahmad.wird.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing, radius and size tokens. Features must never write a raw `.dp`. */
object Spacing {
    val none: Dp = 0.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
}

object Radius {
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 20.dp
}

object Sizes {
    /** Accessibility floor. Nothing tappable may be smaller than this. */
    val minTouchTarget: Dp = 48.dp
    val listRowHeight: Dp = 56.dp
}
