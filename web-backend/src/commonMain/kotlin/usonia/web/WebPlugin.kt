package usonia.web

import kimchi.logger.KimchiLogger
import usonia.core.ServerPlugin
import usonia.core.server.HttpController
import usonia.core.server.WebSocketController
import usonia.core.state.ActionPublisher
import usonia.core.state.ConfigurationAccess
import usonia.core.state.EventAccess
import usonia.core.state.EventPublisher
import usonia.serialization.SiteSerializer
import usonia.web.actions.ActionBridgeHttpPublisher
import usonia.web.actions.ActionHttpPublisher
import usonia.web.config.ConfigSocket
import usonia.web.events.EventBridgeHttpPublisher
import usonia.web.events.EventHttpPublisher
import usonia.web.events.EventSocket

class WebPlugin(
    configurationAccess: ConfigurationAccess,
    eventPublisher: EventPublisher,
    eventAccess: EventAccess,
    actionPublisher: ActionPublisher,
    siteSerializer: SiteSerializer,
    logger: KimchiLogger,
): ServerPlugin {
    override val httpControllers: List<HttpController> = listOf(
        ControlPanelController,
        EventHttpPublisher(eventPublisher, logger),
        EventBridgeHttpPublisher(eventPublisher, configurationAccess, logger),
        ActionHttpPublisher(actionPublisher, logger),
        ActionBridgeHttpPublisher(configurationAccess, actionPublisher, logger),
    )
    override val socketController: List<WebSocketController> = listOf(
        LogSocket,
        ConfigSocket(configurationAccess, siteSerializer),
        EventSocket(eventAccess, logger),
    )
    override val staticResources: List<String> = listOf(
        "web-frontend.js",
        "web-frontend.js.map",
        "main.css",
    )
}
