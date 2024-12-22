package usonia.server.auth

import kimchi.logger.KimchiLogger
import kotlinx.datetime.Clock
import regolith.init.Initializer
import usonia.auth.AuthModule
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient

class ServerAuthPlugin(
    client: BackendClient,
    clock: Clock,
    logger: KimchiLogger,
): ServerPlugin {
    override val initializers: List<Initializer> = listOf(
        *AuthModule.initializers.toTypedArray(),
    )

    val auth: Authorization = PskAuthorization(
        client = client,
        authTracker = AuthTracker(clock),
        logger = logger,
    )
}
