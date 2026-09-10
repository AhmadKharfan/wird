package dev.ahmad.wird.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.ahmad.wird.ui.theme.Radius
import dev.ahmad.wird.ui.theme.Sizes
import dev.ahmad.wird.ui.theme.Spacing

/**
 * Stateless row with slot content. Enforces the 48dp touch-target floor so callers
 * cannot accidentally ship a smaller tap area.
 *
 * The click belongs to the `Surface` itself rather than to the modifier handed to it: a
 * `clickable` on that modifier sits outside the surface's own clip, so its press ripple was
 * drawn square past the rounded corners. On the surface, the ripple is clipped to the shape
 * and the row reads as a button to accessibility services.
 */
@Composable
fun WirdListRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Sizes.minTouchTarget),
        shape = RoundedCornerShape(Radius.md),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            leading()
            trailing()
        }
    }
}
