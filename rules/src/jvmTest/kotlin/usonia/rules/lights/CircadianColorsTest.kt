package usonia.rules.lights

import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.scalar.percent
import inkapplications.spondee.scalar.toWholePercentage
import inkapplications.spondee.structure.div
import inkapplications.spondee.structure.minus
import inkapplications.spondee.structure.plus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.FakeRooms
import usonia.foundation.FakeSite
import usonia.foundation.Site
import usonia.kotlin.OngoingFlow
import usonia.kotlin.datetime.ZonedClock
import usonia.kotlin.datetime.ZonedSystemClock
import usonia.kotlin.ongoingFlowOf
import usonia.weather.Conditions
import usonia.weather.Forecast
import usonia.weather.WeatherAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class CircadianColorsTest {
    private val startOfDay = LocalDateTime(0, 1, 1, 0, 0, 0)
        .toInstant(TimeZone.currentSystemDefault())
    private val sunrise = startOfDay.plus(7.hours)
    private val sunset = startOfDay.plus(18.hours)
    private val nightStart = startOfDay.plus(DEFAULT_NIGHT_START.minutes)

    val weather = object: WeatherAccess {
        override val forecast: OngoingFlow<Forecast> = ongoingFlowOf(Forecast(
            timestamp = Instant.DISTANT_PAST,
            sunrise = sunrise,
            sunset = sunset,
            rainChance = 0.percent,
            snowChance = 0.percent,
            highTemperature = 0.fahrenheit,
            lowTemperature = 0.fahrenheit,
        ))
        override val conditions: OngoingFlow<Conditions> get() = TODO()
        override val currentConditions: Conditions get() = TODO()
        override val currentForecast: Forecast get() = TODO()
    }

    val config = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite)
    }

    @Test
    fun afterMidnight() = runTest {
        val clock = object: ZonedClock by ZonedSystemClock {
            override fun now(): Instant = sunrise.minus(DEFAULT_PERIOD).minus(2.minutes)
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_NIGHTLIGHT, result.temperature)
        assertEquals(DEFAULT_NIGHT_BRIGHTNESS, result.brightness)
    }

    @Test
    fun staleForecast() = runTest {
        val clock = object: ZonedClock by ZonedSystemClock {
            override fun now(): Instant = startOfDay.plus(1.days)
        }
        val weather = object: WeatherAccess {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf(Forecast(
                timestamp = Instant.DISTANT_PAST,
                sunrise = sunrise,
                sunset = sunset,
                rainChance = 0.percent,
                snowChance = 0.percent,
                highTemperature = 0.fahrenheit,
                lowTemperature = 0.fahrenheit,
            ))
            override val conditions: OngoingFlow<Conditions> get() = TODO()
            override val currentConditions: Conditions get() = TODO()
            override val currentForecast: Forecast get() = TODO()
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_NIGHTLIGHT, result.temperature)
        assertEquals(DEFAULT_NIGHT_BRIGHTNESS, result.brightness)
    }


    @Test
    fun morningBlueHour() = runTest {
        val clock = object: ZonedClock by ZonedSystemClock {
            override fun now(): Instant = sunrise.minus(DEFAULT_PERIOD / 4)
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(
            (DEFAULT_DAYLIGHT.toKelvin() - ((DEFAULT_DAYLIGHT.toKelvin() - DEFAULT_NIGHTLIGHT.toKelvin()) / 4)).value.toInt(),
            result.temperature.toKelvin().value.toInt(),
        )
        assertTrue(
            (100f - ((100 - DEFAULT_NIGHT_BRIGHTNESS.toWholePercentage().value.toInt()) / 4)).toInt() - result.brightness.toWholePercentage().value.toInt() <= 1
        )
    }

    @Test
    fun dawn() = runTest {
        val clock = object: ZonedClock by ZonedSystemClock {
            override fun now(): Instant = sunrise
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_DAYLIGHT, result.temperature)
        assertEquals(100.percent, result.brightness)
    }

    @Test
    fun day() = runTest {
        val clock = object: ZonedClock by ZonedSystemClock {
            override fun now(): Instant = sunrise.plus(2.minutes)
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_DAYLIGHT, result.temperature)
        assertEquals(100.percent, result.brightness)
    }

    @Test
    fun sunset() = runTest {
        val clock = object: ZonedClock by ZonedSystemClock {
            override fun now(): Instant = sunset
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_DAYLIGHT, result.temperature)
        assertEquals(100.percent, result.brightness)
    }

    @Test
    fun evening() = runTest {
        val clock = object: ZonedClock by ZonedSystemClock {
            override fun now(): Instant = sunset.plus(DEFAULT_PERIOD)
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_EVENING, result.temperature)
        assertEquals(100.percent, result.brightness)
    }

    @Test
    fun twilight() = runTest {
        val clock = object: ZonedClock by ZonedSystemClock {
            override fun now(): Instant = nightStart.plus(DEFAULT_PERIOD / 4)
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(
            (DEFAULT_EVENING.toKelvin() + ((DEFAULT_NIGHTLIGHT.toKelvin() - DEFAULT_EVENING.toKelvin()) / 4)).value.toInt(),
            result.temperature.toKelvin().value.toInt(),
        )
        assertTrue(
            (100f - ((100 - DEFAULT_NIGHT_BRIGHTNESS.toWholePercentage().value.toInt()) / 4)).toInt() - result.brightness.toWholePercentage().value.toInt() <= 1
        )
    }

    @Test
    fun twilightExempt() = runTest {
        val clock = object: ZonedClock by ZonedSystemClock {
            override fun now(): Instant = nightStart.plus(DEFAULT_PERIOD / 4)
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getActiveSettings(FakeRooms.Office)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_EVENING, result.temperature)
        assertEquals(100.percent, result.brightness)
    }

    @Test
    fun night() = runTest {
        val clock = object: ZonedClock by ZonedSystemClock {
            override fun now(): Instant = nightStart.plus(DEFAULT_PERIOD).plus(2.minutes)
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_NIGHTLIGHT, result.temperature)
        assertEquals(DEFAULT_NIGHT_BRIGHTNESS, result.brightness)
    }

    @Test
    fun nightExempt() = runTest {
        val clock = object: ZonedClock by ZonedSystemClock {
            override fun now(): Instant = nightStart.plus(DEFAULT_PERIOD).plus(2.minutes)
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getActiveSettings(FakeRooms.Office)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_EVENING, result.temperature)
        assertEquals(100.percent, result.brightness)
    }

    @Test
    fun overlapStart() = runTest {
        val weather = object: WeatherAccess {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf(Forecast(
                timestamp = Instant.DISTANT_PAST,
                sunrise = sunrise,
                sunset = nightStart,
                rainChance = 0.percent,
                snowChance = 0.percent,
                highTemperature = 0.fahrenheit,
                lowTemperature = 0.fahrenheit,
            ))
            override val conditions: OngoingFlow<Conditions> get() = TODO()
            override val currentConditions: Conditions get() = TODO()
            override val currentForecast: Forecast get() = TODO()
        }
        val clock = object: ZonedClock by ZonedSystemClock {
            override fun now(): Instant = nightStart
        }
        val colors = CircadianColors(config, weather, clock)

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(
            DEFAULT_DAYLIGHT,
            result.temperature
        )
        assertTrue(
            (100f - ((100 - DEFAULT_NIGHT_BRIGHTNESS.toWholePercentage().value.toInt()) / 4)).toInt() - result.brightness.toWholePercentage().value.toInt() <= 1
        )
    }
}
