package usonia.rules.indicator

import com.github.ajalt.colormath.model.RGB
import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.measure.us.inches
import inkapplications.spondee.scalar.percent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import usonia.core.state.*
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.asOngoing
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import usonia.weather.Conditions
import usonia.weather.Forecast
import usonia.weather.WeatherAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
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
        lowTemperature = 0.fahrenheit,
        highTemperature = 0.fahrenheit,
    )

    val fakeConditions = Conditions(
        timestamp = Clock.System.now(),
        cloudCover = 0.percent,
        temperature = 0,
        rainInLast6Hours = 0.inches,
        isRaining = false,
    )

    val fakeWeather = object: WeatherAccess {
        override val forecast: OngoingFlow<Forecast> = ongoingFlowOf(fakeForecast)
        override val conditions: OngoingFlow<Conditions> = ongoingFlowOf(fakeConditions)
        override val currentConditions: Conditions get() = TODO()
        override val currentForecast: Forecast get() = TODO()
    }

    @Test
    fun initialCold() = runTest {
        val spyPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            actionPublisher = spyPublisher,
        )

        val indicator = Indicator(client, fakeWeather)

        val indicatorJob = launch { indicator.startDaemon() }

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

        val indicatorJob = launch { indicator.startDaemon() }

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

        val indicatorJob = launch { indicator.startDaemon() }

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

        val indicatorJob = launch { indicator.startDaemon() }

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

        val indicatorJob = launch { indicator.startDaemon() }

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
        val securityState = MutableStateFlow(SecurityState.Disarmed)
        val client = testClient.copy(
            actionPublisher = spyPublisher,
            configurationAccess = object: ConfigurationAccess by fakeConfig {
                override val securityState = securityState.asOngoing()
            },
        )
        val fakeWeather = object: WeatherAccess by this@IndicatorTest.fakeWeather {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf()
            override val conditions: OngoingFlow<Conditions> = ongoingFlowOf()
        }

        val indicator = Indicator(client, fakeWeather)

        val indicatorJob = launch { indicator.startDaemon() }
        runCurrent()
        securityState.value = SecurityState.Armed
        runCurrent()

        assertEquals(2, spyPublisher.actions.size)
        val initialDimAction = spyPublisher.actions[0]
        assertTrue(initialDimAction is Action.Dim)
        assertEquals(FakeDevices.HueGroup.id, initialDimAction.target)
        assertEquals(80.percent, initialDimAction.level)
        val changeAction = spyPublisher.actions[1]
        assertTrue(changeAction is Action.Dim)
        assertEquals(FakeDevices.HueGroup.id, changeAction.target)
        assertEquals(1.percent, changeAction.level)

        indicatorJob.cancelAndJoin()
    }

    @Test
    fun presentBrightness() = runTest {
        val spyPublisher = ActionPublisherSpy()
        val securityState = MutableStateFlow(SecurityState.Armed)
        val client = testClient.copy(
            actionPublisher = spyPublisher,
            configurationAccess = object: ConfigurationAccess by fakeConfig {
                override val securityState = securityState.asOngoing()
            },
        )
        val fakeWeather = object: WeatherAccess by this@IndicatorTest.fakeWeather {
            override val forecast: OngoingFlow<Forecast> = ongoingFlowOf()
            override val conditions: OngoingFlow<Conditions> = ongoingFlowOf()
        }

        val indicator = Indicator(client, fakeWeather)

        val indicatorJob = launch { indicator.startDaemon() }
        runCurrent()
        securityState.value = SecurityState.Disarmed
        runCurrent()

        assertEquals(2, spyPublisher.actions.size)
        val initialDimAction = spyPublisher.actions[0]
        assertTrue(initialDimAction is Action.Dim)
        assertEquals(FakeDevices.HueGroup.id, initialDimAction.target)
        assertEquals(1.percent, initialDimAction.level)
        val changeAction = spyPublisher.actions[1]
        assertTrue(changeAction is Action.Dim)
        assertEquals(FakeDevices.HueGroup.id, changeAction.target)
        assertEquals(80.percent, changeAction.level)

        indicatorJob.cancelAndJoin()
    }
}
