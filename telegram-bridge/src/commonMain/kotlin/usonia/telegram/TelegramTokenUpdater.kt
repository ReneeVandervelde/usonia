package usonia.telegram

import com.inkapplications.telegram.client.TelegramClientModule
import kimchi.logger.KimchiLogger
import regolith.processes.daemon.Daemon
import usonia.foundation.Site
import usonia.kotlin.collectLatest
import usonia.server.client.BackendClient

private const val BOT_KEY = "bot"
private const val BOT_TOKEN = "token"
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

    private fun onSiteUpdate(site: Site) {
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

        proxy.delegate = clientModule.createClient("$bot:$token")
    }
}
