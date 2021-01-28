package usonia.web

import kimchi.logger.KimchiLogger
import usonia.serialization.SerializationModule
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient
import usonia.server.http.HttpController
import usonia.server.http.WebSocketController
import usonia.web.actions.ActionBridgeHttpPublisher
import usonia.web.actions.ActionHttpPublisher
import usonia.web.config.*
import usonia.web.config.ConfigSocket
import usonia.web.events.EventBridgeHttpPublisher
import usonia.web.events.EventHttpPublisher
import usonia.web.events.EventSocket
import usonia.web.events.LatestEventController

class WebPlugin(
    client: BackendClient,
    logger: KimchiLogger,
): ServerPlugin {
    private val json = SerializationModule.json

    override val httpControllers: List<HttpController> = listOf(
        ControlPanelController,
        EventHttpPublisher(client, json, logger),
        EventBridgeHttpPublisher(client, json, logger),
        ActionHttpPublisher(client, json, logger),
        ActionBridgeHttpPublisher(client, json, logger),
        LatestEventController(client, json),
        SiteUpdateController(client, json, logger),
        FlagUpdateController(client, json, logger),
        FlagDeleteController(client, json),
    )
    override val socketController: List<WebSocketController> = listOf(
        LogSocket,
        ConfigSocket(client, json),
        EventSocket(client, json, logger),
        FlagListSocket(client, json),
    )
    override val staticResources: List<String> = listOf(
        "web-frontend.js",
        "web-frontend.js.map",
        "main.css",
    )
}
