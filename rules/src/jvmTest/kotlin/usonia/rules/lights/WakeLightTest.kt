package usonia.rules.lights

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import com.inkapplications.datetime.FixedClock
import com.inkapplications.datetime.atZone
import inkapplications.spondee.scalar.toWholePercentage
import inkapplications.spondee.structure.toDouble
import inkapplications.spondee.structure.toInt
import kimchi.logger.EmptyLogger
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.datetime.*
import usonia.celestials.*
import usonia.core.state.*
import usonia.foundation.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class WakeLightTest {
    private val configuration = object : ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(
            FakeSite.copy(
                rooms = setOf(
                    FakeRooms.FakeBedroom.copy(
                        devices = setOf(
                            FakeDevices.HueColorLight.copy(
                                id = Identifier("test-wake-light"),
                                fixture = Fixture.WakeLight,
                            )
                        )
                    )
                )
            )
        )
    }
    private val actionAccess = ActionAccessFake()
    private val sunrise = LocalDateTime(2025, 1, 2, 7, 0).atZone(TimeZone.UTC)
    private val celestials = FakeCelestials.copy(
        civilTwilight = (sunrise - 1.hours)..(sunrise + 13.hours),
        daylight = sunrise..(sunrise + 12.hours)
    )
    private val celestialAccess = object : CelestialAccess {
        override val localCelestials = ongoingFlowOf(
            FakeUpcomingCelestials.copy(
                today = celestials
            )
        )
    }
    private val fakeClock = FixedClock(Instant.DISTANT_PAST).atZone(TimeZone.UTC)

    @Test
    fun beforeWake() {
        val actionPublisher = ActionPublisherSpy()

        runTest {
            val wakeLight = WakeLight(
                configurationAccess = configuration,
                actionAccess = actionAccess,
                actionPublisher = actionPublisher,
                celestialAccess = celestialAccess,
                clock = fakeClock,
                logger = EmptyLogger,
                backgroundScope = backgroundScope,
            )

            wakeLight.runCron((celestials.civilTwilight.start.instant + 25.minutes).toLocalDateTime(TimeZone.UTC), TimeZone.UTC)
        }

        assertEquals(0, actionPublisher.actions.size, "No events should be published before wake time")
    }

    @Test
    fun lightOn() {
        val actionPublisher = ActionPublisherSpy()

        runTest {
            val wakeLight = WakeLight(
                configurationAccess = configuration,
                actionAccess = actionAccess,
                actionPublisher = actionPublisher,
                celestialAccess = celestialAccess,
                clock = fakeClock,
                logger = EmptyLogger,
                backgroundScope = backgroundScope,
            )

            wakeLight.runCron((celestials.civilTwilight.start.instant + 30.minutes).toLocalDateTime(TimeZone.UTC), TimeZone.UTC)
        }

        assertEquals(1, actionPublisher.actions.size, "Light event should be published")
        val action = actionPublisher.actions.single()
        assertTrue(action is Action.ColorTemperatureChange, "Action should be a color change")
        assertEquals("test-wake-light", action.target.value)
        assertEquals(SwitchState.ON, action.switchState)
        assertEquals(Colors.Warm.toInt(), action.temperature.toKelvin().toInt())
        assertEquals(1, action.level?.toWholePercentage()?.toInt())
    }

    @Test
    fun earlyClamp() {
        val actionPublisher = ActionPublisherSpy()
        val sunrise = LocalDateTime(2025, 1, 2, 4, 0).atZone(TimeZone.UTC)
        val celestials = FakeCelestials.copy(
            civilTwilight = (sunrise - 1.hours)..(sunrise + 13.hours),
            daylight = sunrise..(sunrise + 12.hours)
        )
        val celestialAccess = object : CelestialAccess {
            override val localCelestials = ongoingFlowOf(
                FakeUpcomingCelestials.copy(
                    today = celestials
                )
            )
        }

        runTest {
            val wakeLight = WakeLight(
                configurationAccess = configuration,
                actionAccess = actionAccess,
                actionPublisher = actionPublisher,
                celestialAccess = celestialAccess,
                clock = fakeClock,
                logger = EmptyLogger,
                backgroundScope = backgroundScope,
            )

            wakeLight.runCron((celestials.civilTwilight.start.instant + 30.minutes).toLocalDateTime(TimeZone.UTC), TimeZone.UTC)

            assertEquals(0, actionPublisher.actions.size, "Light should not turn on before clamp time")

            wakeLight.runCron(LocalDateTime(sunrise.localDate, LocalTime(5, 30)), TimeZone.UTC)

            assertEquals(1, actionPublisher.actions.size, "Light turns on after clamp time")
            val action = actionPublisher.actions.single()
            assertTrue(action is Action.ColorTemperatureChange, "Action should be a color change")
            assertEquals("test-wake-light", action.target.value)
            assertEquals(SwitchState.ON, action.switchState)
            assertEquals(Colors.Warm.toInt(), action.temperature.toKelvin().toInt())
            assertEquals(1, action.level?.toWholePercentage()?.toInt())
        }
    }

    @Test
    fun lateClamp() {
        val actionPublisher = ActionPublisherSpy()
        val sunrise = LocalDateTime(2025, 1, 2, 9, 0).atZone(TimeZone.UTC)
        val celestials = FakeCelestials.copy(
            civilTwilight = (sunrise - 1.hours)..(sunrise + 13.hours),
            daylight = sunrise..(sunrise + 12.hours)
        )
        val celestialAccess = object : CelestialAccess {
            override val localCelestials = ongoingFlowOf(
                FakeUpcomingCelestials.copy(
                    today = celestials
                )
            )
        }

        runTest {
            val wakeLight = WakeLight(
                configurationAccess = configuration,
                actionAccess = actionAccess,
                actionPublisher = actionPublisher,
                celestialAccess = celestialAccess,
                clock = fakeClock,
                logger = EmptyLogger,
                backgroundScope = backgroundScope,
            )

            wakeLight.runCron(LocalDateTime(sunrise.localDate, LocalTime(7, 15)), TimeZone.UTC)

            assertEquals(1, actionPublisher.actions.size, "Light turns on after late clamp time")
            val action = actionPublisher.actions.single()
            assertTrue(action is Action.ColorTemperatureChange, "Action should be a color change")
            assertEquals("test-wake-light", action.target.value)
            assertEquals(SwitchState.ON, action.switchState)
            assertEquals(Colors.Warm.toInt(), action.temperature.toKelvin().toInt())
            assertEquals(1, action.level?.toWholePercentage()?.toInt())
        }
    }

    @Test
    fun lightTransitions() {
        val actionPublisher = ActionPublisherSpy()

        runTest {
            val wakeLight = WakeLight(
                configurationAccess = configuration,
                actionAccess = actionAccess,
                actionPublisher = actionPublisher,
                celestialAccess = celestialAccess,
                clock = fakeClock,
                logger = EmptyLogger,
                backgroundScope = backgroundScope,
            )

            wakeLight.runCron((celestials.civilTwilight.start.instant + 50.minutes).toLocalDateTime(TimeZone.UTC), TimeZone.UTC)
        }

        assertEquals(1, actionPublisher.actions.size, "Light event should be published")
        val action = actionPublisher.actions.single()
        assertTrue(action is Action.ColorTemperatureChange, "Action should be a color change")
        assertEquals("test-wake-light", action.target.value)
        assertEquals(SwitchState.ON, action.switchState)
        assertTrue(
            action.temperature.toKelvin().toDouble() > Colors.Warm.toDouble(),
            "Temperature should be transitioning towards daylight"
        )
        assertTrue(
            action.temperature.toKelvin().toDouble() < Colors.Daylight.toDouble(),
            "Temperature should not reach daylight color"
        )
        assertTrue(action.level!!.toDecimal().toDouble() > 0.01, "Brightness should be increasing")
        assertTrue(action.level!!.toDecimal().toDouble() < 1, "Brightness should not reach max")
    }

    @Test
    fun lightMaxTransition() {
        val actionPublisher = ActionPublisherSpy()

        runTest {
            val wakeLight = WakeLight(
                configurationAccess = configuration,
                actionAccess = actionAccess,
                actionPublisher = actionPublisher,
                celestialAccess = celestialAccess,
                clock = fakeClock,
                logger = EmptyLogger,
                backgroundScope = backgroundScope,
            )

            wakeLight.runCron((sunrise.instant + 50.minutes).toLocalDateTime(TimeZone.UTC), TimeZone.UTC)
        }

        assertEquals(1, actionPublisher.actions.size, "Light event should be published")
        val action = actionPublisher.actions.single()
        assertTrue(action is Action.ColorTemperatureChange, "Action should be a color change")
        assertEquals("test-wake-light", action.target.value)
        assertEquals(SwitchState.ON, action.switchState)
        assertEquals(Colors.Daylight.toInt(), action.temperature.toKelvin().toInt())
        assertEquals(100, action.level?.toWholePercentage()?.toInt())
    }

    @Test
    fun autoDismiss() {
        val actionPublisher = ActionPublisherSpy()

        runTest {
            val wakeLight = WakeLight(
                configurationAccess = configuration,
                actionAccess = actionAccess,
                actionPublisher = actionPublisher,
                celestialAccess = celestialAccess,
                clock = fakeClock,
                logger = EmptyLogger,
                backgroundScope = backgroundScope,
            )

            wakeLight.runCron((sunrise.instant + 3.hours).toLocalDateTime(TimeZone.UTC), TimeZone.UTC)
            assertEquals(2, actionPublisher.actions.size, "Light event should be published")
            wakeLight.runCron((sunrise.instant + 4.hours).toLocalDateTime(TimeZone.UTC), TimeZone.UTC)
            assertEquals(2, actionPublisher.actions.size, "No new events should be published after dismissing")
        }

        val firstAction = actionPublisher.actions[0]
        assertTrue(firstAction is Action.ColorTemperatureChange, "Light changes color when dismissing")
        assertEquals("test-wake-light", firstAction.target.value)
        assertEquals(Colors.Warm, firstAction.temperature)
        assertEquals(0.01, firstAction.level?.toDecimal()?.toDouble())

        val secondAction = actionPublisher.actions[1]
        assertTrue(secondAction is Action.Switch, "Action should be a switch")
        assertEquals("test-wake-light", secondAction.target.value)
        assertEquals(SwitchState.OFF, secondAction.state)
    }

    @Test
    fun intentDismiss()
    {
        val actionPublisher = ActionPublisherSpy()

        runTest {
            val clock = FixedClock(sunrise.instant + 2.hours).atZone(TimeZone.UTC)
            val wakeLight = WakeLight(
                configurationAccess = configuration,
                actionAccess = actionAccess,
                actionPublisher = actionPublisher,
                celestialAccess = celestialAccess,
                clock = clock,
                logger = EmptyLogger,
                backgroundScope = backgroundScope,
            )

            backgroundScope.launch { wakeLight.startDaemon() }
            runCurrent()
            actionAccess.mutableActions.emit(
                Action.Intent(
                    target = Identifier("test-wake-light"),
                    action = "usonia.rules.lights.WakeLight.dismiss"
                )
            )

            runCurrent()
            assertEquals(1, actionPublisher.actions.size, "Dimming action is published before delaying")
            advanceTimeBy(5.seconds)
            runCurrent()
            assertEquals(2, actionPublisher.actions.size, "Light is turned off after delay")

            actionAccess.mutableActions.emit(
                Action.Intent(
                    target = Identifier("test-wake-light"),
                    action = "usonia.rules.lights.WakeLight.dismiss"
                )
            )
            assertEquals(2, actionPublisher.actions.size, "No new actions are published after dismissing")
        }

        val firstAction = actionPublisher.actions[0]
        assertTrue(firstAction is Action.ColorTemperatureChange, "Light changes color when dismissing")
        assertEquals("test-wake-light", firstAction.target.value)
        assertEquals(Colors.Warm, firstAction.temperature)
        assertEquals(0.01, firstAction.level?.toDecimal()?.toDouble())

        val secondAction = actionPublisher.actions[1]
        assertTrue(secondAction is Action.Switch, "Action should be a switch")
        assertEquals("test-wake-light", secondAction.target.value)
        assertEquals(SwitchState.OFF, secondAction.state)
    }
}
