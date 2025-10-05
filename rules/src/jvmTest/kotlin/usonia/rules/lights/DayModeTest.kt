package usonia.rules.lights

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import com.inkapplications.datetime.FixedClock
import com.inkapplications.datetime.atZone
import com.inkapplications.datetime.toZonedDateTime
import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.measure.us.inches
import inkapplications.spondee.scalar.percent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import usonia.celestials.CelestialAccess
import usonia.celestials.Celestials
import usonia.celestials.UpcomingCelestials
import usonia.foundation.FakeRooms
import usonia.weather.Conditions
import usonia.weather.Forecast
import usonia.weather.FullForecast
import usonia.weather.LocalWeatherAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class DayModeTest {
    private val now = Instant.fromEpochMilliseconds(1234567).toZonedDateTime(TimeZone.UTC)
    private val fakeClock = FixedClock(now.instant).atZone(TimeZone.UTC)
    private val idealForecast = Forecast(
        timestamp = now.instant,
        rainChance = 0.percent,
        snowChance = 0.percent,
        highTemperature = 0.fahrenheit,
        lowTemperature = 0.fahrenheit,
        precipitation = 0.percent,
    )
    private val idealConditions = Conditions(
        timestamp = now.instant,
        cloudCover = 0.percent,
        temperature = 0.fahrenheit,
        rainInLast6Hours = 0.inches,
        isRaining = false,
    )
    private val testCelestials = Celestials(
        daylight = now.minus(2.hours)..now.plus(2.hours),
        civilTwilight = now.minus(3.hours)..now.plus(3.hours),
    )
    private val celestialsAccess = object: CelestialAccess {
        override val localCelestials: OngoingFlow<UpcomingCelestials> = ongoingFlowOf(
            UpcomingCelestials(
                timestamp = fakeClock.zonedDateTime(),
                today = testCelestials,
                tomorrow = testCelestials,
            )
        )
    }

    @Test
    fun dayModeTest() = runTest {
        val fakeWeather = object: LocalWeatherAccess {
            override val forecast: OngoingFlow<Forecast?> = ongoingFlowOf(idealForecast)
            override val conditions: OngoingFlow<Conditions?> = ongoingFlowOf(idealConditions)
        }
        val picker = DayMode(fakeWeather, celestialsAccess, fakeClock)

        assertEquals(LightSettings.Ignore, picker.getActiveSettings(FakeRooms.LivingRoom))
        assertEquals(LightSettings.Unhandled, picker.getActiveSettings(FakeRooms.FakeHallway))
    }

    @Test
    fun rainy() = runTest {
        val fakeWeather = object: LocalWeatherAccess {
            override val forecast: OngoingFlow<Forecast?> = ongoingFlowOf(idealForecast.copy(
                rainChance = 20.percent,
            ))
            override val conditions: OngoingFlow<Conditions?> = ongoingFlowOf(idealConditions)
        }
        val picker = DayMode(fakeWeather, celestialsAccess, fakeClock)

        assertEquals(LightSettings.Unhandled, picker.getActiveSettings(FakeRooms.LivingRoom))
    }

    @Test
    fun snowy() = runTest {
        val fakeWeather = object: LocalWeatherAccess {
            override val forecast: OngoingFlow<Forecast?> = ongoingFlowOf(idealForecast.copy(
                snowChance = 20.percent,
            ))
            override val conditions: OngoingFlow<Conditions?> = ongoingFlowOf(idealConditions)
        }
        val picker = DayMode(fakeWeather, celestialsAccess, fakeClock)

        assertEquals(LightSettings.Unhandled, picker.getActiveSettings(FakeRooms.LivingRoom))
    }

    @Test
    fun cloudy() = runTest {
        val fakeWeather = object: LocalWeatherAccess {
            override val forecast: OngoingFlow<Forecast?> = ongoingFlowOf(idealForecast)
            override val conditions: OngoingFlow<Conditions?> = ongoingFlowOf(idealConditions.copy(
                cloudCover = 69.percent
            ))
        }
        val picker = DayMode(fakeWeather, celestialsAccess, fakeClock)

        assertEquals(LightSettings.Unhandled, picker.getActiveSettings(FakeRooms.LivingRoom))
    }

    @Test
    fun evening() = runTest {
        val celestialsAccess = object: CelestialAccess {
            override val localCelestials: OngoingFlow<UpcomingCelestials> = ongoingFlowOf(
                UpcomingCelestials(
                    timestamp = fakeClock.zonedDateTime(),
                    today = testCelestials.copy(
                        daylight = testCelestials.daylight.start..now.plus(5.minutes),
                    ),
                    tomorrow = testCelestials,
                )
            )
        }
        val fakeWeather = object: LocalWeatherAccess {
            override val forecast: OngoingFlow<Forecast?> = ongoingFlowOf(idealForecast)
            override val conditions: OngoingFlow<Conditions?> = ongoingFlowOf(idealConditions)
        }
        val picker = DayMode(fakeWeather, celestialsAccess, fakeClock)

        assertEquals(LightSettings.Unhandled, picker.getActiveSettings(FakeRooms.LivingRoom))
    }

    @Test
    fun morning() = runTest {
        val celestialsAccess = object: CelestialAccess {
            override val localCelestials: OngoingFlow<UpcomingCelestials> = ongoingFlowOf(
                UpcomingCelestials(
                    timestamp = fakeClock.zonedDateTime(),
                    today = testCelestials.copy(
                        daylight = now.minus(5.minutes)..testCelestials.daylight.endInclusive,
                    ),
                    tomorrow = testCelestials,
                )
            )
        }
        val fakeWeather = object: LocalWeatherAccess {
            override val forecast: OngoingFlow<Forecast?> = ongoingFlowOf(idealForecast)
            override val conditions: OngoingFlow<Conditions?> = ongoingFlowOf(idealConditions)
        }
        val picker = DayMode(fakeWeather, celestialsAccess, fakeClock)

        assertEquals(LightSettings.Unhandled, picker.getActiveSettings(FakeRooms.LivingRoom))
    }
}
