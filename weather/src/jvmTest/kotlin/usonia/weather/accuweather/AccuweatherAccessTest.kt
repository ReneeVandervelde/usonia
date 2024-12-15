package usonia.weather.accuweather

import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.measure.us.inches
import inkapplications.spondee.measure.us.toFahrenheit
import inkapplications.spondee.measure.us.toInches
import inkapplications.spondee.scalar.percent
import inkapplications.spondee.structure.toFloat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.FakeBridge
import usonia.foundation.FakeSite
import usonia.foundation.Site
import usonia.kotlin.OngoingFlow
import usonia.kotlin.collect
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import usonia.server.test.DummyManager
import usonia.weather.Conditions
import usonia.weather.FullForecast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalCoroutinesApi::class)
class AccuweatherAccessTest {
    private val fakeApi = object: AccuweatherApi {
        var forecast = ForecastResponse(
            daily = listOf(
                ForecastResponse.Daily(
                    sun = ForecastResponse.SunSchedule(
                        rise = 1,
                        set = 4,
                    ),
                    day = ForecastResponse.DayConditions(
                        snowProbability = 12,
                        rainProbability = 34,
                    ),
                    temperature = ForecastResponse.TemperatureConditions(
                        min = ForecastResponse.TemperatureConditions.TemperatureValues(
                            value = 30f
                        ),
                        max = ForecastResponse.TemperatureConditions.TemperatureValues(
                            value = 60f
                        )
                    )
                )
            )
        )

        var conditions = ConditionsResponse(
            cloudCover = 69,
            temperature = ConditionsResponse.TemperatureTypes(
                imperial = ConditionsResponse.TemperatureTypes.TemperatureData(
                    temperature = 42f
                )
            ),
            precipitation = ConditionsResponse.PrecipitationSummary(
                pastSixHours = ConditionsResponse.PrecipitationSummary.PrecipitationTypes(
                    imperial = ConditionsResponse.PrecipitationSummary.PrecipitationTypes.PrecipitationData(
                        value = 0f
                    )
                )
            ),
            hasPrecipitation = false
        )

        override suspend fun getForecast(locationId: String, apiKey: String): ForecastResponse {
            return forecast
        }

        override suspend fun getConditions(locationId: String, apiKey: String): ConditionsResponse {
            return conditions
        }
    }
    private val testClient = DummyClient.copy(
        configurationAccess = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
                bridges = setOf(
                    FakeBridge.copy(
                        service = "accuweather",
                        parameters = mapOf(
                            "token" to "test-token",
                            "location" to "test-location",
                        )
                    )
                )
            ))
        }
    )

    private val now = Clock.System.now()

    private val stubClock = object: Clock {
        override fun now(): Instant = now
    }

    @Test
    fun noConfig() = runTest {
        val config = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite)
        }
        val client = testClient.copy(
            configurationAccess = config,
        )
        val access = AccuweatherAccess(
            api = fakeApi,
            client = client,
        )

        val forecasts = mutableListOf<FullForecast>()
        val conditions = mutableListOf<Conditions>()

        val forecastCollection = launch { access.forecast.collect { forecasts += it } }
        val conditionCollection = launch { access.conditions.collect { conditions += it } }
        runCurrent()

        access.initialize(DummyManager)
        access.runCron(Clock.System.now().toLocalDateTime(TimeZone.UTC), TimeZone.UTC)
        runCurrent()

        assertEquals(1, forecasts.size)
        assertEquals(1, conditions.size)

        forecastCollection.cancelAndJoin()
        conditionCollection.cancelAndJoin()
    }

    @Test
    fun startConfig() = runTest {
        val access = AccuweatherAccess(
            api = fakeApi,
            client = testClient,
            clock = stubClock,
        )

        val forecasts = mutableListOf<FullForecast>()
        val conditions = mutableListOf<Conditions>()

        val forecastCollection = launch { access.forecast.collect { forecasts += it } }
        val conditionCollection = launch { access.conditions.collect { conditions += it } }
        runCurrent()

        access.initialize(DummyManager)
        runCurrent()

        assertEquals(2, forecasts.size)
        assertEquals(2, conditions.size)

        assertEquals(now, forecasts[1].timestamp)
        assertEquals(1, forecasts[1].sunrise.epochSeconds)
        assertEquals(4, forecasts[1].sunset.epochSeconds)
        assertEquals(12.percent, forecasts[1].snowChance)
        assertEquals(34.percent, forecasts[1].rainChance)

        assertEquals(now, conditions[1].timestamp)
        assertEquals(69.percent, conditions[1].cloudCover)
        assertEquals(42, conditions[1].temperature)

        forecastCollection.cancelAndJoin()
        conditionCollection.cancelAndJoin()
    }

    @Test
    fun updatedConditions() = runTest {
        val access = AccuweatherAccess(
            api = fakeApi,
            client = testClient,
            clock = stubClock,
        )

        val forecasts = mutableListOf<FullForecast>()
        val conditions = mutableListOf<Conditions>()

        val forecastCollection = launch { access.forecast.collect { forecasts += it } }
        val conditionCollection = launch { access.conditions.collect { conditions += it } }
        runCurrent()

        access.initialize(DummyManager)
        fakeApi.conditions = ConditionsResponse(
            cloudCover = 100,
            temperature = ConditionsResponse.TemperatureTypes(
                imperial = ConditionsResponse.TemperatureTypes.TemperatureData(
                    temperature = 99f
                )
            ),
            precipitation = ConditionsResponse.PrecipitationSummary(
                pastSixHours = ConditionsResponse.PrecipitationSummary.PrecipitationTypes(
                    imperial = ConditionsResponse.PrecipitationSummary.PrecipitationTypes.PrecipitationData(
                        value = 1.2f
                    )
                )
            ),
            hasPrecipitation = true
        )
        access.runCron(Clock.System.now().toLocalDateTime(TimeZone.UTC), TimeZone.UTC)
        runCurrent()

        assertEquals(2, forecasts.size)
        assertEquals(3, conditions.size)

        assertEquals(69.percent, conditions[1].cloudCover)
        assertEquals(42, conditions[1].temperature)
        assertEquals(0.inches.toFloat(), conditions[1].rainInLast6Hours.toInches().toFloat(), 1e-10f)
        assertEquals(false, conditions[1].isRaining)

        assertEquals(100.percent, conditions[2].cloudCover)
        assertEquals(99, conditions[2].temperature)
        assertEquals(1.2.inches.toFloat(), conditions[2].rainInLast6Hours.toInches().toFloat(), 1e-10f)
        assertEquals(true, conditions[2].isRaining)

        forecastCollection.cancelAndJoin()
        conditionCollection.cancelAndJoin()
    }

    @Test
    fun updateForecast() = runTest {
        val fakeClock = object: Clock {
            var current = this@AccuweatherAccessTest.now
            override fun now(): Instant = current
        }
        val access = AccuweatherAccess(
            api = fakeApi,
            client = testClient,
            clock = fakeClock
        )

        val forecasts = mutableListOf<FullForecast>()
        val conditions = mutableListOf<Conditions>()

        val forecastCollection = launch { access.forecast.collect { forecasts += it } }
        val conditionCollection = launch { access.conditions.collect { conditions += it } }

        access.initialize(DummyManager)
        runCurrent()
        fakeClock.current = fakeClock.current + 5.hours
        fakeApi.forecast = ForecastResponse(
            daily = listOf(
                ForecastResponse.Daily(
                    sun = ForecastResponse.SunSchedule(
                        rise = 20,
                        set = 25,
                    ),
                    day = ForecastResponse.DayConditions(
                        snowProbability = 56,
                        rainProbability = 78,
                    ),
                    temperature = ForecastResponse.TemperatureConditions(
                        min = ForecastResponse.TemperatureConditions.TemperatureValues(
                            value = 56f
                        ),
                        max = ForecastResponse.TemperatureConditions.TemperatureValues(
                            value = 78f
                        ),
                    )
                )
            )
        )
        access.runCron(Clock.System.now().toLocalDateTime(TimeZone.UTC), TimeZone.UTC)
        runCurrent()

        assertEquals(3, forecasts.size)
        assertEquals(3, conditions.size)

        assertEquals(1, forecasts[1].sunrise.epochSeconds)
        assertEquals(4, forecasts[1].sunset.epochSeconds)
        assertEquals(12.percent, forecasts[1].snowChance)
        assertEquals(34.percent, forecasts[1].rainChance)
        assertEquals(30.fahrenheit.toFloat(), forecasts[1].lowTemperature.toFahrenheit().toFloat(), 1e-10f)
        assertEquals(60.fahrenheit.toFloat(), forecasts[1].highTemperature.toFahrenheit().toFloat(), 1e-10f)

        assertEquals(20, forecasts[2].sunrise.epochSeconds)
        assertEquals(25, forecasts[2].sunset.epochSeconds)
        assertEquals(56.percent, forecasts[2].snowChance)
        assertEquals(78.percent, forecasts[2].rainChance)
        assertEquals(56.fahrenheit.toFloat(), forecasts[2].lowTemperature.toFahrenheit().toFloat(), 1e-10f)
        assertEquals(78.fahrenheit.toFloat(), forecasts[2].highTemperature.toFahrenheit().toFloat(), 1e-10f)

        forecastCollection.cancelAndJoin()
        conditionCollection.cancelAndJoin()
    }
}
