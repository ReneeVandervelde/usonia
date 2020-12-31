package usonia.hubitat

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.serialization.SerializationModule
import usonia.server.Daemon
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient

class HubitatPlugin(
    client: BackendClient,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    override val daemons: List<Daemon> = listOf(
        ActionRelay(client, SerializationModule.json, logger),
    )
}
