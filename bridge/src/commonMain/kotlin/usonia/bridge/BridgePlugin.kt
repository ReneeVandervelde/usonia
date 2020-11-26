package usonia.bridge

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.Plugin
import usonia.core.server.HttpController
import usonia.core.server.WebSocketController
import usonia.state.EventAccess
import usonia.state.EventPublisher

class BridgePlugin(
    eventPublisher: EventPublisher,
    eventAccess: EventAccess,
    logger: KimchiLogger = EmptyLogger
): Plugin {
    override val httpControllers: List<HttpController> = listOf(
        EventPublishHttpBridge(eventPublisher, logger)
    )
    override val socketController: List<WebSocketController> = listOf(
        EventSocket(eventAccess, logger)
    )
}
