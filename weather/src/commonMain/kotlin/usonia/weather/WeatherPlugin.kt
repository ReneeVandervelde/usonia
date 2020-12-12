package usonia.weather

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.Plugin
import usonia.core.cron.CronJob
import usonia.state.ConfigurationAccess
import usonia.weather.accuweather.AccuweatherAccess
import usonia.weather.accuweather.AccuweatherApiClient

class WeatherPlugin(
    config: ConfigurationAccess,
    logger: KimchiLogger = EmptyLogger,
): Plugin {
    private val api = AccuweatherApiClient()
    private val accuweather = AccuweatherAccess(
        api = api,
        config = config,
        logger = logger
    )

    val weatherAccess: WeatherAccess = accuweather

    override val crons: List<CronJob> = listOf(accuweather)
}
