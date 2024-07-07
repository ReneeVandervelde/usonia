package usonia.rules.locks

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.asOngoing
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LockOnSecureTest {
    private val securityEvents = MutableStateFlow(SecurityState.Disarmed)
    private val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
            users = setOf(FakeUsers.John),
            rooms = setOf(FakeRooms.FakeBedroom.copy(
                devices = setOf(FakeDevices.Lock)
            ))
        ))
        override val securityState: OngoingFlow<SecurityState> = securityEvents.asOngoing()
    }

    @Test
    fun lock() = runTest {
        val actionSpy = ActionPublisherSpy()
        securityEvents.value = SecurityState.Disarmed
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionPublisher = actionSpy,
        )
        val daemon = LockOnSecure(client)

        val daemonJob = launch { daemon.startDaemon() }
        runCurrent()
        securityEvents.value = SecurityState.Armed
        runCurrent()

        assertEquals(1, actionSpy.actions.size, "Locks are locked immediately")
        val action = actionSpy.actions.single()
        assertTrue(action is Action.Lock)
        assertEquals(FakeDevices.Lock.id, action.target)
        assertEquals(LockState.LOCKED, action.state)

        daemonJob.cancelAndJoin()
    }

    @Test
    fun noopOnDisarm() = runTest {
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionPublisher = actionSpy,
        )
        val daemon = LockOnSecure(client)

        val daemonJob = launch { daemon.startDaemon() }
        runCurrent()
        securityEvents.value = SecurityState.Disarmed
        assertEquals(0, actionSpy.actions.size, "Not locked if home")

        daemonJob.cancelAndJoin()
    }
}
