package usonia.web

import usonia.core.Plugin
import usonia.core.server.HttpController
import usonia.core.server.WebSocketController
import usonia.core.state.ConfigurationAccess
import usonia.serialization.SiteSerializer

class WebPlugin(
    configurationAccess: ConfigurationAccess,
    siteSerializer: SiteSerializer,
): Plugin {
    override val httpControllers: List<HttpController> = listOf(
        ControlPanelController,
    )
    override val socketController: List<WebSocketController> = listOf(
        LogSocket,
        ConfigSocket(configurationAccess, siteSerializer),
    )
    override val staticResources: List<String> = listOf(
        "web-frontend.js",
        "web-frontend.js.map",
    )
}
