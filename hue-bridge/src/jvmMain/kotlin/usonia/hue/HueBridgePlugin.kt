package usonia.hue

import inkapplications.shade.core.Shade
import inkapplications.shade.core.events
import inkapplications.shade.structures.UndocumentedApi
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.kotlin.IoScope
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient

@OptIn(UndocumentedApi::class)
class HueBridgePlugin(
    client: BackendClient,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    private val shade = Shade(
        configuration = LiveConfigContainer(
            client,
            IoScope(),
            logger,
        )
    )

    override val daemons = listOf(
        HueGroupHandler(client, shade.groupedLights, logger),
        HueLightHandler(client, shade.lights, logger),
        HueEventPublisher(shade.events, client, client),
    )
}
