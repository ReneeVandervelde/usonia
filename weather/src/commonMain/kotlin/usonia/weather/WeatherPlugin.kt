package usonia.weather

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.state.ConfigurationAccess
import usonia.server.ServerPlugin
import usonia.server.cron.CronJob
import usonia.weather.accuweather.AccuweatherAccess
import usonia.weather.accuweather.AccuweatherApiClient

class WeatherPlugin(
    config: ConfigurationAccess,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    private val api = AccuweatherApiClient()
    private val accuweather = AccuweatherAccess(
        api = api,
        config = config,
        logger = logger
    )

    val weatherAccess: WeatherAccess = accuweather

    override val crons: List<CronJob> = listOf(accuweather)
}
