package usonia.rules.lights

import inkapplications.spondee.scalar.percent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import usonia.foundation.FakeRooms
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.weather.Conditions
import usonia.weather.Forecast
import usonia.weather.WeatherAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.hours
import kotlin.time.minutes

@OptIn(ExperimentalTime::class)
class DayModeTest {
    private val now = Instant.fromEpochMilliseconds(1234567)
    private val fakeClock = object: Clock {
        override fun now(): Instant = now
    }
    private val idealForecast = Forecast(
        timestamp = now,
        sunrise = now - 2.hours,
        sunset = now + 2.hours,
        rainChance = 0.percent,
        snowChance = 0.percent,
    )
    private val idealConditions = Conditions(
        timestamp = now,
        cloudCover = 0.percent,
        temperature = 0,
    )

    @Test
    fun dayModeTest() = runTest {
        val fakeWeather = object: WeatherAccess {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf(idealForecast)
            override val conditions: OngoingFlow<Conditions> = ongoingFlowOf(idealConditions)

        }
        val picker = DayMode(fakeWeather, fakeClock)

        assertEquals(LightSettings.Ignore, picker.getActiveSettings(FakeRooms.LivingRoom))
        assertEquals(LightSettings.Unhandled, picker.getActiveSettings(FakeRooms.FakeHallway))
    }

    @Test
    fun rainy() = runTest {
        val fakeWeather = object: WeatherAccess {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf(idealForecast.copy(
                rainChance = 20.percent,
            ))
            override val conditions: OngoingFlow<Conditions> = ongoingFlowOf(idealConditions)

        }
        val picker = DayMode(fakeWeather, fakeClock)

        assertEquals(LightSettings.Unhandled, picker.getActiveSettings(FakeRooms.LivingRoom))
    }

    @Test
    fun snowy() = runTest {
        val fakeWeather = object: WeatherAccess {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf(idealForecast.copy(
                snowChance = 20.percent,
            ))
            override val conditions: OngoingFlow<Conditions> = ongoingFlowOf(idealConditions)

        }
        val picker = DayMode(fakeWeather, fakeClock)

        assertEquals(LightSettings.Unhandled, picker.getActiveSettings(FakeRooms.LivingRoom))
    }

    @Test
    fun cloudy() = runTest {
        val fakeWeather = object: WeatherAccess {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf(idealForecast)
            override val conditions: OngoingFlow<Conditions> = ongoingFlowOf(idealConditions.copy(
                cloudCover = 69.percent
            ))

        }
        val picker = DayMode(fakeWeather, fakeClock)

        assertEquals(LightSettings.Unhandled, picker.getActiveSettings(FakeRooms.LivingRoom))
    }

    @Test
    fun evening() = runTest {
        val fakeWeather = object: WeatherAccess {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf(idealForecast.copy(
                sunset = now + 40.minutes
            ))
            override val conditions: OngoingFlow<Conditions> = ongoingFlowOf(idealConditions)

        }
        val picker = DayMode(fakeWeather, fakeClock)

        assertEquals(LightSettings.Unhandled, picker.getActiveSettings(FakeRooms.LivingRoom))
    }

    @Test
    fun morning() = runTest {
        val fakeWeather = object: WeatherAccess {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf(idealForecast.copy(
                sunrise = now - 40.minutes
            ))
            override val conditions: OngoingFlow<Conditions> = ongoingFlowOf(idealConditions)

        }
        val picker = DayMode(fakeWeather, fakeClock)

        assertEquals(LightSettings.Unhandled, picker.getActiveSettings(FakeRooms.LivingRoom))
    }
}
