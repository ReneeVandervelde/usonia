package usonia.rules.greenhouse

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.datetime.Instant
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventAccessFake
import usonia.foundation.*
import usonia.rules.locks.LockOnAway
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FanControlTest {
    private val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: Flow<Site> = flowOf(FakeSite.copy(
            rooms = setOf(
                FakeRooms.FakeGreenhouse.copy(
                    devices = setOf(FakeDevices.TemperatureSensor, FakeDevices.Switch.copy(fixture = Fixture.Fan))
                ),
                FakeRooms.LivingRoom.copy(
                    devices = setOf(
                        FakeDevices.TemperatureSensor.copy(id = Identifier("unrelated-sensor")),
                        FakeDevices.Switch.copy(id = Identifier("unrelated-switch")),
                    ),
                ),
            )
        ))
    }

    @Test
    fun turnOn() = runBlockingTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = FanControl(client)

        val daemonJob = launch { daemon.start() }
        runCurrent()
        fakeEvents.events.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 80f))
        assertEquals(1, actionSpy.actions.size, "Fan is switched on")
        val action = actionSpy.actions.single()
        assertTrue(action is Action.Switch)
        assertEquals(FakeDevices.Switch.id, action.target)
        assertEquals(SwitchState.ON, action.state)

        daemonJob.cancelAndJoin()
    }

    @Test
    fun inBuffer() = runBlockingTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = FanControl(client)

        val daemonJob = launch { daemon.start() }
        runCurrent()
        fakeEvents.events.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 79f))
        fakeEvents.events.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 78f))
        fakeEvents.events.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 77f))
        assertEquals(0, actionSpy.actions.size, "Fan not adjusted in buffer.")

        daemonJob.cancelAndJoin()
    }

    @Test
    fun turnOff() = runBlockingTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = FanControl(client)

        val daemonJob = launch { daemon.start() }
        runCurrent()
        fakeEvents.events.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 76f))
        assertEquals(1, actionSpy.actions.size, "Fan is switched off outside of buffer.")
        val action = actionSpy.actions.single()
        assertTrue(action is Action.Switch)
        assertEquals(FakeDevices.Switch.id, action.target)
        assertEquals(SwitchState.OFF, action.state)

        daemonJob.cancelAndJoin()
    }

    @Test
    fun unrelated() = runBlockingTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = FanControl(client)

        val daemonJob = launch { daemon.start() }
        runCurrent()
        fakeEvents.events.emit(Event.Temperature(Identifier("unrelated-sensor"), Instant.DISTANT_PAST, 99f))
        fakeEvents.events.emit(Event.Temperature(Identifier("unrelated-sensor"), Instant.DISTANT_PAST, 1f))
        assertEquals(0, actionSpy.actions.size, "No action taken on unrelated temperature event")
        daemonJob.cancelAndJoin()
    }
}
