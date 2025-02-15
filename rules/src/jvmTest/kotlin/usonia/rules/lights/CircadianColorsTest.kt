package usonia.rules.lights

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import com.inkapplications.datetime.atZone
import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.scalar.percent
import inkapplications.spondee.scalar.toWholePercentage
import inkapplications.spondee.structure.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.FakeRooms
import usonia.foundation.FakeSite
import usonia.foundation.Site
import usonia.weather.Conditions
import usonia.weather.FullForecast
import usonia.weather.LocalWeatherAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CircadianColorsTest {
    private val startOfDay = LocalDateTime(0, 1, 1, 0, 0, 0)
        .toInstant(TimeZone.UTC)
    private val sunrise = startOfDay.plus(7.hours)
    private val sunset = startOfDay.plus(18.hours)
    private val nightStart = startOfDay.plus(DEFAULT_NIGHT_START.minutes)

    val weather = object: LocalWeatherAccess {
        override val forecast: OngoingFlow<FullForecast> = ongoingFlowOf(FullForecast(
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
        override val currentForecast: FullForecast get() = TODO()
    }

    val config = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite)
    }

    @Test
    fun afterMidnight() = runTest {
        val clock = object: Clock {
            override fun now(): Instant = sunrise.minus(DEFAULT_PERIOD).minus(2.minutes)
        }
        val colors = CircadianColors(config, weather, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_NIGHTLIGHT.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(DEFAULT_NIGHT_BRIGHTNESS.toInt(), result.brightness.toWholePercentage().toInt())
    }

    @Test
    fun staleForecast() = runTest {
        val clock = object: Clock {
            override fun now(): Instant = startOfDay.plus(1.days)
        }
        val weather = object: LocalWeatherAccess {
            override val forecast: OngoingFlow<FullForecast> = ongoingFlowOf(FullForecast(
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
            override val currentForecast: FullForecast get() = TODO()
        }
        val colors = CircadianColors(config, weather, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_NIGHTLIGHT.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(DEFAULT_NIGHT_BRIGHTNESS.toInt(), result.brightness.toWholePercentage().toInt())
    }


    @Test
    fun morningBlueHour() = runTest {
        val clock = object: Clock {
            override fun now(): Instant = sunrise.minus(DEFAULT_PERIOD / 4)
        }
        val colors = CircadianColors(config, weather, clock.atZone(TimeZone.UTC))

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
        val clock = object: Clock {
            override fun now(): Instant = sunrise
        }
        val colors = CircadianColors(config, weather, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_DAYLIGHT.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(100, result.brightness.toWholePercentage().toInt())
    }

    @Test
    fun day() = runTest {
        val clock = object: Clock {
            override fun now(): Instant = sunrise.plus(2.minutes)
        }
        val colors = CircadianColors(config, weather, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_DAYLIGHT.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(100, result.brightness.toWholePercentage().toInt())
    }

    @Test
    fun sunset() = runTest {
        val clock = object: Clock {
            override fun now(): Instant = sunset
        }
        val colors = CircadianColors(config, weather, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_DAYLIGHT.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(100, result.brightness.toWholePercentage().toInt())
    }

    @Test
    fun evening() = runTest {
        val clock = object: Clock {
            override fun now(): Instant = sunset.plus(DEFAULT_PERIOD)
        }
        val colors = CircadianColors(config, weather, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_EVENING.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(100, result.brightness.toWholePercentage().toInt())
    }

    @Test
    fun twilight() = runTest {
        val clock = object: Clock {
            override fun now(): Instant = nightStart.plus(DEFAULT_PERIOD / 4)
        }
        val colors = CircadianColors(config, weather, clock.atZone(TimeZone.UTC))

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
        val clock = object: Clock {
            override fun now(): Instant = nightStart.plus(DEFAULT_PERIOD / 4)
        }
        val colors = CircadianColors(config, weather, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.Office)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_EVENING.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(100, result.brightness.toWholePercentage().toInt())
    }

    @Test
    fun night() = runTest {
        val clock = object: Clock {
            override fun now(): Instant = nightStart.plus(DEFAULT_PERIOD).plus(2.minutes)
        }
        val colors = CircadianColors(config, weather, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_NIGHTLIGHT.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(DEFAULT_NIGHT_BRIGHTNESS.toWholePercentage().toInt(), result.brightness.toWholePercentage().toInt())
    }

    @Test
    fun nightExempt() = runTest {
        val clock = object: Clock {
            override fun now(): Instant = nightStart.plus(DEFAULT_PERIOD).plus(2.minutes)
        }
        val colors = CircadianColors(config, weather, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.Office)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_EVENING.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(100, result.brightness.toWholePercentage().toInt())
    }

    @Test
    fun overlapStart() = runTest {
        val weather = object: LocalWeatherAccess {
            override val forecast: OngoingFlow<FullForecast> = ongoingFlowOf(FullForecast(
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
            override val currentForecast: FullForecast get() = TODO()
        }
        val clock = object: Clock {
            override fun now(): Instant = nightStart
        }
        val colors = CircadianColors(config, weather, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(
            DEFAULT_DAYLIGHT.toInt(),
            result.temperature.toKelvin().toInt(),
        )
        assertTrue(
            (100f - ((100 - DEFAULT_NIGHT_BRIGHTNESS.toWholePercentage().value.toInt()) / 4)).toInt() - result.brightness.toWholePercentage().value.toInt() <= 1
        )
    }
}
