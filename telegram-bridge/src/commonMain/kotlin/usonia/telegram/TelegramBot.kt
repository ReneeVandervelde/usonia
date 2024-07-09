package usonia.telegram

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.*
import kimchi.logger.KimchiLogger
import kotlinx.datetime.Clock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import usonia.core.client.alertAll
import usonia.core.state.getSite
import usonia.foundation.*
import usonia.foundation.Action.Alert.Icon
import usonia.server.client.BackendClient
import usonia.server.http.HttpRequest
import usonia.server.http.RestController
import usonia.server.http.RestResponse
import usonia.telegram.commands.Command

internal class TelegramBot(
    private val client: BackendClient,
    private val telegram: TelegramBotClient,
    private val commands: Set<Command>,
    json: Json,
    logger: KimchiLogger,
): RestController<Update, Status>(json, logger) {
    override val path: String = "/telegram-bridge"
    override val method: String = "POST"
    override val serializer: KSerializer<Status> = Status.serializer()
    override val deserializer: KSerializer<Update> = Update.serializer()

    override suspend fun getResponse(data: Update, request: HttpRequest): RestResponse<Status> {
        if (data !is Update.MessageUpdate) {
            logger.warn("Ignoring unhandled update type: ${data::class.simpleName}")
            return RestResponse(Statuses.SUCCESS)
        }

        val user = client.getSite().users
            .find { data.message.chat.id.value == it.parameters[CHAT_ID_KEY]?.toLongOrNull() }
            ?: run {
                onUnknownUserCommand(data)
                return RestResponse(Statuses.SUCCESS)
            }

        val commandId = data.message.entities
            ?.firstOrNull { it.type == MessageEntityType.BotCommand }
            ?.let { data.message.text?.subSequence(it.offset, it.length) }
            ?.toString()
            ?.lowercase()

        val command = commands.find { it.id == commandId }

        when (command) {
            null -> onUnknownCommand(data)
            else -> command.onReceiveCommand(data, user)
        }

        return RestResponse(Statuses.SUCCESS)
    }

    private suspend fun onUnknownUserCommand(update: Update.MessageUpdate) {
        client.alertAll(
            message = """
                An unknown telegram user (@${update.message.from?.username})
                In Chat ID: `${update.message.chat.id.value}`
                Sent the following message:
                --------
                ${update.message.text}
                """.trimIndent(),
            level = Action.Alert.Level.Debug
        )
        telegram.sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = Icon.Disallowed.asSticker,
            message = "You don't have permission to use this bot! I let the admin know.",
        )
    }

    private suspend fun onUnknownCommand(update: Update.MessageUpdate) {
        telegram.sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = Icon.Confused.asSticker,
            message = "Please Send a command with the menu",
        )
    }
}
