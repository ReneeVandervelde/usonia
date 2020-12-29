package usonia.hubitat

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.state.*
import usonia.serialization.SerializationModule
import usonia.server.Daemon
import usonia.server.ServerPlugin

class HubitatPlugin(
    actionAccess: ActionAccess,
    configurationAccess: ConfigurationAccess,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    override val daemons: List<Daemon> = listOf(
        ActionRelay(configurationAccess, actionAccess, SerializationModule.json, logger),
    )
}
