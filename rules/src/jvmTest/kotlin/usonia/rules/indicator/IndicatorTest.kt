package usonia.rules.indicator

import com.github.ajalt.colormath.RGB
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import usonia.core.state.*
import usonia.foundation.*
import usonia.kotlin.suspendedFlow
import usonia.kotlin.unit.percent
import usonia.weather.Conditions
import usonia.weather.Forecast
import usonia.weather.WeatherAccess
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IndicatorTest {
    val fakeConfig = object: ConfigurationAccess {
        override val site: Flow<Site> = suspendedFlow(FakeSite.copy(
            users = setOf(FakeUsers.John),
            rooms = setOf(
                FakeRooms.LivingRoom.copy(
                    devices = setOf(FakeDevices.HueGroup.copy(
                        fixture = Fixture.Indicator,
                    ))
                )
            ),
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

        val indicator = Indicator(fakeWeather, fakeConfig, EventAccessStub, spyPublisher)

        val indicatorJob = launch { indicator.start() }

        runCurrent()
        assertEquals(1, spyPublisher.actions.size)
        val action = spyPublisher.actions.single()
        assertTrue(action is Action.ColorChange)
        assertEquals(FakeDevices.HueGroup.id, action.target)
        assertEquals(RGB(0, 0, 255), action.color)
        assertNull(action.level)

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

        val indicator = Indicator(fakeWeather, fakeConfig, EventAccessStub, spyPublisher)

        val indicatorJob = launch { indicator.start() }

        runCurrent()
        assertEquals(1, spyPublisher.actions.size)
        val action = spyPublisher.actions.single()
        assertTrue(action is Action.ColorChange)
        assertEquals(FakeDevices.HueGroup.id, action.target)
        assertEquals(RGB(255, 0, 0), action.color)
        assertNull(action.level)

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

        val indicator = Indicator(fakeWeather, fakeConfig, EventAccessStub, spyPublisher)

        val indicatorJob = launch { indicator.start() }

        runCurrent()
        assertEquals(1, spyPublisher.actions.size)
        val action = spyPublisher.actions.single()
        assertTrue(action is Action.ColorChange)
        assertEquals(FakeDevices.HueGroup.id, action.target)
        assertEquals(RGB(255, 255, 255), action.color)
        assertNull(action.level)

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

        val indicator = Indicator(fakeWeather, fakeConfig, EventAccessStub, spyPublisher)

        val indicatorJob = launch { indicator.start() }

        runCurrent()
        assertEquals(1, spyPublisher.actions.size)
        val action = spyPublisher.actions.single()
        assertTrue(action is Action.ColorChange)
        assertEquals(FakeDevices.HueGroup.id, action.target)
        assertEquals(RGB(0, 255, 255), action.color)
        assertNull(action.level)

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

        val indicator = Indicator(fakeWeather, fakeConfig, EventAccessStub, spyPublisher)

        val indicatorJob = launch { indicator.start() }

        runCurrent()
        assertEquals(1, spyPublisher.actions.size)
        val action = spyPublisher.actions.single()
        assertTrue(action is Action.ColorChange)
        assertEquals(FakeDevices.HueGroup.id, action.target)
        assertEquals(RGB(255, 255, 255), action.color)
        assertNull(action.level)

        indicatorJob.cancelAndJoin()
    }

    @Test
    fun awayBrightness() = runBlockingTest {
        val spyPublisher = ActionPublisherSpy()
        val presence = Event.Presence(
            source = FakeUsers.John.id,
            timestamp = Clock.System.now(),
            state = PresenceState.AWAY,
        )
        val eventAccess = object: EventAccess {
            override val events = MutableSharedFlow<Event>()
            override suspend fun <T : Event> getState(id: Uuid, type: KClass<T>): T? = when (type) {
                Event.Presence::class -> presence as T
                else -> TODO()
            }
        }
        val fakeWeather = object: WeatherAccess by this@IndicatorTest.fakeWeather {
            override val forecast: Flow<Forecast> = flow {}
            override val conditions: Flow<Conditions> = flow {}
        }

        val indicator = Indicator(fakeWeather, fakeConfig, eventAccess, spyPublisher)

        val indicatorJob = launch { indicator.start() }
        eventAccess.events.emit(presence)

        runCurrent()
        assertEquals(1, spyPublisher.actions.size)
        val action = spyPublisher.actions.single()
        assertTrue(action is Action.Dim)
        assertEquals(FakeDevices.HueGroup.id, action.target)
        assertEquals(5.percent, action.level)

        indicatorJob.cancelAndJoin()
    }

    @Test
    fun presentBrightness() = runBlockingTest {
        val spyPublisher = ActionPublisherSpy()
        val presence = Event.Presence(
            source = FakeUsers.John.id,
            timestamp = Clock.System.now(),
            state = PresenceState.HOME,
        )
        val eventAccess = object: EventAccess {
            override val events = MutableSharedFlow<Event>()
            override suspend fun <T : Event> getState(id: Uuid, type: KClass<T>): T? = when (type) {
                Event.Presence::class -> presence as T
                else -> TODO()
            }
        }
        val fakeWeather = object: WeatherAccess by this@IndicatorTest.fakeWeather {
            override val forecast: Flow<Forecast> = flow {}
            override val conditions: Flow<Conditions> = flow {}
        }

        val indicator = Indicator(fakeWeather, fakeConfig, eventAccess, spyPublisher)

        val indicatorJob = launch { indicator.start() }
        eventAccess.events.emit(presence)

        runCurrent()
        assertEquals(1, spyPublisher.actions.size)
        val action = spyPublisher.actions.single()
        assertTrue(action is Action.Dim)
        assertEquals(FakeDevices.HueGroup.id, action.target)
        assertEquals(100.percent, action.level)

        indicatorJob.cancelAndJoin()
    }
}
