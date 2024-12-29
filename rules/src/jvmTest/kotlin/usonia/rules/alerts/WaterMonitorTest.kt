package usonia.rules.alerts

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
    fun sendAlert() = runTest {
        val events = EventAccessFake()
        val actionPublisherSpy = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = events,
            actionPublisher = actionPublisherSpy,
        )
        val monitor = WaterMonitor(client, backgroundScope = this)

        val monitorJob = launch { monitor.startDaemon() }
        advanceUntilIdle()

        events.mutableEvents.emit(FakeEvents.Wet)
        advanceUntilIdle()
        val action = actionPublisherSpy.actions.single()

        assertTrue(action is Action.Alert, "Alert is sent")
        assertEquals(action.target, FakeUsers.John.id, "Alert is sent to site user")
        monitorJob.cancelAndJoin()
    }

    @Test
    fun noDuplicateEvents() = runTest {
        val events = EventAccessFake()
        val actionPublisherSpy = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = events,
            actionPublisher = actionPublisherSpy,
        )
        val monitor = WaterMonitor(client, backgroundScope = this)

        val monitorJob = launch { monitor.startDaemon() }
        advanceUntilIdle()

        events.mutableEvents.emit(FakeEvents.Wet)
        events.mutableEvents.emit(FakeEvents.Wet)
        advanceUntilIdle()


        assertEquals(1, actionPublisherSpy.actions.size, "Only one alert sent per wet event")
        monitorJob.cancelAndJoin()
    }

    @Test
    fun resetAfterDry() = runTest {
        val events = EventAccessFake()
        val actionPublisherSpy = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = events,
            actionPublisher = actionPublisherSpy,
        )
        val monitor = WaterMonitor(client, backgroundScope = this)

        val monitorJob = launch { monitor.startDaemon() }
        advanceUntilIdle()

        events.mutableEvents.emit(FakeEvents.Wet)
        events.mutableEvents.emit(FakeEvents.Dry)
        events.mutableEvents.emit(FakeEvents.Wet)
        advanceUntilIdle()

        assertEquals(2, actionPublisherSpy.actions.size, "Alerts resume after dry")
        monitorJob.cancelAndJoin()
    }

    @Test
    fun loneDryEvent() = runTest {
        val events = EventAccessFake()
        val actionPublisherSpy = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = events,
            actionPublisher = actionPublisherSpy,
        )
        val monitor = WaterMonitor(client, backgroundScope = this)

        val monitorJob = launch { monitor.startDaemon() }
        advanceUntilIdle()

        events.mutableEvents.emit(FakeEvents.Dry)
        advanceUntilIdle()

        assertEquals(0, actionPublisherSpy.actions.size, "No alerts sent")
        monitorJob.cancelAndJoin()
    }

    @Test
    fun unrelatedEvent() = runTest {
        val events = EventAccessFake()
        val actionPublisherSpy = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = events,
            actionPublisher = actionPublisherSpy,
        )
        val monitor = WaterMonitor(client, backgroundScope = this)

        val monitorJob = launch { monitor.startDaemon() }
        advanceUntilIdle()

        events.mutableEvents.emit(FakeEvents.SwitchOff)
        advanceUntilIdle()

        assertEquals(0, actionPublisherSpy.actions.size, "No alerts sent")
        monitorJob.cancelAndJoin()
    }
}
