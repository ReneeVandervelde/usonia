package usonia.rules.alerts

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import inkapplications.spondee.measure.us.fahrenheit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Test
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventAccessFake
import usonia.foundation.*
import usonia.server.DummyClient
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PipeMonitorTest {
    private val standardConfig = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
            users = setOf(FakeUsers.John),
            rooms = setOf(
                FakeRooms.LivingRoom.copy(
                    devices = setOf(FakeDevices.TemperatureSensor.copy(fixture = Fixture.Pipe))
                )
            )
        ))
    }

    private val testClient = DummyClient.copy(
        configurationAccess = standardConfig,
    )

    @Test
    fun sendAlert() = runTest {
        val events = EventAccessFake()
        val actionPublisherSpy = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = events,
            actionPublisher = actionPublisherSpy,
        )
        val monitor = PipeMonitor(client, backgroundScope = backgroundScope)

        val monitorJob = launch { monitor.startDaemon() }
        advanceUntilIdle()

        events.mutableEvents.emit(Event.Temperature(
            source = FakeDevices.TemperatureSensor.id,
            timestamp = Instant.DISTANT_PAST,
            temperature = 20.fahrenheit,
        ))
        advanceUntilIdle()

        assertEquals(1, actionPublisherSpy.actions.size, "Single Alert is sent.")
        val action = actionPublisherSpy.actions.single()

        assertTrue(action is Action.Alert, "Alert is sent")
        assertEquals(action.target, FakeUsers.John.id, "Alert is sent to site user")
        monitorJob.cancelAndJoin()
    }

    @Test
    fun noDuplicates() = runTest {
        val events = EventAccessFake()
        val actionPublisherSpy = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = events,
            actionPublisher = actionPublisherSpy,
        )
        val monitor = PipeMonitor(client, backgroundScope = backgroundScope)

        val monitorJob = launch { monitor.startDaemon() }
        advanceUntilIdle()

        events.mutableEvents.emit(Event.Temperature(
            source = FakeDevices.TemperatureSensor.id,
            timestamp = Instant.DISTANT_PAST,
            temperature = 20.fahrenheit,
        ))
        events.mutableEvents.emit(Event.Temperature(
            source = FakeDevices.TemperatureSensor.id,
            timestamp = Instant.DISTANT_PAST,
            temperature = 20.fahrenheit,
        ))
        advanceUntilIdle()

        assertEquals(1, actionPublisherSpy.actions.size, "Only a single Alert is sent.")
        monitorJob.cancelAndJoin()
    }

    @Test
    fun resetAfterThreshold() = runTest {
        val events = EventAccessFake()
        val actionPublisherSpy = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = events,
            actionPublisher = actionPublisherSpy,
        )
        val monitor = PipeMonitor(client, backgroundScope = backgroundScope)

        val monitorJob = launch { monitor.startDaemon() }
        advanceUntilIdle()

        events.mutableEvents.emit(Event.Temperature(
            source = FakeDevices.TemperatureSensor.id,
            timestamp = Instant.DISTANT_PAST,
            temperature = 20.fahrenheit,
        ))
        events.mutableEvents.emit(Event.Temperature(
            source = FakeDevices.TemperatureSensor.id,
            timestamp = Instant.DISTANT_PAST,
            temperature = 45.fahrenheit,
        ))
        events.mutableEvents.emit(Event.Temperature(
            source = FakeDevices.TemperatureSensor.id,
            timestamp = Instant.DISTANT_PAST,
            temperature = 20.fahrenheit,
        ))
        advanceUntilIdle()

        assertEquals(2, actionPublisherSpy.actions.size, "New alerts sent after reset.")
        monitorJob.cancelAndJoin()
    }
}
