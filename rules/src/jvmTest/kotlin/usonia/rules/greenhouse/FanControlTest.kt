package usonia.rules.greenhouse

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import inkapplications.spondee.measure.us.fahrenheit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventAccessFake
import usonia.foundation.*
import usonia.rules.DummyFailureHandler
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class FanControlTest {
    private val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
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
    fun turnOn() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = FanControl(
            client = client,
            failureHandler = DummyFailureHandler,
            backgroundScope = backgroundScope,
        )

        val daemonJob = launch { daemon.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 80.fahrenheit))
        runCurrent()
        assertEquals(1, actionSpy.actions.size, "Fan is switched on")
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
        val daemon = FanControl(
            client = client,
            failureHandler = DummyFailureHandler,
            backgroundScope = backgroundScope,
        )

        val daemonJob = launch { daemon.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 79.fahrenheit))
        fakeEvents.mutableEvents.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 78.fahrenheit))
        fakeEvents.mutableEvents.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 77.fahrenheit))
        assertEquals(0, actionSpy.actions.size, "Fan not adjusted in buffer.")

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
        val daemon = FanControl(
            client = client,
            failureHandler = DummyFailureHandler,
            backgroundScope = backgroundScope,
        )

        val daemonJob = launch { daemon.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 76.fahrenheit))
        runCurrent()
        assertEquals(1, actionSpy.actions.size, "Fan is switched off outside of buffer.")
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
        val daemon = FanControl(
            client = client,
            failureHandler = DummyFailureHandler,
            backgroundScope = backgroundScope,
        )

        val daemonJob = launch { daemon.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Temperature(Identifier("unrelated-sensor"), Instant.DISTANT_PAST, 99.fahrenheit))
        fakeEvents.mutableEvents.emit(Event.Temperature(Identifier("unrelated-sensor"), Instant.DISTANT_PAST, 1.fahrenheit))
        assertEquals(0, actionSpy.actions.size, "No action taken on unrelated temperature event")
        daemonJob.cancelAndJoin()
    }

    @Test
    fun periodicFan()
    {
        runTest {
            val actionSpy = ActionPublisherSpy()
            val fakeEvents = EventAccessFake()
            val client = DummyClient.copy(
                configurationAccess = fakeConfig,
                eventAccess = fakeEvents,
                actionPublisher = actionSpy,
            )
            val control = FanControl(
                client = client,
                failureHandler = DummyFailureHandler,
                backgroundScope = backgroundScope,
            )

            backgroundScope.launch {
                control.runCron(Instant.DISTANT_PAST.toLocalDateTime(TimeZone.UTC), TimeZone.UTC)
            }
            runCurrent()
            assertEquals(1, actionSpy.actions.size, "Fan is switched on")
            val action = actionSpy.actions[0]
            assertTrue(action is Action.Switch)
            assertEquals(FakeDevices.Switch.id, action.target)
            assertEquals(SwitchState.ON, action.state)

            advanceTimeBy(10.minutes)
            runCurrent()
            assertEquals(2, actionSpy.actions.size, "Fan is switched off")
            val action2 = actionSpy.actions[1]
            assertTrue(action2 is Action.Switch)
            assertEquals(FakeDevices.Switch.id, action2.target)
            assertEquals(SwitchState.OFF, action2.state)
        }
    }

    @Test
    fun periodicFanWhileCooling()
    {
        runTest {
            val actionSpy = ActionPublisherSpy()
            val fakeEvents = EventAccessFake()
            val client = DummyClient.copy(
                configurationAccess = fakeConfig,
                eventAccess = fakeEvents,
                actionPublisher = actionSpy,
            )
            val control = FanControl(
                client = client,
                failureHandler = DummyFailureHandler,
                backgroundScope = backgroundScope,
            )

            backgroundScope.launch { control.startDaemon() }
            runCurrent()
            assertEquals(0, actionSpy.actions.size, "No events before cron or cooling")

            fakeEvents.mutableEvents.emit(Event.Temperature(FakeDevices.TemperatureSensor.id, Instant.DISTANT_PAST, 80.fahrenheit))
            runCurrent()
            assertEquals(1, actionSpy.actions.size, "Fan is switched on for cooling")
            val coolAction = actionSpy.actions[0]
            assertTrue(coolAction is Action.Switch)
            assertEquals(FakeDevices.Switch.id, coolAction.target)
            assertEquals(SwitchState.ON, coolAction.state)

            backgroundScope.launch {
                control.runCron(Instant.DISTANT_PAST.toLocalDateTime(TimeZone.UTC), TimeZone.UTC)
            }
            runCurrent()
            assertEquals(2, actionSpy.actions.size, "Fan is switched on for periodic fan")
            val periodicAction = actionSpy.actions[1]
            assertTrue(periodicAction is Action.Switch)
            assertEquals(FakeDevices.Switch.id, periodicAction.target)
            assertEquals(SwitchState.ON, periodicAction.state)

            advanceTimeBy(1.hours)
            runCurrent()
            assertEquals(2, actionSpy.actions.size, "Fan is NOT switched off by periodic fan")
        }
    }
}
