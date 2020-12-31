package usonia.weather.accuweather

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import usonia.core.state.ConfigurationAccess
import usonia.foundation.FakeBridge
import usonia.foundation.FakeSite
import usonia.foundation.Site
import usonia.kotlin.unit.percent
import usonia.server.DummyClient
import usonia.weather.Conditions
import usonia.weather.Forecast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.hours

@OptIn(ExperimentalTime::class)
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
                )
            )
        )

        var conditions = ConditionsResponse(
            cloudCover = 69,
            temperature = ConditionsResponse.TemperatureTypes(
                imperial = ConditionsResponse.TemperatureTypes.TemperatureData(
                    temperature = 42f
                )
            )
        )

        override suspend fun getForecast(locationId: String, apiKey: String): ForecastResponse {
            return forecast
        }

        override suspend fun getConditions(locationId: String, apiKey: String): ConditionsResponse {
            return conditions
        }
    }
    private val testClient = DummyClient.copy(
        configurationAccess = object: ConfigurationAccess {
            override val site: Flow<Site> = flowOf(FakeSite.copy(
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
    fun noConfig() = runBlockingTest {
        val config = object: ConfigurationAccess {
            override val site: Flow<Site> = flowOf(FakeSite)
        }
        val client = testClient.copy(
            configurationAccess = config,
        )
        val access = AccuweatherAccess(
            api = fakeApi,
            client = client,
        )

        val forecasts = mutableListOf<Forecast>()
        val conditions = mutableListOf<Conditions>()

        val forecastCollection = launch { access.forecast.collect { forecasts += it } }
        val conditionCollection = launch { access.conditions.collect { conditions += it } }

        pauseDispatcher {
            access.start()
            access.run(now.toLocalDateTime(TimeZone.UTC), TimeZone.UTC)
        }

        assertEquals(1, forecasts.size)
        assertEquals(1, conditions.size)

        forecastCollection.cancelAndJoin()
        conditionCollection.cancelAndJoin()
    }

    @Test
    fun startConfig() = runBlockingTest {
        val access = AccuweatherAccess(
            api = fakeApi,
            client = testClient,
            clock = stubClock,
        )

        val forecasts = mutableListOf<Forecast>()
        val conditions = mutableListOf<Conditions>()

        val forecastCollection = launch { access.forecast.collect { forecasts += it } }
        val conditionCollection = launch { access.conditions.collect { conditions += it } }

        pauseDispatcher {
            access.start()
        }

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
    fun updatedConditions() = runBlockingTest {
        val access = AccuweatherAccess(
            api = fakeApi,
            client = testClient,
            clock = stubClock,
        )

        val forecasts = mutableListOf<Forecast>()
        val conditions = mutableListOf<Conditions>()

        val forecastCollection = launch { access.forecast.collect { forecasts += it } }
        val conditionCollection = launch { access.conditions.collect { conditions += it } }

        pauseDispatcher {
            access.start()
            fakeApi.conditions = ConditionsResponse(
                cloudCover = 100,
                temperature = ConditionsResponse.TemperatureTypes(
                    imperial = ConditionsResponse.TemperatureTypes.TemperatureData(
                        temperature = 99f
                    )
                )
            )
            access.run(now.toLocalDateTime(TimeZone.UTC), TimeZone.UTC)
        }

        assertEquals(2, forecasts.size)
        assertEquals(3, conditions.size)

        assertEquals(69.percent, conditions[1].cloudCover)
        assertEquals(42, conditions[1].temperature)

        assertEquals(100.percent, conditions[2].cloudCover)
        assertEquals(99, conditions[2].temperature)

        forecastCollection.cancelAndJoin()
        conditionCollection.cancelAndJoin()
    }

    @Test
    fun updateForecast() = runBlockingTest {
        val fakeClock = object: Clock {
            var current = this@AccuweatherAccessTest.now
            override fun now(): Instant = current
        }
        val access = AccuweatherAccess(
            api = fakeApi,
            client = testClient,
            clock = fakeClock
        )

        val forecasts = mutableListOf<Forecast>()
        val conditions = mutableListOf<Conditions>()

        val forecastCollection = launch { access.forecast.collect { forecasts += it } }
        val conditionCollection = launch { access.conditions.collect { conditions += it } }

        access.start()
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
                    )
                )
            )
        )
        access.run(now.toLocalDateTime(TimeZone.UTC), TimeZone.UTC)
        runCurrent()

        assertEquals(3, forecasts.size)
        assertEquals(3, conditions.size)

        assertEquals(1, forecasts[1].sunrise.epochSeconds)
        assertEquals(4, forecasts[1].sunset.epochSeconds)
        assertEquals(12.percent, forecasts[1].snowChance)
        assertEquals(34.percent, forecasts[1].rainChance)

        assertEquals(20, forecasts[2].sunrise.epochSeconds)
        assertEquals(25, forecasts[2].sunset.epochSeconds)
        assertEquals(56.percent, forecasts[2].snowChance)
        assertEquals(78.percent, forecasts[2].rainChance)

        forecastCollection.cancelAndJoin()
        conditionCollection.cancelAndJoin()
    }
}
