package usonia.cli.server

import kimchi.logger.KimchiLogger
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import usonia.glass.GlassPlugin
import usonia.hubitat.HubitatPlugin
import usonia.hue.HueBridgePlugin
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
    clock: Clock,
) {
    val weatherPlugin = WeatherPlugin(client, logger)
    val plugins = setOf(
        WebPlugin(client, logger),
        GlassPlugin(client, logger, json, clock),
        weatherPlugin,
        TodoistBridgePlugin(client, logger),
        RulesPlugin(client, weatherPlugin.weatherAccess, logger),
        HubitatPlugin(client, logger),
        HueBridgePlugin(client, logger),
        TelegramBridgePlugin(client, logger),
        NotionBridgePlugin(client, logger),
    )
}
