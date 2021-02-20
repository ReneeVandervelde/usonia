package usonia.rules.locks

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventAccessStub
import usonia.foundation.*
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LockOnSleepTest {
    private val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
        override val flags = MutableSharedFlow<Map<String, String?>>()
        override val site: Flow<Site> = flowOf(FakeSite.copy(
            rooms = setOf(FakeRooms.FakeBedroom.copy(
                devices = setOf(FakeDevices.Lock)
            ))
        ))
    }

    @Test
    fun lock() = runBlockingTest {
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = EventAccessStub,
            actionPublisher = actionSpy,
        )
        val picker = LockOnSleep(client)

        val daemon = launch { picker.start() }
        fakeConfig.flags.emit(mapOf("Sleep Mode" to "true"))
        runCurrent()
        assertEquals(1, actionSpy.actions.size, "Locks are locked immediately")
        val action = actionSpy.actions.single()
        assertTrue(action is Action.Lock)
        assertEquals(FakeDevices.Lock.id, action.target)
        assertEquals(LockState.LOCKED, action.state)

        daemon.cancelAndJoin()
    }

    @Test
    fun noop() = runBlockingTest {
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = EventAccessStub,
            actionPublisher = actionSpy,
        )
        val picker = LockOnSleep(client)

        val daemon = launch { picker.start() }
        fakeConfig.flags.emit(mapOf("Sleep Mode" to "false"))
        runCurrent()
        assertEquals(0, actionSpy.actions.size, "Locks are locked immediately")

        daemon.cancelAndJoin()
    }
}
