package usonia.rules.locks

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventAccessFake
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LockJammedTest {
    private val fakeSite = FakeSite.copy(
        rooms = setOf(FakeRooms.FakeBedroom.copy(
            devices = setOf(FakeDevices.Lock.copy(
                id = Identifier("test-lock"),
            )),
        )),
        users = setOf(
            FakeUsers.John.copy(
                id = Identifier("test-user"),
                alertLevel = Action.Alert.Level.Debug,
            )
        )
    )
    private val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(fakeSite)
    }

    @Test
    fun alertSentOnUnknownEvent() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val alerts = LockJammed(client)

        val daemon = launch { alerts.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Lock(
            source = Identifier("test-lock"),
            timestamp = Instant.DISTANT_PAST,
            state = LockState.UNKNOWN,
            method = Event.Lock.LockMethod.COMMAND,
            code = null,
        ))
        advanceUntilIdle()

        assertEquals(1, actionSpy.actions.size)
        val alert = actionSpy.actions.first()
        assertTrue(alert is Action.Alert)
        assertEquals(Action.Alert.Level.Warning, alert.level)
        assertEquals(Action.Alert.Icon.Panic, alert.icon)
        assertEquals("test-user", alert.target.value)

        daemon.cancelAndJoin()
    }

    @Test
    fun noAlertSentOnNormalLock() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val alerts = LockJammed(client)

        val daemon = launch { alerts.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Lock(
            source = Identifier("test-lock"),
            timestamp = Instant.DISTANT_PAST,
            state = LockState.LOCKED,
            method = Event.Lock.LockMethod.COMMAND,
            code = null,
        ))
        advanceUntilIdle()

        assertEquals(0, actionSpy.actions.size)

        daemon.cancelAndJoin()
    }
}
