package usonia.core.state.memory

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runBlockingTest
import usonia.foundation.Event
import usonia.foundation.FakeDevices
import usonia.foundation.FakeEvents
import usonia.kotlin.first
import kotlin.test.Test
import kotlin.test.*

class InMemoryEventAccessTest {
    @Test
    fun testPublish() = runBlockingTest {
        val events = InMemoryEventAccess()

        val result = async { events.events.first() }
        events.publishEvent(FakeEvents.SwitchOff)

        assertEquals(FakeEvents.SwitchOff, result.await())
    }

    @Test
    fun testStateTracking() = runBlockingTest {
        val events = InMemoryEventAccess()

        events.publishEvent(FakeEvents.SwitchOff)
        val currentState = events.getState(FakeDevices.Switch.id, Event.Switch::class)

        assertEquals(FakeEvents.SwitchOff, currentState, "Event action is stored")
    }

    @Test
    fun defaultState() = runBlockingTest {
        val events = InMemoryEventAccess()

        val currentState = events.getState(FakeDevices.Switch.id, Event.Switch::class)

        assertNull(currentState, "Source with no events results in null state")
    }

    @Test
    fun overwriteState() = runBlockingTest {
        val events = InMemoryEventAccess()

        events.publishEvent(FakeEvents.SwitchOff)
        events.publishEvent(FakeEvents.SwitchOn)
        val currentState = events.getState(FakeDevices.Switch.id, Event.Switch::class)

        assertEquals(FakeEvents.SwitchOn, currentState, "Most recent event is stored in memory")
    }
}
