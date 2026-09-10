package dev.ahmad.wird.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * Collects one-shot effects only while the host is at least STARTED, on both targets
 * — `LocalLifecycleOwner` and `repeatOnLifecycle` are multiplatform in
 * lifecycle-runtime-compose, so this needs no expect/actual.
 *
 * Effects buffered while stopped are delivered on the next START rather than dropped.
 */
@Composable
fun <E> EffectHandler(
    effect: Flow<E>,
    onEffect: suspend (E) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEffect by rememberUpdatedState(onEffect)

    LaunchedEffect(effect, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            effect.collect { currentOnEffect(it) }
        }
    }
}
