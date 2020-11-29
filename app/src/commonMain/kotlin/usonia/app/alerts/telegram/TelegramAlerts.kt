package usonia.app.alerts.telegram

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import usonia.core.Daemon
import usonia.foundation.Action
import usonia.foundation.Site
import usonia.kotlin.neverEnding
import usonia.state.ActionAccess
import usonia.state.ConfigurationAccess

private const val BOT_KEY = "telegram.bot"
private const val BOT_TOKEN_KEY = "telegram.token"
private const val CHAT_ID_KEY = "telegram.chat"

/**
 * Sends alerts to users configured to use telegram.
 *
 * Requires `telegram.bot` and `telegram.token` to be configured as
 * site-level parameters, and `telegram.chat` configured on each user
 * that should receive a notification with a pre-setup bot chat ID.
 */
class TelegramAlerts(
    private val actionAccess: ActionAccess,
    private val configurationAccess: ConfigurationAccess,
    private val telegramApi: TelegramApi,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    override suspend fun start() = neverEnding {
        configurationAccess.site.collectLatest { site ->
            site.parameters[BOT_KEY] ?: logger.warn(
                "Telegram Alerts not configured. Set `$BOT_KEY` in site parameters."
            )
            site.parameters[BOT_TOKEN_KEY] ?: logger.warn(
                "Telegram Alerts not configured. Set `$BOT_TOKEN_KEY` in site parameters."
            )
            actionAccess.actions
                .filterIsInstance<Action.Alert>()
                .collect { send(site, it) }
        }
    }

    private suspend fun send(site: Site, alert: Action.Alert) {
        val bot = site.parameters[BOT_KEY] ?: return
        val token = site.parameters[BOT_TOKEN_KEY] ?: return
        val user = site.users.find { it.id == alert.target } ?: run {
            logger.warn("Unable to find user for alert: <${alert.target}>")
            return
        }
        val chatId = user.parameters[CHAT_ID_KEY] ?: run {
            logger.debug("Skipping alert for <${user.name}> with no `${CHAT_ID_KEY}` parameter.")
            return
        }
        logger.trace("Sending alerts to <${user.name}>")
        telegramApi.sendMessage(bot, token, chatId, alert.message)
    }
}
