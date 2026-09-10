package dev.ahmad.wird.ui.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ahmad.wird.ui.base.EffectHandler
import dev.ahmad.wird.ui.components.WirdListRow
import dev.ahmad.wird.ui.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TodayScreen(viewModel: TodayViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    EffectHandler(viewModel.effect) { effect ->
        when (effect) {
            is TodayEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
        }
    }

    TodayContent(
        state = state,
        listener = viewModel,
        snackbarHostState = snackbarHostState,
    )
}

/**
 * Stateless: previewable and testable without a ViewModel.
 *
 * Provisional layout. It renders the real routine from the real database so the data layer
 * is exercised end to end, but every visual decision here is a placeholder until the Today
 * design board lands.
 */
@Composable
private fun TodayContent(
    state: TodayUiState,
    listener: TodayInteractionListener,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.errorMessage != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.errorMessage, style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = listener::onRetry) { Text(RETRY) }
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    item {
                        Text(
                            text = state.scoreLabel,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = Spacing.sm),
                        )
                    }
                    items(items = state.rows, key = { it.habitId }) { row ->
                        WirdListRow(
                            onClick = { listener.onHabitTapped(row.habitId) },
                            leading = { Text(row.label, style = MaterialTheme.typography.titleMedium) },
                            trailing = {
                                Text(
                                    text = row.valueLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (row.isComplete) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
