package usonia.rules.indicator

import com.github.ajalt.colormath.RGB
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.foundation.*
import usonia.kotlin.suspendedFlow
import usonia.kotlin.unit.percent
import usonia.weather.Conditions
import usonia.weather.Forecast
import usonia.weather.WeatherAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndicatorTest {
    val fakeConfig = object: ConfigurationAccess {
        override val site: Flow<Site> = suspendedFlow(FakeSite.copy(
            rooms = setOf(
                FakeRooms.LivingRoom.copy(
                    devices = setOf(FakeDevices.HueGroup.copy(
                        fixture = Fixture.Indicator,
                    ))
                )
            )
        ))
    }

    val fakeForecast = Forecast(
        Clock.System.now(),
        sunrise = Instant.DISTANT_FUTURE,
        sunset = Instant.DISTANT_FUTURE,
        rainChance = 0.percent,
        snowChance = 0.percent,
    )

    val fakeConditions = Conditions(
        timestamp = Clock.System.now(),
        cloudCover = 0.percent,
        temperature = 0,
    )

    val fakeWeather = object: WeatherAccess {
        override val forecast: Flow<Forecast> = suspendedFlow(fakeForecast)
        override val conditions: Flow<Conditions> = suspendedFlow(fakeConditions)
    }

    @Test
    fun initialCold() = runBlockingTest {
        val spyPublisher = ActionPublisherSpy()

        val indicator = Indicator(fakeWeather, fakeConfig, spyPublisher)

        val indicatorJob = launch { indicator.start() }

        runCurrent()
        assertEquals(1, spyPublisher.actions.size)
        val action = spyPublisher.actions.single()
        assertTrue(action is Action.ColorChange)
        assertEquals(FakeDevices.HueGroup.id, action.target)
        assertEquals(RGB(0, 0, 255), action.color)
        assertEquals(100.percent, action.level)

        indicatorJob.cancelAndJoin()
    }

    @Test
    fun hot() = runBlockingTest {
        val spyPublisher = ActionPublisherSpy()
        val fakeWeather = object: WeatherAccess by this@IndicatorTest.fakeWeather {
            override val conditions: Flow<Conditions> = suspendedFlow(fakeConditions.copy(
                temperature = 100,
            ))
        }

        val indicator = Indicator(fakeWeather, fakeConfig, spyPublisher)

        val indicatorJob = launch { indicator.start() }

        runCurrent()
        assertEquals(1, spyPublisher.actions.size)
        val action = spyPublisher.actions.single()
        assertTrue(action is Action.ColorChange)
        assertEquals(FakeDevices.HueGroup.id, action.target)
        assertEquals(RGB(255, 0, 0), action.color)
        assertEquals(100.percent, action.level)

        indicatorJob.cancelAndJoin()
    }

    @Test
    fun snow() = runBlockingTest {
        val spyPublisher = ActionPublisherSpy()
        val fakeWeather = object: WeatherAccess by this@IndicatorTest.fakeWeather {
            override val forecast: Flow<Forecast> = suspendedFlow(fakeForecast.copy(
                snowChance = 25.percent
            ))
        }

        val indicator = Indicator(fakeWeather, fakeConfig, spyPublisher)

        val indicatorJob = launch { indicator.start() }

        runCurrent()
        assertEquals(1, spyPublisher.actions.size)
        val action = spyPublisher.actions.single()
        assertTrue(action is Action.ColorChange)
        assertEquals(FakeDevices.HueGroup.id, action.target)
        assertEquals(RGB(255, 255, 255), action.color)
        assertEquals(100.percent, action.level)

        indicatorJob.cancelAndJoin()
    }

    @Test
    fun rain() = runBlockingTest {
        val spyPublisher = ActionPublisherSpy()
        val fakeWeather = object: WeatherAccess by this@IndicatorTest.fakeWeather {
            override val forecast: Flow<Forecast> = suspendedFlow(fakeForecast.copy(
                rainChance = 25.percent
            ))
        }

        val indicator = Indicator(fakeWeather, fakeConfig, spyPublisher)

        val indicatorJob = launch { indicator.start() }

        runCurrent()
        assertEquals(1, spyPublisher.actions.size)
        val action = spyPublisher.actions.single()
        assertTrue(action is Action.ColorChange)
        assertEquals(FakeDevices.HueGroup.id, action.target)
        assertEquals(RGB(0, 255, 255), action.color)
        assertEquals(100.percent, action.level)

        indicatorJob.cancelAndJoin()
    }

    @Test
    fun rainAndSnow() = runBlockingTest {
        val spyPublisher = ActionPublisherSpy()
        val fakeWeather = object: WeatherAccess by this@IndicatorTest.fakeWeather {
            override val forecast: Flow<Forecast> = suspendedFlow(fakeForecast.copy(
                rainChance = 25.percent,
                snowChance = 25.percent,
            ))
        }

        val indicator = Indicator(fakeWeather, fakeConfig, spyPublisher)

        val indicatorJob = launch { indicator.start() }

        runCurrent()
        assertEquals(1, spyPublisher.actions.size)
        val action = spyPublisher.actions.single()
        assertTrue(action is Action.ColorChange)
        assertEquals(FakeDevices.HueGroup.id, action.target)
        assertEquals(RGB(255, 255, 255), action.color)
        assertEquals(100.percent, action.level)

        indicatorJob.cancelAndJoin()
    }
}
