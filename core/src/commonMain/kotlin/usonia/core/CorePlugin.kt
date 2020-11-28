package usonia.core

import usonia.core.server.HttpController
import usonia.core.server.WebSocketController
import usonia.state.ConfigurationAccess

class CorePlugin(
    private val configurationAccess: ConfigurationAccess,
): Plugin {
    override val httpControllers: List<HttpController> = listOf(
        ControlPanelController,
    )
    override val socketController: List<WebSocketController> = listOf(
        LogSocket,
        ConfigSocket(configurationAccess),
    )
    override val staticResources: List<String> = listOf(
        "frontend.js",
        "frontend.js.map",
    )
}
