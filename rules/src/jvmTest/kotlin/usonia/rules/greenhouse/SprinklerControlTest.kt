package usonia.rules.greenhouse

import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.measure.us.inches
import inkapplications.spondee.scalar.percent
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.*
import usonia.server.DummyClient
import usonia.weather.Conditions
import usonia.weather.FixedWeather
import usonia.weather.Forecast
import usonia.weather.FullForecast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class SprinklerControlTest {
    private val baseConditions = Conditions(
        timestamp = Instant.DISTANT_PAST,
        cloudCover = 0.percent,
        temperature = 75.fahrenheit,
        rainInLast6Hours = 0.inches,
        isRaining = false,
    )
    private val baseForecast = Forecast(
        timestamp = Instant.DISTANT_PAST,
        rainChance = 0.percent,
        snowChance = 0.percent,
        highTemperature = 75.fahrenheit,
        lowTemperature = 75.fahrenheit,
        precipitation = 0.percent,
    )
    private val configuration = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site = ongoingFlowOf(FakeSite.copy(
            rooms = setOf(
                FakeRooms.LivingRoom.copy(
                    devices = setOf(
                        FakeDevices.Switch.copy(
                            id = Identifier("sprinkler-1"),
                            fixture = Fixture.MomentarySprinkler,
                        ),
                        FakeDevices.Switch.copy(
                            id = Identifier("sprinkler-2"),
                            fixture = Fixture.MomentarySprinkler,
                        ),
                    )
                )
            ),
        ))
    }

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
        val rule = SprinklerControl(client, weather, backgroundScope = this)

        rule.runCron(
            time = LocalDateTime(2024, 7, 13, 6, 0, 0),
            zone = TimeZone.UTC,
        )
        runCurrent()

        actionSpy.assertRanSprinkler()
    }

    @Test
    fun fullCycle() = runTest {
        val weather = FixedWeather(
            initialConditions = baseConditions,
            initialForecast = baseForecast,
        )
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = configuration,
            actionPublisher = actionSpy,
        )
        val rule = SprinklerControl(client, weather, backgroundScope = this)

        rule.runCron(
            time = LocalDateTime(2024, 7, 13, 6, 0, 0),
            zone = TimeZone.UTC,
        )
        runCurrent()

        assertEquals(1, actionSpy.actions.size, "Action is published to turn on sprinkler")
        val firstOnAction = actionSpy.actions.single()
        assertTrue(firstOnAction is Action.Switch, "Action should be a Switch")
        assertEquals(SwitchState.ON, firstOnAction.state, "Action should turn on the switch")

        advanceTimeBy(15.minutes)
        runCurrent()

        assertEquals(3, actionSpy.actions.size, "First sprinkler should be turned off, second should be on")
        val offAction = actionSpy.actions[1]
        assertTrue(offAction is Action.Switch, "Second Action should be a Switch")
        assertEquals(SwitchState.OFF, offAction.state, "Action should turn off the switch")

        val secondOnAction = actionSpy.actions[2]
        assertTrue(secondOnAction is Action.Switch, "Second Action should be a Switch")
        assertEquals(SwitchState.ON, secondOnAction.state, "Action should turn on the switch")

        advanceTimeBy(15.minutes)
        runCurrent()

        assertEquals(4, actionSpy.actions.size, "Second sprinkler should be turned off")
        val thirdOffAction = actionSpy.actions[3]
        assertTrue(thirdOffAction is Action.Switch, "Third Action should be a Switch")
        assertEquals(SwitchState.OFF, thirdOffAction.state, "Action should turn off the switch")
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
        val rule = SprinklerControl(client, weather, backgroundScope = this)

        rule.runCron(
            time = LocalDateTime(2024, 7, 13, 6, 0, 0),
            zone = TimeZone.UTC,
        )
        runCurrent()

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
        val rule = SprinklerControl(client, weather, backgroundScope = this)

        rule.runCron(
            time = LocalDateTime(2024, 7, 13, 6, 0, 0),
            zone = TimeZone.UTC,
        )
        runCurrent()

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
        val rule = SprinklerControl(client, weather, backgroundScope = this)

        rule.runCron(
            time = LocalDateTime(2024, 7, 13, 6, 0, 0),
            zone = TimeZone.UTC,
        )
        runCurrent()

        actionSpy.assertNoActions()
    }

    @Test
    fun noTriggerWhenFreezing() = runTest {
        val weather = FixedWeather(
            initialConditions = baseConditions.copy(
                temperature = 30.fahrenheit,
            ),
            initialForecast = baseForecast,
        )
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = configuration,
            actionPublisher = actionSpy,
        )
        val rule = SprinklerControl(client, weather, backgroundScope = this)

        rule.runCron(
            time = LocalDateTime(2024, 7, 13, 6, 0, 0),
            zone = TimeZone.UTC,
        )
        runCurrent()

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
        val rule = SprinklerControl(client, weather, backgroundScope = this)

        rule.runCron(
            time = LocalDateTime(2024, 7, 14, 6, 0, 0),
            zone = TimeZone.UTC,
        )
        runCurrent()

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
        val rule = SprinklerControl(client, weather, backgroundScope = this)

        rule.runCron(
            time = LocalDateTime(2024, 7, 14, 6, 0, 0),
            zone = TimeZone.UTC,
        )
        runCurrent()

        actionSpy.assertRanSprinkler()
    }

    @Test
    fun onForExcessHeatSeedling() = runTest {
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
        val rule = SprinklerControl(client, weather, backgroundScope = this)

        rule.runCron(
            time = LocalDateTime(2024, 5, 14, 13, 0, 0),
            zone = TimeZone.UTC,
        )
        runCurrent()

        actionSpy.assertRanSprinkler()
    }

    @Test
    fun noExcessiveHeatRunAfternoonAfterSeedling() = runTest {
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
        val rule = SprinklerControl(client, weather, backgroundScope = this)

        rule.runCron(
            time = LocalDateTime(2024, 7, 14, 13, 0, 0),
            zone = TimeZone.UTC,
        )
        runCurrent()

        actionSpy.assertNoActions()
    }

    @Test
    fun noAfternoonTriggerNormal() = runTest {
        val weather = FixedWeather(
            initialConditions = baseConditions,
            initialForecast = baseForecast
        )
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = configuration,
            actionPublisher = actionSpy,
        )
        val rule = SprinklerControl(client, weather, backgroundScope = this)

        rule.runCron(
            time = LocalDateTime(2024, 7, 13, 13, 0, 0),
            zone = TimeZone.UTC,
        )
        runCurrent()

        actionSpy.assertNoActions()
    }

    @Test
    fun afternoonTriggerForSeedling() = runTest {
        val weather = FixedWeather(
            initialConditions = baseConditions,
            initialForecast = baseForecast
        )
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = configuration,
            actionPublisher = actionSpy,
        )
        val rule = SprinklerControl(client, weather, backgroundScope = this)

        rule.runCron(
            time = LocalDateTime(2024, 5, 13, 13, 0, 0),
            zone = TimeZone.UTC,
        )
        runCurrent()

        actionSpy.assertRanSprinkler()
    }

    @Test
    fun assertEveryDaySeedling() = runTest {
        val weather = FixedWeather(
            initialConditions = baseConditions,
            initialForecast = baseForecast
        )
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = configuration,
            actionPublisher = actionSpy,
        )
        val rule = SprinklerControl(client, weather, backgroundScope = this)

        rule.runCron(
            time = LocalDateTime(2024, 5, 14, 13, 0, 0),
            zone = TimeZone.UTC,
        )
        runCurrent()

        actionSpy.assertRanSprinkler()
    }

    private fun ActionPublisherSpy.assertRanSprinkler() {
        assertEquals(1, actions.size, "1 action (sprinkler) should have been published")
        val onAction = actions.single()
        assertTrue(onAction is Action.Switch, "Action should be a Switch")
        assertEquals(SwitchState.ON, onAction.state, "Action should turn on the switch")
    }

    private fun ActionPublisherSpy.assertNoActions() {
        assertEquals(0, actions.size, "No actions should be published")
    }
}
