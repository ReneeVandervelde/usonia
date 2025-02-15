package usonia.rules.lights

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import inkapplications.spondee.measure.metric.kelvin
import inkapplications.spondee.scalar.percent
import inkapplications.spondee.structure.toDouble
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventAccessFake
import usonia.foundation.*
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
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
            temperature = 420.kelvin,
            brightness = 75.percent,
        )

        override suspend fun getIdleSettings(room: Room): LightSettings {
            return LightSettings.Switch(SwitchState.OFF)
        }

        override suspend fun getStartIdleSettings(room: Room): LightSettings {
            return LightSettings.Brightness(
                brightness = 1.percent,
            )
        }

        override suspend fun getIdleConditions(room: Room): IdleConditions = IdleConditions.Timed(1.seconds)
    }

    @Test
    fun lightsOn() = runTest {
        val eventAccess = EventAccessFake()
        val actionPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = eventAccess,
            actionPublisher = actionPublisher,
        )

        val controller = LightController(client, settingsPicker, backgroundScope = backgroundScope)
        val daemonJob = launch { controller.startDaemon() }
        runCurrent()

        eventAccess.mutableEvents.emit(Event.Motion(
            FakeDevices.Motion.id,
            Clock.System.now(),
            MotionState.MOTION
        ))
        runCurrent()

        assertEquals(1, actionPublisher.actions.size, "One light action should be published.")
        val action = actionPublisher.actions.single()
        assertTrue(action is Action.ColorTemperatureChange)
        assertEquals(SwitchState.ON, action.switchState)

        daemonJob.cancelAndJoin()
    }

    @Test
    fun settingsIgnore() = runTest {
        val eventAccess = EventAccessFake()
        val actionPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = eventAccess,
            actionPublisher = actionPublisher,
        )
        val settingsPicker = object: LightSettingsPicker {
            override suspend fun getActiveSettings(room: Room): LightSettings = LightSettings.Ignore
        }

        val controller = LightController(client, settingsPicker, backgroundScope = backgroundScope)
        val daemonJob = launch { controller.startDaemon() }

        eventAccess.mutableEvents.emit(Event.Motion(
            FakeDevices.Motion.id,
            Clock.System.now(),
            MotionState.MOTION
        ))
        runCurrent()

        assertEquals(0, actionPublisher.actions.size, "No action taken when unhandled settings.")

        daemonJob.cancelAndJoin()
    }

    @Test
    fun settingsUnhandled() = runTest {
        val eventAccess = EventAccessFake()
        val actionPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = eventAccess,
            actionPublisher = actionPublisher,
        )
        val settingsPicker = object: LightSettingsPicker {
            override suspend fun getActiveSettings(room: Room): LightSettings = LightSettings.Unhandled
        }

        val controller = LightController(client, settingsPicker, backgroundScope = backgroundScope)
        val daemonJob = launch { controller.startDaemon() }
        runCurrent()

        eventAccess.mutableEvents.emit(Event.Motion(
            FakeDevices.Motion.id,
            Clock.System.now(),
            MotionState.MOTION
        ))
        runCurrent()

        assertEquals(0, actionPublisher.actions.size, "No action taken when unhandled settings.")

        daemonJob.cancelAndJoin()
    }

    @Test
    fun lightsOff() = runTest {
        val eventAccess = EventAccessFake()
        val actionPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = eventAccess,
            actionPublisher = actionPublisher,
        )

        val controller = LightController(client, settingsPicker, backgroundScope = backgroundScope)
        val daemonJob = launch { controller.startDaemon() }
        runCurrent()

        eventAccess.mutableEvents.emit(Event.Motion(
            FakeDevices.Motion.id,
            Clock.System.now(),
            MotionState.IDLE
        ))
        advanceUntilIdle()
        advanceTimeBy(31.minutes.inWholeMilliseconds)
        runCurrent()

        assertEquals(1, actionPublisher.actions.size, "One light action should be published.")
        val action = actionPublisher.actions.single()
        assertTrue(action is Action.Switch)
        assertEquals(SwitchState.OFF, action.state)

        daemonJob.cancelAndJoin()
    }

    @Test
    fun cancelled() = runTest {
        val eventAccess = EventAccessFake()
        val actionPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = eventAccess,
            actionPublisher = actionPublisher,
        )

        val controller = LightController(client, settingsPicker, backgroundScope = backgroundScope)
        val daemonJob = launch { controller.startDaemon() }
        runCurrent()

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

        assertEquals(1, actionPublisher.actions.size, "One light action should be published.")
        val action = actionPublisher.actions.single()
        assertTrue(action is Action.ColorTemperatureChange)
        assertEquals(SwitchState.ON, action.switchState)

        daemonJob.cancelAndJoin()
    }

    @Test
    fun phasedIdle()
    {
        val eventAccess = EventAccessFake()
        val actionPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            eventAccess = eventAccess,
            actionPublisher = actionPublisher,
        )
        val phasedSettings = object: LightSettingsPicker by settingsPicker {
            override suspend fun getIdleConditions(room: Room): IdleConditions {
                return IdleConditions.Phased(1.seconds, 1.minutes)
            }
        }

        runTest {
            val controller = LightController(client, phasedSettings, backgroundScope = backgroundScope)
            val daemonJob = launch { controller.startDaemon() }
            runCurrent()

            eventAccess.mutableEvents.emit(Event.Motion(
                FakeDevices.Motion.id,
                Clock.System.now(),
                MotionState.IDLE
            ))
            advanceTimeBy(1.seconds)
            runCurrent()

            assertEquals(1, actionPublisher.actions.size, "Light should dim action published at idle")
            val startAction = actionPublisher.actions.single()
            assertTrue(startAction is Action.Dim, "Dim action is published at idle")
            assertEquals(0.01, startAction.level.toDecimal().toDouble())

            advanceTimeBy(1.minutes)
            runCurrent()

            assertEquals(2, actionPublisher.actions.size, "Light should be turned off after phase completes")
            val completeAction = actionPublisher.actions[1]
            assertTrue(completeAction is Action.Switch, "Switch action is published after phase completes")
            assertEquals(SwitchState.OFF, completeAction.state)

            daemonJob.cancelAndJoin()
        }
    }
}
