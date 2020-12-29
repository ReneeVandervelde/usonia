package usonia.web

import kimchi.logger.KimchiLogger
import usonia.core.state.ActionPublisher
import usonia.core.state.ConfigurationAccess
import usonia.core.state.EventAccess
import usonia.core.state.EventPublisher
import usonia.serialization.SerializationModule
import usonia.server.ServerPlugin
import usonia.server.http.HttpController
import usonia.server.http.WebSocketController
import usonia.web.actions.ActionBridgeHttpPublisher
import usonia.web.actions.ActionHttpPublisher
import usonia.web.config.ConfigSocket
import usonia.web.events.EventBridgeHttpPublisher
import usonia.web.events.EventHttpPublisher
import usonia.web.events.EventSocket
import usonia.web.events.LatestEventController

class WebPlugin(
    configurationAccess: ConfigurationAccess,
    eventPublisher: EventPublisher,
    eventAccess: EventAccess,
    actionPublisher: ActionPublisher,
    logger: KimchiLogger,
): ServerPlugin {
    private val json = SerializationModule.json

    override val httpControllers: List<HttpController> = listOf(
        ControlPanelController,
        EventHttpPublisher(eventPublisher, json, logger),
        EventBridgeHttpPublisher(eventPublisher, configurationAccess, json, logger),
        ActionHttpPublisher(actionPublisher, json, logger),
        ActionBridgeHttpPublisher(configurationAccess, actionPublisher, json, logger),
        LatestEventController(eventAccess, json),
    )
    override val socketController: List<WebSocketController> = listOf(
        LogSocket,
        ConfigSocket(configurationAccess, json),
        EventSocket(eventAccess, json, logger),
    )
    override val staticResources: List<String> = listOf(
        "web-frontend.js",
        "web-frontend.js.map",
        "main.css",
    )
}
