package usonia.rules.locks

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventAccessFake
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LockOnAwayTest {
    private val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
            users = setOf(FakeUsers.John),
            rooms = setOf(FakeRooms.FakeBedroom.copy(
                devices = setOf(FakeDevices.Lock)
            ))
        ))
    }

    @Test
    fun lock() = runBlockingTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = object: EventAccessFake() {
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? = Event.Presence(
                source = id,
                timestamp = Instant.DISTANT_PAST,
                state = PresenceState.AWAY,
            ) as T
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = LockOnAway(client)

        val daemonJob = launch { daemon.start() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Presence(FakeUsers.John.id, Clock.System.now(), PresenceState.AWAY))
        assertEquals(1, actionSpy.actions.size, "Locks are locked immediately")
        val action = actionSpy.actions.single()
        assertTrue(action is Action.Lock)
        assertEquals(FakeDevices.Lock.id, action.target)
        assertEquals(LockState.LOCKED, action.state)

        daemonJob.cancelAndJoin()
    }

    @Test
    fun noopOnHome() = runBlockingTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = object: EventAccessFake() {
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? = Event.Presence(
                source = id,
                timestamp = Instant.DISTANT_PAST,
                state = PresenceState.HOME,
            ) as T
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = LockOnAway(client)

        val daemonJob = launch { daemon.start() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Presence(FakeUsers.John.id, Clock.System.now(), PresenceState.HOME))
        assertEquals(0, actionSpy.actions.size, "Not locked if home")

        daemonJob.cancelAndJoin()
    }

    @Test
    fun noopOnPartial() = runBlockingTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = object: EventAccessFake() {
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? = Event.Presence(
                source = id,
                timestamp = Instant.DISTANT_PAST,
                state = PresenceState.HOME,
            ) as T
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = LockOnAway(client)

        val daemonJob = launch { daemon.start() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Presence(FakeUsers.John.id, Clock.System.now(), PresenceState.AWAY))
        assertEquals(0, actionSpy.actions.size, "Not locked if some user is home")

        daemonJob.cancelAndJoin()
    }
}
