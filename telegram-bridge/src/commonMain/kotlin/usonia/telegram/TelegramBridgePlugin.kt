package usonia.telegram

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.server.Daemon
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient

class TelegramBridgePlugin(
    client: BackendClient,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    override val daemons: List<Daemon> = listOf(
        TelegramAlerts(
            client = client,
            telegramApi = TelegramClient,
            logger = logger
        )
    )
}
