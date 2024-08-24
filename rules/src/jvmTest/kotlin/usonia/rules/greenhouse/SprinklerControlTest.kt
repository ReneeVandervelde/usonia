package usonia.rules.greenhouse

import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.measure.us.inches
import inkapplications.spondee.scalar.percent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.*
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import usonia.weather.Conditions
import usonia.weather.FixedWeather
import usonia.weather.Forecast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SprinklerControlTest {
    private val baseConditions = Conditions(
        timestamp = Instant.DISTANT_PAST,
        cloudCover = 0.percent,
        temperature = 75,
        rainInLast6Hours = 0.inches,
        isRaining = false,
    )
    private val baseForecast = Forecast(
        timestamp = Instant.DISTANT_PAST,
        sunrise = Instant.DISTANT_PAST,
        sunset = Instant.DISTANT_PAST,
        rainChance = 0.percent,
        snowChance = 0.percent,
        highTemperature = 75.fahrenheit,
        lowTemperature = 75.fahrenheit,
    )
    private val configuration = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site = ongoingFlowOf(FakeSite.copy(
            rooms = setOf(
                FakeRooms.LivingRoom.copy(
                    devices = setOf(FakeDevices.Switch.copy(
                        fixture = Fixture.MomentarySprinkler,
                    ))
                )
            ),
        ))
    }
    private val saturday = LocalDateTime(2024, 7,13, 6, 0, 0)
    private val sunday = LocalDateTime(2024, 7,14, 6, 0, 0)

    @Test
    fun onForSchedule() = runTest {
        val weather = FixedWeather(
            initialConditions = baseConditions,
            initialForecast = baseForecast,
        )
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = configuration,
            actionPublisher = actionSpy,
        )
        val rule = SprinklerControl(client, weather)

        rule.runCron(
            time = LocalDateTime(2024, 7, 13, 6, 0, 0),
            zone = TimeZone.UTC,
        )

        actionSpy.assertRanSprinkler()
    }

    @Test
    fun noTriggerWhileRaining() = runTest {
        val weather = FixedWeather(
            initialConditions = baseConditions.copy(
                isRaining = true
            ),
            initialForecast = baseForecast,
        )
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = configuration,
            actionPublisher = actionSpy,
        )
        val rule = SprinklerControl(client, weather)

        rule.runCron(
            time = LocalDateTime(2024, 7, 13, 6, 0, 0),
            zone = TimeZone.UTC,
        )

        actionSpy.assertNoActions()
    }

    @Test
    fun noTriggerWhenRecentRain() = runTest {
        val weather = FixedWeather(
            initialConditions = baseConditions.copy(
                rainInLast6Hours = 1.inches
            ),
            initialForecast = baseForecast,
        )
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = configuration,
            actionPublisher = actionSpy,
        )
        val rule = SprinklerControl(client, weather)

        rule.runCron(
            time = LocalDateTime(2024, 7, 13, 6, 0, 0),
            zone = TimeZone.UTC,
        )

        actionSpy.assertNoActions()
    }

    @Test
    fun noTriggerWithForecastedRain() = runTest {
        val weather = FixedWeather(
            initialConditions = baseConditions,
            initialForecast = baseForecast.copy(
                rainChance = 50.percent,
            ),
        )
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = configuration,
            actionPublisher = actionSpy,
        )
        val rule = SprinklerControl(client, weather)

        rule.runCron(
            time = LocalDateTime(2024, 7, 13, 6, 0, 0),
            zone = TimeZone.UTC,
        )

        actionSpy.assertNoActions()
    }

    @Test
    fun noTriggerWhenCold() = runTest {
        val weather = FixedWeather(
            initialConditions = baseConditions.copy(
                temperature = 40,
            ),
            initialForecast = baseForecast,
        )
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = configuration,
            actionPublisher = actionSpy,
        )
        val rule = SprinklerControl(client, weather)

        rule.runCron(
            time = LocalDateTime(2024, 7, 13, 6, 0, 0),
            zone = TimeZone.UTC,
        )

        actionSpy.assertNoActions()
    }

    @Test
    fun noTriggerWhenColdForecast() = runTest {
        val weather = FixedWeather(
            initialConditions = baseConditions,
            initialForecast = baseForecast.copy(
                lowTemperature = 40.fahrenheit,
            ),
        )
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = configuration,
            actionPublisher = actionSpy,
        )
        val rule = SprinklerControl(client, weather)

        rule.runCron(
            time = LocalDateTime(2024, 7, 13, 6, 0, 0),
            zone = TimeZone.UTC,
        )

        actionSpy.assertNoActions()
    }

    @Test
    fun noTriggerOffSchedule() = runTest {
        val weather = FixedWeather(
            initialConditions = baseConditions,
            initialForecast = baseForecast,
        )
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = configuration,
            actionPublisher = actionSpy,
        )
        val rule = SprinklerControl(client, weather)

        rule.runCron(
            time = LocalDateTime(2024, 7, 14, 6, 0, 0),
            zone = TimeZone.UTC,
        )

        actionSpy.assertNoActions()
    }

    @Test
    fun onForExcessHeat() = runTest {
        val weather = FixedWeather(
            initialConditions = baseConditions,
            initialForecast = baseForecast.copy(
                highTemperature = 90.fahrenheit,
            ),
        )
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = configuration,
            actionPublisher = actionSpy,
        )
        val rule = SprinklerControl(client, weather)

        rule.runCron(
            time = LocalDateTime(2024, 7, 14, 6, 0, 0),
            zone = TimeZone.UTC,
        )

        actionSpy.assertRanSprinkler()
    }

    private fun ActionPublisherSpy.assertRanSprinkler() {
        assertEquals(1, actions.size)
        val onAction = actions.single()
        assertTrue(onAction is Action.Switch)
        assertEquals(SwitchState.ON, onAction.state)
    }

    private fun ActionPublisherSpy.assertNoActions() {
        assertEquals(0, actions.size)
    }
}
