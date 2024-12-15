package usonia.cli.server

import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import usonia.glass.GlassPlugin
import usonia.hubitat.HubitatPlugin
import usonia.hue.HueBridgePlugin
import usonia.kotlin.datetime.ZonedClock
import usonia.notion.NotionBridgePlugin
import usonia.rules.RulesPlugin
import usonia.server.client.BackendClient
import usonia.telegram.TelegramBridgePlugin
import usonia.todoist.TodoistBridgePlugin
import usonia.weather.WeatherPlugin
import usonia.web.WebPlugin

/**
 * Server-side plugins for the Usonia Application.
 */
class PluginsModule(
    client: BackendClient,
    logger: KimchiLogger,
    json: Json,
    clock: ZonedClock,
) {
    val weatherPlugin = WeatherPlugin(client, clock, logger)
    val plugins = setOf(
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
        TodoistBridgePlugin(client, logger),
        RulesPlugin(client, weatherPlugin.weatherAccess, logger),
        HubitatPlugin(client, logger),
        HueBridgePlugin(client, logger),
        TelegramBridgePlugin(client, logger),
        NotionBridgePlugin(client, logger),
    )
}
