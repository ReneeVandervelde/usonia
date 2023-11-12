package usonia.rules.alerts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
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
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DoorAlertTest {
    private val entryConfig = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
            users = setOf(FakeUsers.John, FakeUsers.Jane),
            rooms = setOf(FakeRooms.LivingRoom.copy(
                devices = setOf(FakeDevices.Latch.copy(
                    fixture = Fixture.EntryPoint,
                )),
            )),
        ))
    }

    @Test
    fun sendAlert() = runTest {
        val fakeEvents = object: EventAccessFake() {
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return Event.Presence(id, Instant.DISTANT_PAST, PresenceState.AWAY) as T
            }
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = entryConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )

        val daemon = launch { DoorAlert(client).startDaemon() }
        advanceUntilIdle()

        fakeEvents.mutableEvents.emit(Event.Latch(FakeDevices.Latch.id, Instant.DISTANT_PAST, LatchState.OPEN))
        advanceUntilIdle()

        assertEquals(2, actionSpy.actions.size, "Alert sent for each user.")
        assertTrue(actionSpy.actions.all { it is Action.Alert }, "All actions are alerts.")

        daemon.cancel()
    }

    @Test
    fun home() = runTest {
        val fakeEvents = object: EventAccessFake() {
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return when (id) {
                    FakeUsers.John.id -> Event.Presence(id, Instant.DISTANT_PAST, PresenceState.AWAY)
                    FakeUsers.Jane.id -> Event.Presence(id, Instant.DISTANT_PAST, PresenceState.HOME)
                    else -> TODO()
                } as T
            }
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = entryConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )

        val daemon = launch { DoorAlert(client).startDaemon() }
        advanceUntilIdle()

        fakeEvents.mutableEvents.emit(Event.Latch(FakeDevices.Latch.id, Instant.DISTANT_PAST, LatchState.OPEN))
        advanceUntilIdle()

        assertEquals(0, actionSpy.actions.size, "No alert sent when a user is home.")

        daemon.cancel()
    }

    @Test
    fun nonEntryPoint() = runTest {
        val fakeEvents = object: EventAccessFake() {
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return Event.Presence(id, Instant.DISTANT_PAST, PresenceState.AWAY) as T
            }
        }
        val config = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
                users = setOf(FakeUsers.John),
                rooms = setOf(FakeRooms.LivingRoom.copy(
                    devices = setOf(FakeDevices.Latch),
                )),
            ))
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = config,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )

        val daemon = launch { DoorAlert(client).startDaemon() }
        advanceUntilIdle()

        fakeEvents.mutableEvents.emit(Event.Latch(FakeDevices.Latch.id, Instant.DISTANT_PAST, LatchState.OPEN))
        advanceUntilIdle()

        assertEquals(0, actionSpy.actions.size, "No alert sent for non-entrypoint.")

        daemon.cancel()
    }

    @Test
    fun closed() = runTest {
        val fakeEvents = object: EventAccessFake() {
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return Event.Presence(id, Instant.DISTANT_PAST, PresenceState.AWAY) as T
            }
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = entryConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )

        val daemon = launch { DoorAlert(client).startDaemon() }
        advanceUntilIdle()

        fakeEvents.mutableEvents.emit(Event.Latch(FakeDevices.Latch.id, Instant.DISTANT_PAST, LatchState.CLOSED))
        advanceUntilIdle()

        assertEquals(0, actionSpy.actions.size, "No alert sent for close event.")

        daemon.cancel()
    }
}
