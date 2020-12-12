package usonia.rules

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.rules.alerts.WaterMonitor
import usonia.core.Daemon
import usonia.core.Plugin
import usonia.core.state.ActionAccess
import usonia.core.state.ActionPublisher
import usonia.core.state.ConfigurationAccess
import usonia.core.state.EventAccess

class RulesPlugin(
    configurationAccess: ConfigurationAccess,
    eventAccess: EventAccess,
    actionPublisher: ActionPublisher,
    actionAccess: ActionAccess,
    logger: KimchiLogger = EmptyLogger,
): Plugin {
    override val daemons: List<Daemon> = listOf(
        WaterMonitor(configurationAccess, eventAccess, actionPublisher, logger),
    )
}
