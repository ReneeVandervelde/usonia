package usonia.weather

import com.inkapplications.datetime.ZonedClock
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import regolith.processes.cron.CronJob
import regolith.processes.daemon.Daemon
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient
import usonia.weather.nws.NwsApiClient
import usonia.weather.nws.NwsLocationWeatherAccess

class WeatherPlugin(
    client: BackendClient,
    clock: ZonedClock = ZonedClock.System,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    private val nwsApi = NwsApiClient()
    private val nws = NwsLocationWeatherAccess(
        api = nwsApi,
        client = client,
        clock = clock,
        logger = logger,
    )

    val weatherAccess: LocalWeatherAccess = nws
    val locationWeatherAccess: LocationWeatherAccess = nws

    override val crons: List<CronJob> = listOf(nws)
    override val daemons: List<Daemon> = listOf(nws)
}
