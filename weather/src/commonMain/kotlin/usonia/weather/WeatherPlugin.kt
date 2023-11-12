package usonia.weather

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import regolith.init.Initializer
import regolith.processes.cron.CronJob
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient
import usonia.weather.accuweather.AccuweatherAccess
import usonia.weather.accuweather.AccuweatherApiClient

class WeatherPlugin(
    client: BackendClient,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    private val api = AccuweatherApiClient()
    private val accuweather = AccuweatherAccess(
        api = api,
        client = client,
        logger = logger,
    )

    val weatherAccess: WeatherAccess = accuweather

    override val crons: List<CronJob> = listOf(accuweather)
    override val initializers: List<Initializer> = listOf(accuweather)

}
