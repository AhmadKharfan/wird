package dev.ahmad.wird.ui.base

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private data class CounterState(val value: Int = 0, val failed: Boolean = false)

private sealed interface CounterEffect {
    data class Announce(val text: String) : CounterEffect
}

private class CounterViewModel(
    private val dispatcher: CoroutineDispatcher,
) : BaseViewModel<CounterState, CounterEffect>(CounterState()) {

    fun increment() = updateState { it.copy(value = it.value + 1) }

    fun announce(text: String) = sendEffect(CounterEffect.Announce(text))

    fun runFailing() = tryToExecute(
        block = { error("boom") },
        onSuccess = { },
        onError = { updateState { state -> state.copy(failed = true) } },
        dispatcher = dispatcher,
    )

    fun runSucceeding() = tryToExecute(
        block = { 41 },
        onSuccess = { result -> updateState { it.copy(value = result + 1) } },
        onError = { },
        dispatcher = dispatcher,
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun updateStateReducesState() = runTest {
        val viewModel = CounterViewModel(testDispatcher)

        viewModel.increment()
        viewModel.increment()

        assertEquals(2, viewModel.state.value.value)
    }

    @Test
    fun effectsAreDeliveredInOrder() = runTest {
        val viewModel = CounterViewModel(testDispatcher)

        viewModel.announce("first")
        viewModel.announce("second")

        val received = mutableListOf<CounterEffect>()
        // receiveAsFlow hands each effect to exactly one collector, so take what was sent.
        received.add(viewModel.effect.first())
        received.add(viewModel.effect.first())

        assertEquals(
            listOf<CounterEffect>(CounterEffect.Announce("first"), CounterEffect.Announce("second")),
            received,
        )
    }

    @Test
    fun effectsAreNotReplayedToALaterCollector() = runTest {
        val viewModel = CounterViewModel(testDispatcher)

        viewModel.announce("only once")
        assertEquals(CounterEffect.Announce("only once"), viewModel.effect.first())

        // The later collector attaches before anything else is sent. A replaying stream would
        // hand it the effect already delivered; a channel makes it wait for the next one.
        val late = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effect.first() }
        viewModel.announce("second")

        assertEquals(CounterEffect.Announce("second"), late.await())
    }

    @Test
    fun tryToExecuteRoutesFailureToOnError() = runTest {
        val viewModel = CounterViewModel(testDispatcher)

        viewModel.runFailing()

        assertEquals(true, viewModel.state.value.failed)
    }

    @Test
    fun tryToExecuteRoutesSuccessToOnSuccess() = runTest {
        val viewModel = CounterViewModel(testDispatcher)

        viewModel.runSucceeding()

        assertEquals(42, viewModel.state.value.value)
    }
}
