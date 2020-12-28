package usonia.telegram

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import usonia.core.Daemon
import usonia.core.state.ActionAccess
import usonia.core.state.ConfigurationAccess
import usonia.foundation.Action
import usonia.foundation.Site
import usonia.kotlin.IoScope
import usonia.kotlin.neverEnding

private const val BOT_KEY = "bot"
private const val BOT_TOKEN = "token"
private const val CHAT_ID_KEY = "telegram.chat"

/**
 * Sends alerts to users configured to use telegram.
 *
 * Requires `telegram.bot` and `telegram.token` to be configured as
 * site-level parameters, and `telegram.chat` configured on each user
 * that should receive a notification with a pre-setup bot chat ID.
 */
internal class TelegramAlerts(
    private val actionAccess: ActionAccess,
    private val configurationAccess: ConfigurationAccess,
    private val telegramApi: TelegramApi,
    private val logger: KimchiLogger = EmptyLogger,
    private val requestScope: CoroutineScope = IoScope()
): Daemon {
    override suspend fun start() = neverEnding {
        configurationAccess.site.collectLatest { site -> onSiteUpdate(site) }
    }

    private suspend fun onSiteUpdate(site: Site) {
        val bridge = site.bridges.singleOrNull { it.service == "telegram" } ?: run {
            logger.warn("Telegram not configured. Not enabling alerts.")
            return
        }

        val bot = bridge.parameters[BOT_KEY] ?: run {
            logger.error("Telegram bridge config does not contain a bot key. Set it in parameters: <${BOT_KEY}>")
            return
        }

        val token = bridge.parameters[BOT_TOKEN] ?: run {
            logger.error("Telegram bridge config does not contain a bot token. Set it in parameters: <${BOT_TOKEN}>")
            return
        }

        actionAccess.actions.filterIsInstance<Action.Alert>().collect {
            send(bot, token, site, it)
        }
    }

    private fun send(bot: String, token: String, site: Site, alert: Action.Alert) {
        val user = site.users.find { it.id == alert.target } ?: run {
            logger.warn("Unable to find user for alert: <${alert.target}>")
            return
        }
        val chatId = user.parameters[CHAT_ID_KEY] ?: run {
            logger.debug("Skipping alert for <${user.name}> with no `${CHAT_ID_KEY}` parameter.")
            return
        }
        logger.trace("Sending alerts to <${user.name}>")
        requestScope.launch { telegramApi.sendMessage(bot, token, chatId, alert.message) }
    }
}
