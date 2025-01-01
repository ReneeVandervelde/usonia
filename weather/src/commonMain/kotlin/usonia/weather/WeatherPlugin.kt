package usonia.weather

import com.inkapplications.datetime.ZonedClock
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import regolith.init.Initializer
import regolith.processes.cron.CronJob
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient
import usonia.weather.accuweather.AccuweatherAccess
import usonia.weather.accuweather.AccuweatherApiClient
import usonia.weather.nws.NwsApiClient
import usonia.weather.nws.NwsLocationWeatherAccess

class WeatherPlugin(
    client: BackendClient,
    clock: ZonedClock = ZonedClock.System,
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
