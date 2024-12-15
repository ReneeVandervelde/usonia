package usonia.rules.greenhouse

import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.scalar.percent
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import usonia.weather.Conditions
import usonia.weather.FullForecast
import usonia.weather.LocalWeatherAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class MorningPlantLightTest {
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
    fun before() = runTest {
        val sunriseTime = Instant.fromEpochMilliseconds(currentTime + 4.hours.inWholeMilliseconds)
        val now = Instant.fromEpochMilliseconds(currentTime)
        val weatherAccess = object: LocalWeatherAccess {
            override val forecast: OngoingFlow<FullForecast> = ongoingFlowOf(FullForecast(
                timestamp = Instant.DISTANT_PAST,
                sunrise = sunriseTime,
                sunset = sunriseTime + 12.hours,
                rainChance = 0.percent,
                snowChance = 0.percent,
                highTemperature = 0.fahrenheit,
                lowTemperature = 0.fahrenheit,
            ))
            override val conditions: OngoingFlow<Conditions> get() = TODO()
            override val currentConditions: Conditions get() = TODO()
            override val currentForecast: FullForecast get() = TODO()
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionPublisher = actionSpy,
        )

        MorningPlantLight(client, weatherAccess).runCron(now.toLocalDateTime(TimeZone.UTC), TimeZone.UTC)

        runCurrent()
        assertEquals(0, actionSpy.actions.size, "No action before sunrise")
    }

    @Test
    fun afterStart() = runTest {
        val sunriseTime = Instant.fromEpochMilliseconds(currentTime + 4.hours.inWholeMilliseconds)
        val now = sunriseTime - 20.minutes
        val weatherAccess = object: LocalWeatherAccess {
            override val forecast: OngoingFlow<FullForecast> = ongoingFlowOf(FullForecast(
                timestamp = Instant.DISTANT_PAST,
                sunrise = sunriseTime,
                sunset = sunriseTime + 12.hours,
                rainChance = 0.percent,
                snowChance = 0.percent,
                lowTemperature = 0.fahrenheit,
                highTemperature = 0.fahrenheit,
            ))
            override val conditions: OngoingFlow<Conditions> get() = TODO()
            override val currentConditions: Conditions get() = TODO()
            override val currentForecast: FullForecast get() = TODO()
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionPublisher = actionSpy,
        )

        MorningPlantLight(client, weatherAccess).runCron(now.toLocalDateTime(TimeZone.UTC), TimeZone.UTC)

        runCurrent()
        assertEquals(1, actionSpy.actions.size, "On action sent")
        assertEquals(SwitchState.ON, (actionSpy.actions.single() as? Action.ColorTemperatureChange)?.switchState)
    }

    @Test
    fun afterEnd() = runTest {
        val sunriseTime = Instant.fromEpochMilliseconds(currentTime + 20.hours.inWholeMilliseconds)
        val now = sunriseTime + 3.hours
        val weatherAccess = object: LocalWeatherAccess {
            override val forecast: OngoingFlow<FullForecast> = ongoingFlowOf(FullForecast(
                timestamp = Instant.DISTANT_PAST,
                sunrise = sunriseTime,
                sunset = sunriseTime + 12.hours,
                rainChance = 0.percent,
                snowChance = 0.percent,
                lowTemperature = 0.fahrenheit,
                highTemperature = 0.fahrenheit,
            ))
            override val conditions: OngoingFlow<Conditions> get() = TODO()
            override val currentConditions: Conditions get() = TODO()
            override val currentForecast: FullForecast get() = TODO()
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionPublisher = actionSpy,
        )

        MorningPlantLight(client, weatherAccess).runCron(now.toLocalDateTime(TimeZone.UTC), TimeZone.UTC)

        runCurrent()
        assertEquals(1, actionSpy.actions.size, "Off action sent")
        assertEquals(SwitchState.OFF, (actionSpy.actions.single() as? Action.Switch)?.state)
    }
}
