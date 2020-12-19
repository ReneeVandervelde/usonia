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

    private val configured = FakeSite.copy(
        bridges = setOf(
            FakeBridge.copy(
                service = "accuweather",
                parameters = mapOf(
                    "token" to "test-token",
                    "location" to "test-location",
                )
            )
        )
    )

    private val now = Instant.fromEpochSeconds(5)

    private val stubClock = object: Clock {
        override fun now(): Instant = now
    }

    @Test
    fun noConfig() = runBlockingTest {
        val config = object: ConfigurationAccess {
            override val site: Flow<Site> = flowOf(FakeSite)
        }
        val access = AccuweatherAccess(
            api = fakeApi,
            config = config,
        )

        val forecasts = mutableListOf<Forecast>()
        val conditions = mutableListOf<Conditions>()

        val forecastCollection = launch { access.forecast.collect { forecasts += it } }
        val conditionCollection = launch { access.conditions.collect { conditions += it } }

        pauseDispatcher {
            access.start()
            access.run(now.toLocalDateTime(TimeZone.UTC))
        }

        assertEquals(0, forecasts.size)
        assertEquals(0, conditions.size)

        forecastCollection.cancelAndJoin()
        conditionCollection.cancelAndJoin()
    }

    @Test
    fun startConfig() = runBlockingTest {
        val config = object: ConfigurationAccess {
            override val site: Flow<Site> = flowOf(configured)
        }
        val access = AccuweatherAccess(
            api = fakeApi,
            config = config,
            clock = stubClock,
        )

        val forecasts = mutableListOf<Forecast>()
        val conditions = mutableListOf<Conditions>()

        val forecastCollection = launch { access.forecast.collect { forecasts += it } }
        val conditionCollection = launch { access.conditions.collect { conditions += it } }

        pauseDispatcher {
            access.start()
        }

        assertEquals(1, forecasts.size)
        assertEquals(1, conditions.size)

        assertEquals(5, forecasts.single().timestamp.epochSeconds)
        assertEquals(1, forecasts.single().sunrise.epochSeconds)
        assertEquals(4, forecasts.single().sunset.epochSeconds)
        assertEquals(12.percent, forecasts.single().snowChance)
        assertEquals(34.percent, forecasts.single().rainChance)

        assertEquals(5, conditions.single().timestamp.epochSeconds)
        assertEquals(69.percent, conditions.single().cloudCover)
        assertEquals(42, conditions.single().temperature)

        forecastCollection.cancelAndJoin()
        conditionCollection.cancelAndJoin()
    }

    @Test
    fun updatedConditions() = runBlockingTest {
        val config = object: ConfigurationAccess {
            override val site: Flow<Site> = flowOf(configured)
        }
        val access = AccuweatherAccess(
            api = fakeApi,
            config = config,
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
            access.run(now.toLocalDateTime(TimeZone.UTC))
        }

        assertEquals(1, forecasts.size)
        assertEquals(2, conditions.size)

        assertEquals(69.percent, conditions[0].cloudCover)
        assertEquals(42, conditions[0].temperature)

        assertEquals(100.percent, conditions[1].cloudCover)
        assertEquals(99, conditions[1].temperature)

        forecastCollection.cancelAndJoin()
        conditionCollection.cancelAndJoin()
    }

    @Test
    fun updateForecast() = runBlockingTest {
        val config = object: ConfigurationAccess {
            override val site: Flow<Site> = flowOf(configured)
        }
        val fakeClock = object: Clock {
            var current = this@AccuweatherAccessTest.now
            override fun now(): Instant = current
        }
        val access = AccuweatherAccess(
            api = fakeApi,
            config = config,
            clock = fakeClock
        )

        val forecasts = mutableListOf<Forecast>()
        val conditions = mutableListOf<Conditions>()

        val forecastCollection = launch { access.forecast.collect { forecasts += it } }
        val conditionCollection = launch { access.conditions.collect { conditions += it } }

        pauseDispatcher {
            access.start()
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
            access.run(now.toLocalDateTime(TimeZone.UTC))
        }

        assertEquals(2, forecasts.size)
        assertEquals(2, conditions.size)

        assertEquals(1, forecasts[0].sunrise.epochSeconds)
        assertEquals(4, forecasts[0].sunset.epochSeconds)
        assertEquals(12.percent, forecasts[0].snowChance)
        assertEquals(34.percent, forecasts[0].rainChance)

        assertEquals(20, forecasts[1].sunrise.epochSeconds)
        assertEquals(25, forecasts[1].sunset.epochSeconds)
        assertEquals(56.percent, forecasts[1].snowChance)
        assertEquals(78.percent, forecasts[1].rainChance)

        forecastCollection.cancelAndJoin()
        conditionCollection.cancelAndJoin()
    }
}
