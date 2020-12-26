package usonia.hubitat

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.Daemon
import usonia.core.ServerPlugin
import usonia.core.state.*
import usonia.serialization.SerializationModule

class HubitatPlugin(
    actionAccess: ActionAccess,
    configurationAccess: ConfigurationAccess,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    override val daemons: List<Daemon> = listOf(
        ActionRelay(configurationAccess, actionAccess, SerializationModule.json, logger),
    )
}
