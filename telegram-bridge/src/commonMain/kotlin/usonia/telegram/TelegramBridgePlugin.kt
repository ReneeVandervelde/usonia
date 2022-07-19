package usonia.telegram

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.client.TelegramClientModule
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.server.Daemon
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient

class TelegramBridgePlugin(
    client: BackendClient,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    private val clientModule = TelegramClientModule()
    private val clientFactory = object: ClientFactory {
        override fun create(key: String, token: String): TelegramBotClient {
            return clientModule.createClient("$key:$token")
        }
    }

    override val daemons: List<Daemon> = listOf(
        TelegramAlerts(
            client = client,
            clientFactory = clientFactory,
            logger = logger
        )
    )
}
