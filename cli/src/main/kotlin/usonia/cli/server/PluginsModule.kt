package usonia.cli.server

import com.inkapplications.datetime.ZonedClock
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import usonia.celestials.CelestialAccess
import usonia.glass.GlassPlugin
import usonia.hubitat.HubitatPlugin
import usonia.hue.HueBridgePlugin
import usonia.notion.NotionBridgePlugin
import usonia.rules.RulesPlugin
import usonia.server.auth.ServerAuthPlugin
import usonia.server.client.BackendClient
import usonia.telegram.TelegramBridgePlugin
import usonia.weather.WeatherPlugin
import usonia.web.WebPlugin

/**
 * Server-side plugins for the Usonia Application.
 */
class PluginsModule(
    client: BackendClient,
    celestialAccess: CelestialAccess,
    logger: KimchiLogger,
    json: Json,
    clock: ZonedClock,
) {
    val serverAuthPlugin = ServerAuthPlugin(client, clock, logger)
    val weatherPlugin = WeatherPlugin(client, clock, logger)
    val plugins = setOf(
        serverAuthPlugin,
        WebPlugin(client, logger),
        GlassPlugin(
            client = client,
            weatherAccess = weatherPlugin.weatherAccess,
            locationWeatherAccess = weatherPlugin.locationWeatherAccess,
            logger = logger,
            json = json,
            clock = clock
        ),
        weatherPlugin,
        RulesPlugin(client, weatherPlugin.weatherAccess, celestialAccess, clock, logger),
        HubitatPlugin(client, logger),
        HueBridgePlugin(client, logger),
        TelegramBridgePlugin(client, logger),
        NotionBridgePlugin(client, logger),
    )
}
