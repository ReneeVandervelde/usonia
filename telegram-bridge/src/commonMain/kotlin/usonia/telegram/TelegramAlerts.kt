package usonia.telegram

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.ChatReference
import com.inkapplications.telegram.structures.InputFile
import com.inkapplications.telegram.structures.MessageParameters
import com.inkapplications.telegram.structures.StickerParameters
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import usonia.foundation.Action
import usonia.foundation.Site
import usonia.kotlin.IoScope
import usonia.kotlin.collect
import usonia.kotlin.collectLatest
import usonia.kotlin.filterIsInstance
import usonia.server.Daemon
import usonia.server.client.BackendClient

/**
 * Sends alerts to users configured to use telegram.
 *
 * Requires `telegram.bot` and `telegram.token` to be configured as
 * site-level parameters, and `telegram.chat` configured on each user
 * that should receive a notification with a pre-setup bot chat ID.
 */
internal class TelegramAlerts(
    private val client: BackendClient,
    private val telegram: TelegramBotClient,
    private val logger: KimchiLogger = EmptyLogger,
    private val requestScope: CoroutineScope = IoScope()
): Daemon {
    override suspend fun start(): Nothing {
        client.site.collectLatest { site ->
            client.actions.filterIsInstance<Action.Alert>().collect {
                send(site, it)
            }
        }
    }

    private fun send(site: Site, alert: Action.Alert) {
        val user = site.users.find { it.id == alert.target } ?: run {
            logger.warn("Unable to find user for alert: <${alert.target}>")
            return
        }
        val chatId = user.parameters[CHAT_ID_KEY]?.toLongOrNull() ?: run {
            logger.debug("Skipping alert for <${user.name}> with no `${CHAT_ID_KEY}` parameter.")
            return
        }
        logger.trace("Sending alerts to <${user.name}>")
        requestScope.launch {
            val stickerId = alert.icon?.asSticker
            if (stickerId != null) {
                telegram.sendSticker(StickerParameters(
                    chatId = ChatReference.Id(chatId),
                    sticker = InputFile.FileId(stickerId),
                ))
            }
            telegram.sendMessage(MessageParameters(
                chatId = ChatReference.Id(chatId),
                text = alert.message,
            ))
        }
    }
}
