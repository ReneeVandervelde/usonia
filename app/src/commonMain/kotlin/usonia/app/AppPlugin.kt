package usonia.app

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.app.alerts.WaterMonitor
import usonia.app.alerts.telegram.TelegramAlerts
import usonia.app.alerts.telegram.TelegramClient
import usonia.core.Daemon
import usonia.core.Plugin
import usonia.state.ActionAccess
import usonia.state.ActionPublisher
import usonia.state.ConfigurationAccess
import usonia.state.EventAccess

class AppPlugin(
    configurationAccess: ConfigurationAccess,
    eventAccess: EventAccess,
    actionPublisher: ActionPublisher,
    actionAccess: ActionAccess,
    logger: KimchiLogger = EmptyLogger,
): Plugin {
    override val daemons: List<Daemon> = listOf(
        WaterMonitor(configurationAccess, eventAccess, actionPublisher, logger),
        TelegramAlerts(actionAccess, configurationAccess, TelegramClient, logger),
    )
}
