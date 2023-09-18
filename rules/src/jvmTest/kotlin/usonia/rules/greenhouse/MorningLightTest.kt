package usonia.rules.greenhouse

import inkapplications.spondee.scalar.percent
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import usonia.weather.Conditions
import usonia.weather.Forecast
import usonia.weather.WeatherAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class MorningLightTest {
    private val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
            rooms = setOf(
                FakeRooms.LivingRoom.copy(
                    devices = setOf(
                        FakeDevices.Switch.copy(id = Identifier("plant-switch"), fixture = Fixture.Plant),
                        FakeDevices.Switch.copy(id = Identifier("unrelated-switch")),
                    ),
                ),
            )
        ))
    }

    @Test
    fun idealCase() = runTest {
        val sunriseTime = Instant.fromEpochMilliseconds(currentTime + 20.hours.inWholeMilliseconds)
        val weatherAccess = object: WeatherAccess {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf(Forecast(
                timestamp = Instant.DISTANT_PAST,
                sunrise = sunriseTime,
                sunset = sunriseTime + 12.hours,
                rainChance = 0.percent,
                snowChance = 0.percent,
            ))
            override val conditions: OngoingFlow<Conditions> get() = TODO()
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionPublisher = actionSpy,
        )
        val fakeClock = object: Clock {
            override fun now(): Instant = Instant.fromEpochMilliseconds(currentTime)
        }

        val daemonJob = launch { MorningPlantLight(client, weatherAccess, fakeClock).start() }
        advanceTimeBy(16.hours.inWholeMilliseconds)
        runCurrent()
        assertEquals(0, actionSpy.actions.size, "No action taken before sunrise")
        advanceTimeBy(1.hours.inWholeMilliseconds)
        runCurrent()
        assertEquals(1, actionSpy.actions.size, "On Light Action Sent")
        assertEquals(SwitchState.ON, (actionSpy.actions[0] as? Action.ColorTemperatureChange?)?.switchState)
        advanceTimeBy(4.hours.inWholeMilliseconds)
        runCurrent()
        assertEquals(1, actionSpy.actions.size, "Off command not sent before timeframe")
        advanceTimeBy(1.hours.inWholeMilliseconds)
        runCurrent()
        assertEquals(2, actionSpy.actions.size, "Off Light Action Sent")
        assertEquals(SwitchState.OFF, (actionSpy.actions[1] as? Action.Switch?)?.state)
        advanceUntilIdle()
        runCurrent()
        assertEquals(2, actionSpy.actions.size, "No further actions sent")

        daemonJob.cancelAndJoin()
    }

    @Test
    fun midMorningStart() = runTest {
        val sunriseTime = Instant.fromEpochMilliseconds(currentTime + 1.hours.inWholeMilliseconds)
        val weatherAccess = object: WeatherAccess {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf(Forecast(
                timestamp = Instant.DISTANT_PAST,
                sunrise = sunriseTime,
                sunset = sunriseTime + 12.hours,
                rainChance = 0.percent,
                snowChance = 0.percent,
            ))
            override val conditions: OngoingFlow<Conditions> get() = TODO()
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionPublisher = actionSpy,
        )
        val fakeClock = object: Clock {
            override fun now(): Instant = Instant.fromEpochMilliseconds(currentTime)
        }

        val daemonJob = launch { MorningPlantLight(client, weatherAccess, fakeClock).start() }
        runCurrent()
        assertEquals(1, actionSpy.actions.size, "On Light Action Sent Immediately")
        assertEquals(SwitchState.ON, (actionSpy.actions[0] as? Action.ColorTemperatureChange?)?.switchState)
        advanceTimeBy(4.hours.inWholeMilliseconds)
        assertEquals(2, actionSpy.actions.size, "Off command sent")
        assertEquals(SwitchState.OFF, (actionSpy.actions[1] as? Action.Switch?)?.state)
        advanceUntilIdle()
        runCurrent()
        assertEquals(2, actionSpy.actions.size, "No additional actions sent")

        daemonJob.cancelAndJoin()
    }

    @Test
    fun afterHoursStart() = runTest {
        val sunriseTime = Instant.fromEpochMilliseconds(currentTime - 2.hours.inWholeMilliseconds)
        val weatherAccess = object: WeatherAccess {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf(Forecast(
                timestamp = Instant.DISTANT_PAST,
                sunrise = sunriseTime,
                sunset = sunriseTime + 12.hours,
                rainChance = 0.percent,
                snowChance = 0.percent,
            ))
            override val conditions: OngoingFlow<Conditions> get() = TODO()
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionPublisher = actionSpy,
        )
        val fakeClock = object: Clock {
            override fun now(): Instant = Instant.fromEpochMilliseconds(currentTime)
        }

        val daemonJob = launch { MorningPlantLight(client, weatherAccess, fakeClock).start() }
        advanceUntilIdle()
        runCurrent()
        assertEquals(0, actionSpy.actions.size, "No additional actions sent")

        daemonJob.cancelAndJoin()
    }
}
