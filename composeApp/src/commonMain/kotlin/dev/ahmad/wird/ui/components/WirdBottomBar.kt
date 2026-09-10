package dev.ahmad.wird.ui.components

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ahmad.wird.ui.navigation.TopLevelDestination
import dev.ahmad.wird.ui.theme.Sizes

/**
 * Stateless bottom bar. Takes the selected destination and a callback; it neither owns
 * navigation state nor knows about the NavController.
 *
 * Every colour it paints is a theme token. Material's bar reads its container, indicator and
 * label colours from roles the Wird schemes leave unset, which painted the library's own
 * lavender; they are named here instead. There are no icons yet, so there is no separate
 * selection indicator either — it would be an empty pill — and the selected label's colour
 * carries the selection.
 */
@Composable
fun WirdBottomBar(
    destinations: List<TopLevelDestination>,
    selected: TopLevelDestination,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier, containerColor = MaterialTheme.colorScheme.surface) {
        destinations.forEach { destination ->
            NavigationBarItem(
                modifier = Modifier.defaultMinSize(minHeight = Sizes.minTouchTarget),
                selected = destination == selected,
                onClick = { onSelect(destination) },
                label = {
                    Text(destination.label, style = MaterialTheme.typography.labelMedium)
                },
                icon = {},
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    }
}
