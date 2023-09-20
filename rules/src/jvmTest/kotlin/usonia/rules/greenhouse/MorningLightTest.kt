package usonia.rules.greenhouse

import inkapplications.spondee.scalar.percent
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.datetime.withZone
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import usonia.weather.Conditions
import usonia.weather.Forecast
import usonia.weather.WeatherAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

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
    fun before() = runTest {
        val sunriseTime = Instant.fromEpochMilliseconds(currentTime + 20.hours.inWholeMilliseconds)
        val now = Instant.fromEpochMilliseconds(currentTime)
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

        MorningPlantLight(client, weatherAccess).runCron(now.withZone(TimeZone.UTC))

        runCurrent()
        assertEquals(0, actionSpy.actions.size, "No action before sunrise")
    }

    @Test
    fun afterStart() = runTest {
        val sunriseTime = Instant.fromEpochMilliseconds(currentTime + 20.hours.inWholeMilliseconds)
        val now = sunriseTime - 3.hours
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

        MorningPlantLight(client, weatherAccess).runCron(now.withZone(TimeZone.UTC))

        runCurrent()
        assertEquals(1, actionSpy.actions.size, "On action sent")
        assertEquals(SwitchState.ON, (actionSpy.actions.single() as? Action.ColorTemperatureChange)?.switchState)
    }

    @Test
    fun afterEnd() = runTest {
        val sunriseTime = Instant.fromEpochMilliseconds(currentTime + 20.hours.inWholeMilliseconds)
        val now = sunriseTime + 3.hours
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

        MorningPlantLight(client, weatherAccess).runCron(now.withZone(TimeZone.UTC))

        runCurrent()
        assertEquals(1, actionSpy.actions.size, "Off action sent")
        assertEquals(SwitchState.OFF, (actionSpy.actions.single() as? Action.Switch)?.state)
    }
}
