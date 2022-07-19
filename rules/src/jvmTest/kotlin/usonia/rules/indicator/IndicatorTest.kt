package usonia.rules.indicator

import com.github.ajalt.colormath.RGB
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import usonia.core.state.*
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.kotlin.unit.percent
import usonia.server.DummyClient
import usonia.weather.Conditions
import usonia.weather.Forecast
import usonia.weather.WeatherAccess
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IndicatorTest {
    val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
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

    val testClient = DummyClient.copy(
        configurationAccess = fakeConfig,
        eventAccess = EventAccessStub,
    )

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
        override val forecast: OngoingFlow<Forecast> = ongoingFlowOf(fakeForecast)
        override val conditions: OngoingFlow<Conditions> = ongoingFlowOf(fakeConditions)
    }

    @Test
    fun initialCold() = runTest {
        val spyPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            actionPublisher = spyPublisher,
        )

        val indicator = Indicator(client, fakeWeather)

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
    fun hot() = runTest {
        val spyPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            actionPublisher = spyPublisher,
        )
        val fakeWeather = object: WeatherAccess by this@IndicatorTest.fakeWeather {
            override val conditions: OngoingFlow<Conditions> = ongoingFlowOf(fakeConditions.copy(
                temperature = 100,
            ))
        }

        val indicator = Indicator(client, fakeWeather)

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
    fun snow() = runTest {
        val spyPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            actionPublisher = spyPublisher,
        )
        val fakeWeather = object: WeatherAccess by this@IndicatorTest.fakeWeather {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf(fakeForecast.copy(
                snowChance = 25.percent
            ))
        }

        val indicator = Indicator(client, fakeWeather)

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
    fun rain() = runTest {
        val spyPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            actionPublisher = spyPublisher,
        )
        val fakeWeather = object: WeatherAccess by this@IndicatorTest.fakeWeather {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf(fakeForecast.copy(
                rainChance = 25.percent
            ))
        }

        val indicator = Indicator(client, fakeWeather)

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
    fun rainAndSnow() = runTest {
        val spyPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            actionPublisher = spyPublisher,
        )
        val fakeWeather = object: WeatherAccess by this@IndicatorTest.fakeWeather {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf(fakeForecast.copy(
                rainChance = 25.percent,
                snowChance = 25.percent,
            ))
        }

        val indicator = Indicator(client, fakeWeather)

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
    fun awayBrightness() = runTest {
        val spyPublisher = ActionPublisherSpy()
        val presence = Event.Presence(
            source = FakeUsers.John.id,
            timestamp = Clock.System.now(),
            state = PresenceState.AWAY,
        )
        val eventAccess = object: EventAccessFake() {
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? = when (type) {
                Event.Presence::class -> presence as T
                else -> TODO()
            }
        }
        val client = testClient.copy(
            actionPublisher = spyPublisher,
            eventAccess = eventAccess,
        )
        val fakeWeather = object: WeatherAccess by this@IndicatorTest.fakeWeather {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf()
            override val conditions: OngoingFlow<Conditions> = ongoingFlowOf()
        }

        val indicator = Indicator(client, fakeWeather)

        val indicatorJob = launch { indicator.start() }
        runCurrent()
        eventAccess.mutableEvents.emit(presence)
        runCurrent()

        assertEquals(1, spyPublisher.actions.size)
        val action = spyPublisher.actions.single()
        assertTrue(action is Action.Dim)
        assertEquals(FakeDevices.HueGroup.id, action.target)
        assertEquals(5.percent, action.level)

        indicatorJob.cancelAndJoin()
    }

    @Test
    fun presentBrightness() = runTest {
        val spyPublisher = ActionPublisherSpy()
        val presence = Event.Presence(
            source = FakeUsers.John.id,
            timestamp = Clock.System.now(),
            state = PresenceState.HOME,
        )
        val eventAccess = object: EventAccessFake() {
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? = when (type) {
                Event.Presence::class -> presence as T
                else -> TODO()
            }
        }
        val client = testClient.copy(
            actionPublisher = spyPublisher,
            eventAccess = eventAccess,
        )
        val fakeWeather = object: WeatherAccess by this@IndicatorTest.fakeWeather {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf()
            override val conditions: OngoingFlow<Conditions> = ongoingFlowOf()
        }

        val indicator = Indicator(client, fakeWeather)

        val indicatorJob = launch { indicator.start() }
        runCurrent()
        eventAccess.mutableEvents.emit(presence)
        runCurrent()

        assertEquals(1, spyPublisher.actions.size)
        val action = spyPublisher.actions.single()
        assertTrue(action is Action.Dim)
        assertEquals(FakeDevices.HueGroup.id, action.target)
        assertEquals(100.percent, action.level)

        indicatorJob.cancelAndJoin()
    }
}
