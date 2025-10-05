package usonia.rules.lights

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import com.inkapplications.datetime.FixedClock
import com.inkapplications.datetime.atZone
import com.inkapplications.datetime.toZonedDateTime
import inkapplications.spondee.scalar.toWholePercentage
import inkapplications.spondee.structure.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import usonia.celestials.CelestialAccess
import usonia.celestials.Celestials
import usonia.celestials.UpcomingCelestials
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.FakeRooms
import usonia.foundation.FakeSite
import usonia.foundation.Site
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
    private val sunrise = startOfDay.plus(7.hours).toZonedDateTime(TimeZone.UTC)
    private val sunset = startOfDay.plus(18.hours).toZonedDateTime(TimeZone.UTC)
    private val nightStart = startOfDay.plus(DEFAULT_NIGHT_START.minutes)

    val celestialAccess = object: CelestialAccess {
        override val localCelestials: OngoingFlow<UpcomingCelestials> = ongoingFlowOf(
            UpcomingCelestials(
                timestamp = Instant.DISTANT_PAST.toZonedDateTime(TimeZone.UTC),
                today = Celestials(
                    daylight = sunrise..sunset,
                    civilTwilight = sunrise.minus(20.minutes)..sunrise.plus(20.minutes),
                ),
                tomorrow = Celestials(
                    daylight = sunrise.plus(1.days)..sunset.plus(1.days),
                    civilTwilight = sunrise.plus(1.days).minus(20.minutes)..sunrise.plus(1.days).plus(20.minutes),
                )
            )
        )
    }

    val config = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite)
    }

    @Test
    fun afterMidnight() = runTest {
        val clock = FixedClock(sunrise.instant.minus(DEFAULT_PERIOD).minus(2.minutes))
        val colors = CircadianColors(config, celestialAccess, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_NIGHTLIGHT.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(DEFAULT_NIGHT_BRIGHTNESS.toInt(), result.brightness.toWholePercentage().toInt())
    }

    @Test
    fun staleForecast() = runTest {
        val clock = FixedClock(startOfDay.plus(1.days))
        val colors = CircadianColors(config, celestialAccess, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_NIGHTLIGHT.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(DEFAULT_NIGHT_BRIGHTNESS.toInt(), result.brightness.toWholePercentage().toInt())
    }


    @Test
    fun morningBlueHour() = runTest {
        val clock = FixedClock(sunrise.instant.minus(DEFAULT_PERIOD / 4))
        val colors = CircadianColors(config, celestialAccess, clock.atZone(TimeZone.UTC))

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
        val clock = FixedClock(sunrise.instant)
        val colors = CircadianColors(config, celestialAccess, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_DAYLIGHT.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(100, result.brightness.toWholePercentage().toInt())
    }

    @Test
    fun day() = runTest {
        val clock = FixedClock(sunrise.instant + 2.minutes)
        val colors = CircadianColors(config, celestialAccess, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_DAYLIGHT.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(100, result.brightness.toWholePercentage().toInt())
    }

    @Test
    fun sunset() = runTest {
        val clock = FixedClock(sunset.instant)
        val colors = CircadianColors(config, celestialAccess, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_DAYLIGHT.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(100, result.brightness.toWholePercentage().toInt())
    }

    @Test
    fun evening() = runTest {
        val clock = FixedClock(sunset.instant + DEFAULT_PERIOD)
        val colors = CircadianColors(config, celestialAccess, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_EVENING.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(100, result.brightness.toWholePercentage().toInt())
    }

    @Test
    fun twilight() = runTest {
        val clock = FixedClock(nightStart.plus(DEFAULT_PERIOD / 4))
        val colors = CircadianColors(config, celestialAccess, clock.atZone(TimeZone.UTC))

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
        val clock = FixedClock(nightStart.plus(DEFAULT_PERIOD / 4))
        val colors = CircadianColors(config, celestialAccess, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.Office)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_EVENING.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(100, result.brightness.toWholePercentage().toInt())
    }

    @Test
    fun night() = runTest {
        val clock = FixedClock(nightStart + DEFAULT_PERIOD +2.minutes)
        val colors = CircadianColors(config, celestialAccess, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_NIGHTLIGHT.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(DEFAULT_NIGHT_BRIGHTNESS.toWholePercentage().toInt(), result.brightness.toWholePercentage().toInt())
    }

    @Test
    fun nightExempt() = runTest {
        val clock = FixedClock(nightStart + DEFAULT_PERIOD + 2.minutes)
        val colors = CircadianColors(config, celestialAccess, clock.atZone(TimeZone.UTC))

        val result = colors.getActiveSettings(FakeRooms.Office)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(DEFAULT_EVENING.toInt(), result.temperature.toKelvin().toInt())
        assertEquals(100, result.brightness.toWholePercentage().toInt())
    }

    @Test
    fun overlapStart() = runTest {
        val celestialAccess = object: CelestialAccess {
            override val localCelestials: OngoingFlow<UpcomingCelestials> = ongoingFlowOf(
                UpcomingCelestials(
                    timestamp = Instant.DISTANT_PAST.toZonedDateTime(TimeZone.UTC),
                    today = Celestials(
                        daylight = sunrise..nightStart.toZonedDateTime(TimeZone.UTC),
                        civilTwilight = sunrise.minus(20.minutes)..sunrise.plus(20.minutes),
                    ),
                    tomorrow = Celestials(
                        daylight = sunrise.plus(1.days)..nightStart.toZonedDateTime(TimeZone.UTC).plus(1.days),
                        civilTwilight = sunrise.plus(1.days).minus(20.minutes)..sunrise.plus(1.days).plus(20.minutes),
                    )
                )
            )
        }
        val clock = FixedClock(nightStart)
        val colors = CircadianColors(config, celestialAccess, clock.atZone(TimeZone.UTC))

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
