package dev.ahmad.wird.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * One state object per screen, exposed as [StateFlow]; one-shot events exposed as a
 * Channel-backed [Flow].
 *
 * State is replayed to a new collector (a rotation, a re-attach). Effects are not:
 * the Channel has no buffer replay, so a navigation or a toast fires exactly once.
 */
abstract class BaseViewModel<S : Any, E : Any>(initialState: S) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<E>(Channel.BUFFERED)
    val effect: Flow<E> = _effect.receiveAsFlow()

    protected val currentState: S get() = _state.value

    protected fun updateState(reducer: (S) -> S) {
        _state.update(reducer)
    }

    protected fun sendEffect(effect: E) {
        viewModelScope.launch { _effect.send(effect) }
    }

    /**
     * Runs [block] off the main dispatcher and routes the outcome to [onSuccess] or
     * [onError], so no feature repeats try/catch. Cancellation is rethrown, never
     * reported as an error.
     */
    protected fun <T> tryToExecute(
        block: suspend () -> T,
        onSuccess: (T) -> Unit,
        onError: (Throwable) -> Unit,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ) {
        viewModelScope.launch {
            try {
                val result = withContext(dispatcher) { block() }
                onSuccess(result)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                onError(error)
            }
        }
    }

    /** Collects a stream, routing each emission to [onEach] and any failure to [onError]. */
    protected fun <T> collectFlow(
        flow: Flow<T>,
        onEach: (T) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                flow.collect { onEach(it) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                onError(error)
            }
        }
    }
}
