package usonia.plugins

import usonia.server.HttpController
import usonia.server.WebSocketController

data class Plugin(
    val daemons: List<Daemon> = emptyList(),
    val httpControllers: List<HttpController> = emptyList(),
    val socketController: List<WebSocketController> = emptyList(),
    val staticResources: List<String> = emptyList(),
)
