package usonia.core

import usonia.server.HttpController
import usonia.server.WebSocketController

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
