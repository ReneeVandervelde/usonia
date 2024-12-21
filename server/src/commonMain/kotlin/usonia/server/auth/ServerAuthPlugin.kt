package usonia.server.auth

import kimchi.logger.KimchiLogger
import regolith.init.Initializer
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient

class ServerAuthPlugin(
    client: BackendClient,
    logger: KimchiLogger,
): ServerPlugin {
    override val initializers: List<Initializer> = listOf(LibSodiumInitializer)

    val auth: Authorization = PskAuthorization(
        client = client,
        logger = logger,
    )
}
