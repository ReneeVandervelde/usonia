package usonia.rules.greenhouse

import inkapplications.spondee.measure.us.fahrenheit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import usonia.core.state.*
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class HeatControlTest {
    private val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
            rooms = setOf(
                FakeRooms.FakeGreenhouse.copy(
                    devices = setOf(FakeDevices.TemperatureSensor, FakeDevices.Switch.copy(fixture = Fixture.Heat))
                ),
                FakeRooms.LivingRoom.copy(
                    devices = setOf(
                        FakeDevices.TemperatureSensor.copy(id = Identifier("unrelated-sensor")),
                        FakeDevices.Switch.copy(id = Identifier("unrelated-switch")),
                    ),
                ),
            ),
            users = setOf(FakeUsers.John),
        ))
    }

    @Test
    fun turnOn() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = HeatControl(client, backgroundScope = backgroundScope)

        val daemonJob = launch { daemon.start() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 73.fahrenheit))
        runCurrent()
        assertEquals(1, actionSpy.actions.size, "Heat is switched on")
        val action = actionSpy.actions.single()
        assertTrue(action is Action.Switch)
        assertEquals(FakeDevices.Switch.id, action.target)
        assertEquals(SwitchState.ON, action.state)

        daemonJob.cancelAndJoin()
    }

    @Test
    fun inBuffer() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = HeatControl(client, backgroundScope = backgroundScope)

        val daemonJob = launch { daemon.start() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 74.fahrenheit))
        fakeEvents.mutableEvents.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 75.fahrenheit))
        fakeEvents.mutableEvents.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 76.fahrenheit))
        assertEquals(0, actionSpy.actions.size, "Heat not adjusted in buffer.")

        daemonJob.cancelAndJoin()
    }

    @Test
    fun turnOff() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = HeatControl(client, backgroundScope = backgroundScope)

        val daemonJob = launch { daemon.start() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 77.fahrenheit))
        runCurrent()
        assertEquals(1, actionSpy.actions.size, "Heat is switched off outside of buffer.")
        val action = actionSpy.actions.single()
        assertTrue(action is Action.Switch)
        assertEquals(FakeDevices.Switch.id, action.target)
        assertEquals(SwitchState.OFF, action.state)

        daemonJob.cancelAndJoin()
    }

    @Test
    fun unrelated() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = HeatControl(client, backgroundScope = backgroundScope)

        val daemonJob = launch { daemon.start() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Temperature(Identifier("unrelated-sensor"), Instant.DISTANT_PAST, 99.fahrenheit))
        fakeEvents.mutableEvents.emit(Event.Temperature(Identifier("unrelated-sensor"), Instant.DISTANT_PAST, 1.fahrenheit))
        assertEquals(0, actionSpy.actions.size, "No action taken on unrelated temperature event")
        daemonJob.cancelAndJoin()
    }

    @Test
    fun dutyCycle() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = HeatControl(client, backgroundScope = backgroundScope)

        val daemonJob = launch { daemon.start() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 73.fahrenheit))
        runCurrent()
        assertEquals(1, actionSpy.actions.size, "Heat is switched on")
        val action = actionSpy.actions.single()
        assertTrue(action is Action.Switch)
        assertEquals(FakeDevices.Switch.id, action.target)
        assertEquals(SwitchState.ON, action.state)

        advanceTimeBy(1.hours.inWholeMilliseconds)
        runCurrent()
        assertEquals(2, actionSpy.actions.size, "Off command should be sent for duty cycle")
        val offAction = actionSpy.actions[1]
        assertTrue(offAction is Action.Switch)
        assertEquals(FakeDevices.Switch.id, offAction.target)
        assertEquals(SwitchState.OFF, offAction.state)

        advanceTimeBy(20.minutes.inWholeMilliseconds)
        runCurrent()
        assertEquals(3, actionSpy.actions.size, "Off command should be sent for duty cycle")
        val resumeAction = actionSpy.actions[2]
        assertTrue(resumeAction is Action.Switch)
        assertEquals(FakeDevices.Switch.id, resumeAction.target)
        assertEquals(SwitchState.ON, resumeAction.state)

        daemonJob.cancelAndJoin()
    }

    @Test
    fun dutyCycleCancels() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = HeatControl(client, backgroundScope = backgroundScope)

        val daemonJob = launch { daemon.start() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 73.fahrenheit))
        runCurrent()
        assertEquals(1, actionSpy.actions.size, "Heat is switched on")
        val action = actionSpy.actions.single()
        assertTrue(action is Action.Switch)
        assertEquals(FakeDevices.Switch.id, action.target)
        assertEquals(SwitchState.ON, action.state)


        fakeEvents.mutableEvents.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 77.fahrenheit))
        runCurrent()

        assertEquals(2, actionSpy.actions.size, "Off command should be sent for duty cycle")
        val offAction = actionSpy.actions[1]
        assertTrue(offAction is Action.Switch)
        assertEquals(FakeDevices.Switch.id, offAction.target)
        assertEquals(SwitchState.OFF, offAction.state)

        advanceTimeBy(24.hours.inWholeMilliseconds)
        runCurrent()
        assertEquals(2, actionSpy.actions.size, "No additional commands sent after duty cycle")

        daemonJob.cancelAndJoin()
    }

    @Test
    fun awayShift() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = object: EventAccessFake() {
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return when (type) {
                    Event.Presence::class -> Event.Presence(id, Instant.DISTANT_PAST, PresenceState.AWAY) as T
                    else -> null
                }
            }
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = HeatControl(client, backgroundScope = backgroundScope)

        val daemonJob = launch { daemon.start() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 66.fahrenheit))
        runCurrent()
        assertEquals(0, actionSpy.actions.size, "Heat is not switched on per usual while away")

        fakeEvents.mutableEvents.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 65.fahrenheit))
        runCurrent()
        assertEquals(1, actionSpy.actions.size, "Heat is turned on at a lower temp while away")
        val action = actionSpy.actions.single()
        assertTrue(action is Action.Switch)
        assertEquals(FakeDevices.Switch.id, action.target)
        assertEquals(SwitchState.ON, action.state)

        fakeEvents.mutableEvents.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 69.fahrenheit))
        runCurrent()
        assertEquals(2, actionSpy.actions.size, "Heat is turned off at a lower temp while away")
        val offAction = actionSpy.actions[1]
        assertTrue(offAction is Action.Switch)
        assertEquals(FakeDevices.Switch.id, offAction.target)
        assertEquals(SwitchState.OFF, offAction.state)

        advanceTimeBy(24.hours.inWholeMilliseconds)
        runCurrent()
        assertEquals(2, actionSpy.actions.size, "No additional commands sent after duty cycle")

        daemonJob.cancelAndJoin()
    }
}
