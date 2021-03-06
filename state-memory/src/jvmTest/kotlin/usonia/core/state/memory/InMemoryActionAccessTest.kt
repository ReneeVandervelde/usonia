package usonia.core.state.memory

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runBlockingTest
import usonia.foundation.FakeActions
import usonia.kotlin.first
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryActionAccessTest {
    @Test
    fun publish() = runBlockingTest {
        val actions = InMemoryActionAccess()

        val result = async { actions.actions.first() }
        actions.publishAction(FakeActions.SwitchOff)

        assertEquals(FakeActions.SwitchOff, result.await())
    }
}
