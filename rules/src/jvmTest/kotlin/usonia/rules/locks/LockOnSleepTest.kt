package usonia.rules.locks

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.asOngoing
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventAccessStub
import usonia.foundation.*
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LockOnSleepTest {
    private val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
        val mutableFlags = MutableSharedFlow<Map<String, String?>>()
        override val flags = mutableFlags.asOngoing()
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
            rooms = setOf(FakeRooms.FakeBedroom.copy(
                devices = setOf(FakeDevices.Lock)
            ))
        ))
    }

    @Test
    fun lock() = runTest {
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = EventAccessStub,
            actionPublisher = actionSpy,
        )
        val picker = LockOnSleep(client)

        val daemon = launch { picker.startDaemon() }
        runCurrent()
        fakeConfig.mutableFlags.emit(mapOf("Sleep Mode" to "true"))
        runCurrent()
        assertEquals(1, actionSpy.actions.size, "Locks are locked immediately")
        val action = actionSpy.actions.single()
        assertTrue(action is Action.Lock)
        assertEquals(FakeDevices.Lock.id, action.target)
        assertEquals(LockState.LOCKED, action.state)

        daemon.cancelAndJoin()
    }

    @Test
    fun noop() = runTest {
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = EventAccessStub,
            actionPublisher = actionSpy,
        )
        val picker = LockOnSleep(client)

        val daemon = launch { picker.startDaemon() }
        fakeConfig.mutableFlags.emit(mapOf("Sleep Mode" to "false"))
        runCurrent()
        assertEquals(0, actionSpy.actions.size, "Locks not locked if not entering sleep mode")

        daemon.cancelAndJoin()
    }
}
