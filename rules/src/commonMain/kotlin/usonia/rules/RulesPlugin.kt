package usonia.rules

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.rules.alerts.WaterMonitor
import usonia.rules.indicator.Indicator
import usonia.rules.lights.AwayMode
import usonia.rules.lights.CircadianColors
import usonia.rules.lights.CompositeLightingPicker
import usonia.rules.lights.LightController
import usonia.rules.lights.MovieMode
import usonia.server.Daemon
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient
import usonia.weather.WeatherAccess

class RulesPlugin(
    client: BackendClient,
    weather: WeatherAccess,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    private val movieMode = MovieMode(client)

    private val colorPicker = CompositeLightingPicker(
        AwayMode(client),
        movieMode,
        CircadianColors(client, weather, logger = logger),
    )

    override val daemons: List<Daemon> = listOf(
        WaterMonitor(client, logger),
        LightController(client, colorPicker, logger),
        Indicator(client, weather, logger),
        movieMode,
    )
}
