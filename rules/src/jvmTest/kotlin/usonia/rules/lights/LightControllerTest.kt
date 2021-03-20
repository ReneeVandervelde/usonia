package usonia.rules.lights

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.datetime.Clock
import usonia.core.state.*
import usonia.foundation.*
import usonia.foundation.unit.ColorTemperature
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.kotlin.unit.percent
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds

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
        override val site: OngoingFlow<Site> = ongoingFlowOf(testSite)
    }
    val testClient = DummyClient.copy(
        configurationAccess = configurationAccess,
    )

    private val settingsPicker = object: LightSettingsPicker {
        override suspend fun getActiveSettings(room: Room) = LightSettings.Temperature(
            temperature = ColorTemperature(420),
            brightness = 75.percent,
        )

        override suspend fun getIdleSettings(room: Room): LightSettings {
            return LightSettings.Switch(SwitchState.OFF)
        }

        override suspend fun getIdleConditions(room: Room): IdleConditions = IdleConditions.Timed(1.seconds)
    }

    @Test
    fun lightsOn() = runBlockingTest {
        val eventAccess = EventAccessFake()
        val actionPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = eventAccess,
            actionPublisher = actionPublisher,
        )

        val controller = LightController(client, settingsPicker, backgroundScope = this)
        val daemonJob = launch { controller.start() }

        pauseDispatcher {
            eventAccess.mutableEvents.emit(Event.Motion(
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
    fun settingsIgnore() = runBlockingTest {
        val eventAccess = EventAccessFake()
        val actionPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = eventAccess,
            actionPublisher = actionPublisher,
        )
        val settingsPicker = object: LightSettingsPicker {
            override suspend fun getActiveSettings(room: Room): LightSettings = LightSettings.Ignore
        }

        val controller = LightController(client, settingsPicker, backgroundScope = this)
        val daemonJob = launch { controller.start() }

        pauseDispatcher {
            eventAccess.mutableEvents.emit(Event.Motion(
                FakeDevices.Motion.id,
                Clock.System.now(),
                MotionState.MOTION
            ))
        }

        assertEquals(0, actionPublisher.actions.size, "No action taken when unhandled settings.")

        daemonJob.cancelAndJoin()
    }

    @Test
    fun settingsUnhandled() = runBlockingTest {
        val eventAccess = EventAccessFake()
        val actionPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = eventAccess,
            actionPublisher = actionPublisher,
        )
        val settingsPicker = object: LightSettingsPicker {
            override suspend fun getActiveSettings(room: Room): LightSettings = LightSettings.Unhandled
        }

        val controller = LightController(client, settingsPicker, backgroundScope = this)
        val daemonJob = launch { controller.start() }

        pauseDispatcher {
            eventAccess.mutableEvents.emit(Event.Motion(
                FakeDevices.Motion.id,
                Clock.System.now(),
                MotionState.MOTION
            ))
        }

        assertEquals(0, actionPublisher.actions.size, "No action taken when unhandled settings.")

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

        val controller = LightController(client, settingsPicker, backgroundScope = this)
        val daemonJob = launch { controller.start() }

        pauseDispatcher {
            eventAccess.mutableEvents.emit(Event.Motion(
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

        val controller = LightController(client, settingsPicker, backgroundScope = this)
        val daemonJob = launch { controller.start() }

        eventAccess.mutableEvents.emit(Event.Motion(
            FakeDevices.Motion.id,
            Clock.System.now(),
            MotionState.IDLE
        ))
        eventAccess.mutableEvents.emit(Event.Motion(
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
}
