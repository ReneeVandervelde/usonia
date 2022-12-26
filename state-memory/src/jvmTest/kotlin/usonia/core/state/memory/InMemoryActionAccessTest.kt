package usonia.core.state.memory

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import usonia.foundation.FakeActions
import usonia.kotlin.first
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryActionAccessTest {
    @Test
    fun publish() = runTest {
        val actions = InMemoryActionAccess()

        val result = async { actions.actions.first() }
        launch { actions.publishAction(FakeActions.SwitchOff) }

        assertEquals(FakeActions.SwitchOff, result.await())
    }
}
