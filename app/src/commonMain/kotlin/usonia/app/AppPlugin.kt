package usonia.app

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.app.alerts.WaterMonitor
import usonia.core.Daemon
import usonia.core.Plugin
import usonia.state.ActionPublisher
import usonia.state.ConfigurationAccess
import usonia.state.EventAccess

class AppPlugin(
    configurationAccess: ConfigurationAccess,
    eventAccess: EventAccess,
    actionPublisher: ActionPublisher,
    logger: KimchiLogger = EmptyLogger
): Plugin {
    override val daemons: List<Daemon> = listOf(
        WaterMonitor(configurationAccess, eventAccess, actionPublisher, logger)
    )
}
