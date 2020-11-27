package usonia.bridge

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.Daemon
import usonia.core.Plugin
import usonia.core.server.HttpController
import usonia.core.server.WebSocketController
import usonia.state.*

class BridgePlugin(
    eventPublisher: EventPublisher,
    eventAccess: EventAccess,
    actionAccess: ActionAccess,
    actionPublisher: ActionPublisher,
    configurationAccess: ConfigurationAccess,
    logger: KimchiLogger = EmptyLogger,
): Plugin {
    override val httpControllers: List<HttpController> = listOf(
        EventPublishHttpBridge(eventPublisher, logger),
        ActionPublishHttpBridge(actionPublisher, logger),
    )
    override val socketController: List<WebSocketController> = listOf(
        EventSocket(eventAccess, logger),
    )
    override val daemons: List<Daemon> = listOf(
        ActionRelay(configurationAccess, actionAccess, logger),
    )
}
