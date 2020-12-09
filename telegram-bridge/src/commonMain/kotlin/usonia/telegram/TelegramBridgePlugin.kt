package usonia.telegram

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.Daemon
import usonia.core.Plugin
import usonia.state.ActionAccess
import usonia.state.ConfigurationAccess

class TelegramBridgePlugin(
    actions: ActionAccess,
    config: ConfigurationAccess,
    logger: KimchiLogger = EmptyLogger,
): Plugin {
    override val daemons: List<Daemon> = listOf(
        TelegramAlerts(
            actionAccess = actions,
            configurationAccess = config,
            telegramApi = TelegramClient,
            logger = logger
        )
    )
}
