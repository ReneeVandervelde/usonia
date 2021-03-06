package usonia.rules.alerts

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventAccessFake
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WaterMonitorTest {
    private val standardConfig = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
            users = setOf(FakeUsers.John),
            rooms = setOf(
                FakeRooms.LivingRoom.copy(
                    devices = setOf(FakeDevices.WaterSensor)
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
        val monitor = WaterMonitor(client, backgroundScope = this)

        val monitorJob = launch { monitor.start() }

        pauseDispatcher {
            events.mutableEvents.emit(FakeEvents.Wet)
        }
        val action = actionPublisherSpy.actions.single()

        assertTrue(action is Action.Alert, "Alert is sent")
        assertEquals(action.target, FakeUsers.John.id, "Alert is sent to site user")
        monitorJob.cancelAndJoin()
    }

    @Test
    fun noDuplicateEvents() = runBlockingTest {
        val events = EventAccessFake()
        val actionPublisherSpy = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = events,
            actionPublisher = actionPublisherSpy,
        )
        val monitor = WaterMonitor(client, backgroundScope = this)

        val monitorJob = launch { monitor.start() }

        pauseDispatcher {
            events.mutableEvents.emit(FakeEvents.Wet)
            events.mutableEvents.emit(FakeEvents.Wet)
        }


        assertEquals(1, actionPublisherSpy.actions.size, "Only one alert sent per wet event")
        monitorJob.cancelAndJoin()
    }

    @Test
    fun resetAfterDry() = runBlockingTest {
        val events = EventAccessFake()
        val actionPublisherSpy = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = events,
            actionPublisher = actionPublisherSpy,
        )
        val monitor = WaterMonitor(client, backgroundScope = this)

        val monitorJob = launch { monitor.start() }

        pauseDispatcher {
            events.mutableEvents.emit(FakeEvents.Wet)
            events.mutableEvents.emit(FakeEvents.Dry)
            events.mutableEvents.emit(FakeEvents.Wet)
        }

        assertEquals(2, actionPublisherSpy.actions.size, "Alerts resume after dry")
        monitorJob.cancelAndJoin()
    }

    @Test
    fun loneDryEvent() = runBlockingTest {
        val events = EventAccessFake()
        val actionPublisherSpy = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = events,
            actionPublisher = actionPublisherSpy,
        )
        val monitor = WaterMonitor(client, backgroundScope = this)

        val monitorJob = launch { monitor.start() }

        pauseDispatcher {
            events.mutableEvents.emit(FakeEvents.Dry)
        }

        assertEquals(0, actionPublisherSpy.actions.size, "No alerts sent")
        monitorJob.cancelAndJoin()
    }

    @Test
    fun unrelatedEvent() = runBlockingTest {
        val events = EventAccessFake()
        val actionPublisherSpy = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = events,
            actionPublisher = actionPublisherSpy,
        )
        val monitor = WaterMonitor(client, backgroundScope = this)

        val monitorJob = launch { monitor.start() }

        pauseDispatcher {
            events.mutableEvents.emit(FakeEvents.SwitchOff)
        }

        assertEquals(0, actionPublisherSpy.actions.size, "No alerts sent")
        monitorJob.cancelAndJoin()
    }
}
