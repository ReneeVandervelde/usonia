package usonia.weather

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import regolith.init.Initializer
import regolith.processes.cron.CronJob
import usonia.kotlin.datetime.ZonedClock
import usonia.kotlin.datetime.ZonedSystemClock
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient
import usonia.weather.accuweather.AccuweatherAccess
import usonia.weather.accuweather.AccuweatherApiClient
import usonia.weather.nws.NwsApiClient
import usonia.weather.nws.NwsLocationWeatherAccess

class WeatherPlugin(
    client: BackendClient,
    clock: ZonedClock = ZonedSystemClock,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    private val api = AccuweatherApiClient()
    private val accuweather = AccuweatherAccess(
        api = api,
        client = client,
        logger = logger,
    )
    private val nwsApi = NwsApiClient()
    private val nws = NwsLocationWeatherAccess(
        api = nwsApi,
        clock = clock,
        logger = logger,
    )

    val weatherAccess: LocalWeatherAccess = accuweather
    val locationWeatherAccess: LocationWeatherAccess = nws

    override val crons: List<CronJob> = listOf(accuweather)
    override val initializers: List<Initializer> = listOf(accuweather)

}
