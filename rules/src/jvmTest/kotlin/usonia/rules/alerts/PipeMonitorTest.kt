package usonia.rules.alerts

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.datetime.Instant
import org.junit.Test
import usonia.foundation.*
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventAccessFake
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun sendAlert() = runBlockingTest {
        val events = EventAccessFake()
        val actionPublisherSpy = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = events,
            actionPublisher = actionPublisherSpy,
        )
        val monitor = PipeMonitor(client, backgroundScope = this)

        val monitorJob = launch { monitor.start() }

        pauseDispatcher {
            events.mutableEvents.emit(Event.Temperature(
                source = FakeDevices.TemperatureSensor.id,
                timestamp = Instant.DISTANT_PAST,
                temperature = 20f,
            ))
        }

        assertEquals(1, actionPublisherSpy.actions.size, "Single Alert is sent.")
        val action = actionPublisherSpy.actions.single()

        assertTrue(action is Action.Alert, "Alert is sent")
        assertEquals(action.target, FakeUsers.John.id, "Alert is sent to site user")
        monitorJob.cancelAndJoin()
    }

    @Test
    fun noDuplicates() = runBlockingTest {
        val events = EventAccessFake()
        val actionPublisherSpy = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = events,
            actionPublisher = actionPublisherSpy,
        )
        val monitor = PipeMonitor(client, backgroundScope = this)

        val monitorJob = launch { monitor.start() }

        pauseDispatcher {
            events.mutableEvents.emit(Event.Temperature(
                source = FakeDevices.TemperatureSensor.id,
                timestamp = Instant.DISTANT_PAST,
                temperature = 20f,
            ))
            events.mutableEvents.emit(Event.Temperature(
                source = FakeDevices.TemperatureSensor.id,
                timestamp = Instant.DISTANT_PAST,
                temperature = 20f,
            ))
        }

        assertEquals(1, actionPublisherSpy.actions.size, "Only a single Alert is sent.")
        monitorJob.cancelAndJoin()
    }

    @Test
    fun resetAfterThreshold() = runBlockingTest {
        val events = EventAccessFake()
        val actionPublisherSpy = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = events,
            actionPublisher = actionPublisherSpy,
        )
        val monitor = PipeMonitor(client, backgroundScope = this)

        val monitorJob = launch { monitor.start() }

        pauseDispatcher {
            events.mutableEvents.emit(Event.Temperature(
                source = FakeDevices.TemperatureSensor.id,
                timestamp = Instant.DISTANT_PAST,
                temperature = 20f,
            ))
            events.mutableEvents.emit(Event.Temperature(
                source = FakeDevices.TemperatureSensor.id,
                timestamp = Instant.DISTANT_PAST,
                temperature = 45f,
            ))
            events.mutableEvents.emit(Event.Temperature(
                source = FakeDevices.TemperatureSensor.id,
                timestamp = Instant.DISTANT_PAST,
                temperature = 20f,
            ))
        }

        assertEquals(2, actionPublisherSpy.actions.size, "New alerts sent after reset.")
        monitorJob.cancelAndJoin()
    }
}
