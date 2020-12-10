package usonia.rules

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.rules.alerts.WaterMonitor
import usonia.core.Daemon
import usonia.core.Plugin
import usonia.state.ActionAccess
import usonia.state.ActionPublisher
import usonia.state.ConfigurationAccess
import usonia.state.EventAccess

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
