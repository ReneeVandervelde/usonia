package usonia.telegram

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.Daemon
import usonia.core.ServerPlugin
import usonia.core.state.ActionAccess
import usonia.core.state.ConfigurationAccess

class TelegramBridgePlugin(
    actions: ActionAccess,
    config: ConfigurationAccess,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    override val daemons: List<Daemon> = listOf(
        TelegramAlerts(
            actionAccess = actions,
            configurationAccess = config,
            telegramApi = TelegramClient,
            logger = logger
        )
    )
}
