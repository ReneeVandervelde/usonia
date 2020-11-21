package usonia.core

import usonia.core.server.HttpController
import usonia.core.server.WebSocketController

object CorePlugin: Plugin {
    override val httpControllers: List<HttpController> = listOf(
        ControlPanelController,
    )
    override val socketController: List<WebSocketController> = listOf(
        LogSocket,
    )
    override val staticResources: List<String> = listOf(
        "frontend.js"
    )
}
