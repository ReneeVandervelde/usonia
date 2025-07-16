package usonia.telegram

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.ChatType.Companion.Private
import com.inkapplications.telegram.structures.MessageEntityType
import com.inkapplications.telegram.structures.Update
import kimchi.logger.KimchiLogger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import usonia.core.client.alertAll
import usonia.core.state.getSite
import usonia.foundation.Action
import usonia.foundation.Action.Alert.Icon
import usonia.foundation.Status
import usonia.foundation.Statuses
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

        if (data.message.chat.type != Private) {
            logger.warn("Ignoring message from non-private chat: ${data.message.chat.type}")
            return RestResponse(Statuses.SUCCESS)
        }

        val commandId = data.message.entities
            ?.firstOrNull { it.type == MessageEntityType.BotCommand }
            ?.let { data.message.text?.subSequence(it.offset, it.length) }
            ?.toString()
            ?.lowercase()

        val user = client.getSite().users
            .find { data.message.chat.id.value == it.parameters[CHAT_ID_KEY]?.toLongOrNull() }
            ?: run {
                when (commandId) {
                    "/start" -> newUser(data)
                    else -> onUnknownUserCommand(data)
                }
                return RestResponse(Statuses.SUCCESS)
            }

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

    private suspend fun newUser(update: Update.MessageUpdate) {
        client.alertAll(
            message = """
                New chat started with user (@${update.message.from?.username})
                In Chat ID: `${update.message.chat.id.value}`
            """.trimIndent(),
            level = Action.Alert.Level.Debug,
        )
        telegram.sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = Icon.Wave.asSticker,
            message = """
                Welcome!
                This bot requires permissions to use. I let the admin know.
                You will not be able to use commands until you are approved.
            """.trimIndent()
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
