package usonia.telegram

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import regolith.processes.daemon.Daemon
import usonia.serialization.SerializationModule
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient
import usonia.server.http.HttpController

class TelegramBridgePlugin(
    client: BackendClient,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    private val telegram = TelegramClientProxy()
    private val json = Json(SerializationModule.json) {
        ignoreUnknownKeys = true
    }

    override val daemons: List<Daemon> = listOf(
        TelegramTokenUpdater(
            client,
            telegram,
            logger,
        ),
        TelegramAlerts(
            client = client,
            telegram = telegram,
            logger = logger,
        )
    )
    override val httpControllers: List<HttpController> = listOf(
        TelegramBot(client, telegram, json, logger),
    )
}
