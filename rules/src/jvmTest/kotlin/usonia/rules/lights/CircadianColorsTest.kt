package usonia.rules.lights

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import usonia.core.state.ConfigurationAccess
import usonia.foundation.FakeRooms
import usonia.foundation.FakeSite
import usonia.foundation.Site
import usonia.kotlin.unit.percent
import usonia.weather.Conditions
import usonia.weather.Forecast
import usonia.weather.WeatherAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.hours
import kotlin.time.minutes

@OptIn(ExperimentalTime::class)
class CircadianColorsTest {
    private val sunrise = Instant.fromEpochSeconds(500_000)
    private val sunset = sunrise.plus(8.hours)

    val weather = object: WeatherAccess {
        override val forecast: Flow<Forecast> = flowOf(Forecast(
            timestamp = Instant.DISTANT_PAST,
            expiry = Instant.DISTANT_FUTURE,
            sunrise = sunrise,
            sunset = sunset,
        ))
        override val conditions: Flow<Conditions> get() = TODO()
    }

    val config = object: ConfigurationAccess {
        override val site: Flow<Site> = flowOf(FakeSite)
    }

    @Test
    fun afterMidnight() = runBlockingTest {
        val clock = object: Clock {
            override fun now(): Instant = sunrise.minus(DEFAULT_PERIOD).minus(2.minutes)
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getRoomColor(FakeRooms.LivingRoom)

        assertEquals(DEFAULT_NIGHTLIGHT, result.temperature)
        assertEquals(DEFAULT_NIGHT_BRIGHTNESS, result.brightness)
    }

    @Test
    fun morningBlueHour() = runBlockingTest {
        val clock = object: Clock {
            override fun now(): Instant = sunrise.minus(DEFAULT_PERIOD / 4)
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getRoomColor(FakeRooms.LivingRoom)

        assertEquals(
            DEFAULT_DAYLIGHT.kelvinValue - ((DEFAULT_DAYLIGHT.kelvinValue - DEFAULT_NIGHTLIGHT.kelvinValue) / 4),
            result.temperature.kelvinValue
        )
        assertTrue(
            (100f - ((100 - DEFAULT_NIGHT_BRIGHTNESS.percent) / 4)).toInt() - result.brightness.percent <= 1
        )
    }

    @Test
    fun dawn() = runBlockingTest {
        val clock = object: Clock {
            override fun now(): Instant = sunrise
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getRoomColor(FakeRooms.LivingRoom)

        assertEquals(DEFAULT_DAYLIGHT, result.temperature)
        assertEquals(100.percent, result.brightness)
    }

    @Test
    fun day() = runBlockingTest {
        val clock = object: Clock {
            override fun now(): Instant = sunrise.plus(2.minutes)
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getRoomColor(FakeRooms.LivingRoom)

        assertEquals(DEFAULT_DAYLIGHT, result.temperature)
        assertEquals(100.percent, result.brightness)
    }

    @Test
    fun sunset() = runBlockingTest {
        val clock = object: Clock {
            override fun now(): Instant = sunset
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getRoomColor(FakeRooms.LivingRoom)

        assertEquals(DEFAULT_DAYLIGHT, result.temperature)
        assertEquals(100.percent, result.brightness)
    }

    @Test
    fun eveningBlueHour() = runBlockingTest {
        val clock = object: Clock {
            override fun now(): Instant = sunset.plus(DEFAULT_PERIOD / 4)
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getRoomColor(FakeRooms.LivingRoom)

        assertEquals(
            DEFAULT_DAYLIGHT.kelvinValue + ((DEFAULT_NIGHTLIGHT.kelvinValue - DEFAULT_DAYLIGHT.kelvinValue) / 4),
            result.temperature.kelvinValue
        )
        assertTrue(
            (100f - ((100 - DEFAULT_NIGHT_BRIGHTNESS.percent) / 4)).toInt() - result.brightness.percent <= 1
        )
    }

    @Test
    fun night() = runBlockingTest {
        val clock = object: Clock {
            override fun now(): Instant = sunset.plus(DEFAULT_PERIOD).plus(2.minutes)
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getRoomColor(FakeRooms.LivingRoom)

        assertEquals(DEFAULT_NIGHTLIGHT, result.temperature)
        assertEquals(DEFAULT_NIGHT_BRIGHTNESS, result.brightness)
    }
}
