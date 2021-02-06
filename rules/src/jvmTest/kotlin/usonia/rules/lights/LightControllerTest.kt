package usonia.rules.lights

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.datetime.Clock
import usonia.core.state.*
import usonia.foundation.*
import usonia.foundation.unit.ColorTemperature
import usonia.kotlin.unit.percent
import usonia.server.DummyClient
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

@OptIn(ExperimentalTime::class)
class LightControllerTest {
    val testSite = FakeSite.copy(
        rooms = setOf(FakeRooms.LivingRoom.copy(
            devices = setOf(
                FakeDevices.Motion,
                FakeDevices.HueGroup,
            )
        ))
    )
    val configurationAccess = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: Flow<Site> = flowOf(testSite)
    }
    val testClient = DummyClient.copy(
        configurationAccess = configurationAccess,
    )

    private val colorPicker = object: LightSettingsPicker {
        override suspend fun getRoomColor(room: Room) = LightSettings.Temperature(
            temperature = ColorTemperature(420),
            brightness = 75.percent,
        )
    }

    @Test
    fun lightsOn() = runBlockingTest {
        val eventAccess = EventAccessFake()
        val actionPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = eventAccess,
            actionPublisher = actionPublisher,
        )

        val controller = LightController(client, colorPicker, backgroundScope = this)
        val daemonJob = launch { controller.start() }

        pauseDispatcher {
            eventAccess.events.emit(Event.Motion(
                FakeDevices.Motion.id,
                Clock.System.now(),
                MotionState.MOTION
            ))
        }

        assertEquals(1, actionPublisher.actions.size, "One light action should be published.")
        val action = actionPublisher.actions.single()
        assertTrue(action is Action.ColorTemperatureChange)
        assertEquals(SwitchState.ON, action.switchState)

        daemonJob.cancelAndJoin()
    }

    @Test
    fun lightsOff() = runBlockingTest {
        val eventAccess = EventAccessFake()
        val actionPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = eventAccess,
            actionPublisher = actionPublisher,
        )

        val controller = LightController(client, colorPicker, backgroundScope = this)
        val daemonJob = launch { controller.start() }

        pauseDispatcher {
            eventAccess.events.emit(Event.Motion(
                FakeDevices.Motion.id,
                Clock.System.now(),
                MotionState.IDLE
            ))
            advanceUntilIdle()
            advanceTimeBy(31.minutes.inMilliseconds.toLong())
        }

        assertEquals(1, actionPublisher.actions.size, "One light action should be published.")
        val action = actionPublisher.actions.single()
        assertTrue(action is Action.Switch)
        assertEquals(SwitchState.OFF, action.state)

        daemonJob.cancelAndJoin()
    }

    @Test
    fun cancelled() = runBlockingTest {
        val eventAccess = EventAccessFake()
        val actionPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = eventAccess,
            actionPublisher = actionPublisher,
        )

        val controller = LightController(client, colorPicker, backgroundScope = this)
        val daemonJob = launch { controller.start() }

        eventAccess.events.emit(Event.Motion(
            FakeDevices.Motion.id,
            Clock.System.now(),
            MotionState.IDLE
        ))
        eventAccess.events.emit(Event.Motion(
            FakeDevices.Motion.id,
            Clock.System.now(),
            MotionState.MOTION
        ))
        runCurrent()
        advanceUntilIdle()

        assertEquals(1, actionPublisher.actions.size, "One light action should be published.")
        val action = actionPublisher.actions.single()
        assertTrue(action is Action.ColorTemperatureChange)
        assertEquals(SwitchState.ON, action.switchState)

        daemonJob.cancelAndJoin()
    }

    @Test
    fun away() = runBlockingTest {
        val configurationAccess = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: Flow<Site> = flowOf(testSite.copy(
                users = setOf(FakeUsers.John)
            ))
        }
        val eventAccess = object: EventAccess by EventAccessStub {
            override val events = MutableSharedFlow<Event>()
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? = when(type) {
                Event.Presence::class -> FakeEvents.Away as T
                else -> TODO()
            }
        }
        val actionPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = eventAccess,
            actionPublisher = actionPublisher,
            configurationAccess = configurationAccess,
        )

        val controller = LightController(client, colorPicker, backgroundScope = this)
        val daemonJob = launch { controller.start() }

        pauseDispatcher {
            eventAccess.events.emit(Event.Motion(
                FakeDevices.Motion.id,
                Clock.System.now(),
                MotionState.MOTION
            ))
        }

        assertEquals(0, actionPublisher.actions.size, "No light actions are published while user is away.")

        daemonJob.cancelAndJoin()
    }
}
