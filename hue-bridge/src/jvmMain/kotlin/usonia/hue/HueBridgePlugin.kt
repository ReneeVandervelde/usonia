package usonia.hue

import inkapplications.shade.Shade
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient

class HueBridgePlugin(
    client: BackendClient,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    private val shade = Shade(
        appId = "usonia#bridge",
        storage = ConfigurationTokenStorage(client, logger),
    )
    override val daemons = listOf(
        HueGroupHandler(client, shade.groups, logger),
        HueLightHandler(client, shade.lights, logger),
        ShadeConfigManager(client, shade),
    )
}
