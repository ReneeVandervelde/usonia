package usonia.telegram

import com.inkapplications.coroutines.ongoing.collectLatest
import com.inkapplications.telegram.client.TelegramClientModule
import com.inkapplications.telegram.structures.WebhookParameters
import kimchi.logger.KimchiLogger
import regolith.processes.daemon.Daemon
import usonia.foundation.Site
import usonia.server.client.BackendClient

private const val BOT_KEY = "bot"
private const val BOT_TOKEN = "token"
private const val BOT_CALLBACK = "callback"
const val CHAT_ID_KEY = "telegram.chat"

internal class TelegramTokenUpdater(
    private val client: BackendClient,
    private val proxy: TelegramClientProxy,
    private val logger: KimchiLogger,
): Daemon {
    private val clientModule = TelegramClientModule()

    override suspend fun startDaemon(): Nothing {
        client.site.collectLatest(::onSiteUpdate)
    }

    private suspend fun onSiteUpdate(site: Site) {
        val bridge = site.bridges.singleOrNull { it.service == "telegram" } ?: run {
            logger.warn("Telegram not configured. Not enabling alerts.")
            return
        }

        val bot = bridge.parameters[BOT_KEY] ?: run {
            logger.error("Telegram bridge config does not contain a bot key. Set it in parameters: <$BOT_KEY>")
            return
        }

        val token = bridge.parameters[BOT_TOKEN] ?: run {
            logger.error("Telegram bridge config does not contain a bot token. Set it in parameters: <$BOT_TOKEN>")
            return
        }

        val callback = bridge.parameters[BOT_CALLBACK] ?: run {
            logger.error("Telegram bridge config does not contain a callback. Set it in parameters: <$BOT_CALLBACK>")
            return
        }

        proxy.delegate = clientModule.createClient("$bot:$token").also {
            it.setWebhook(WebhookParameters(
                url = callback,
            ))
        }
    }
}
