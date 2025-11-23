package usonia.rules.indicator

import com.github.ajalt.colormath.model.RGB
import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.asOngoing
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.measure.us.inches
import inkapplications.spondee.scalar.percent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventAccessStub
import usonia.foundation.*
import usonia.server.DummyClient
import usonia.weather.Conditions
import usonia.weather.Forecast
import usonia.weather.LocalWeatherAccess
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
        rainChance = 0.percent,
        snowChance = 0.percent,
        lowTemperature = 0.fahrenheit,
        highTemperature = 0.fahrenheit,
        precipitation = 0.percent,
    )

    val fakeConditions = Conditions(
        timestamp = Clock.System.now(),
        cloudCover = 0.percent,
        temperature = 0.fahrenheit,
        rainInLast6Hours = 0.inches,
        isRaining = false,
    )

    val fakeWeather = object: LocalWeatherAccess {
        override val forecast: OngoingFlow<Forecast?> = ongoingFlowOf(fakeForecast)
        override val conditions: OngoingFlow<Conditions?> = ongoingFlowOf(fakeConditions)
    }

    @Test
    fun initialCold() = runTest {
        val spyPublisher = ActionPublisherSpy()
        val client = testClient.copy(
            actionPublisher = spyPublisher,
        )

        val indicator = Indicator(client, fakeWeather)

        val indicatorJob = launch { indicator.startDaemon() }

        advanceUntilIdle()
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
        val fakeWeather = object: LocalWeatherAccess by this@IndicatorTest.fakeWeather {
            override val conditions: OngoingFlow<Conditions?> = ongoingFlowOf(fakeConditions.copy(
                temperature = 100.fahrenheit,
            ))
        }

        val indicator = Indicator(client, fakeWeather)

        val indicatorJob = launch { indicator.startDaemon() }

        advanceUntilIdle()
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
        val fakeWeather = object: LocalWeatherAccess by this@IndicatorTest.fakeWeather {
            override val forecast: OngoingFlow<Forecast?> = ongoingFlowOf(fakeForecast.copy(
                snowChance = 25.percent
            ))
        }

        val indicator = Indicator(client, fakeWeather)

        val indicatorJob = launch { indicator.startDaemon() }

        advanceUntilIdle()
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
        val fakeWeather = object: LocalWeatherAccess by this@IndicatorTest.fakeWeather {
            override val forecast: OngoingFlow<Forecast?> = ongoingFlowOf(fakeForecast.copy(
                rainChance = 25.percent
            ))
        }

        val indicator = Indicator(client, fakeWeather)

        val indicatorJob = launch { indicator.startDaemon() }

        advanceUntilIdle()
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
        val fakeWeather = object: LocalWeatherAccess by this@IndicatorTest.fakeWeather {
            override val forecast: OngoingFlow<Forecast?> = ongoingFlowOf(fakeForecast.copy(
                rainChance = 25.percent,
                snowChance = 25.percent,
            ))
        }

        val indicator = Indicator(client, fakeWeather)

        val indicatorJob = launch { indicator.startDaemon() }

        advanceUntilIdle()
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
        val fakeWeather = object: LocalWeatherAccess by this@IndicatorTest.fakeWeather {
            override val forecast: OngoingFlow<Forecast?> = ongoingFlowOf()
            override val conditions: OngoingFlow<Conditions?> = ongoingFlowOf()
        }

        val indicator = Indicator(client, fakeWeather)

        val indicatorJob = launch { indicator.startDaemon() }
        advanceUntilIdle()
        securityState.value = SecurityState.Armed
        advanceUntilIdle()

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
        val fakeWeather = object: LocalWeatherAccess by this@IndicatorTest.fakeWeather {
            override val forecast: OngoingFlow<Forecast?> = ongoingFlowOf()
            override val conditions: OngoingFlow<Conditions?> = ongoingFlowOf()
        }

        val indicator = Indicator(client, fakeWeather)

        val indicatorJob = launch { indicator.startDaemon() }
        advanceUntilIdle()
        securityState.value = SecurityState.Disarmed
        advanceUntilIdle()

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
