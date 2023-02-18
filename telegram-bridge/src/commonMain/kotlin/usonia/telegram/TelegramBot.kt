package usonia.telegram

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.*
import kimchi.logger.KimchiLogger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import usonia.core.client.alertAll
import usonia.core.state.getSite
import usonia.core.state.setFlag
import usonia.foundation.Action
import usonia.foundation.Action.Alert.Icon
import usonia.foundation.Status
import usonia.foundation.Statuses
import usonia.rules.Flags
import usonia.server.client.BackendClient
import usonia.server.http.HttpRequest
import usonia.server.http.RestController
import usonia.server.http.RestResponse

private const val USERNAME_KEY = "telegram.username"

internal class TelegramBot(
    private val client: BackendClient,
    private val telegram: TelegramBotClient,
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

        client.getSite().users.find { data.message.chat.id.value == it.parameters[CHAT_ID_KEY]?.toLongOrNull() } ?: run {
            onUnknownUserCommand(data)
            return RestResponse(Statuses.SUCCESS)
        }

        val command = data.message.entities
            ?.firstOrNull { it.type == MessageEntityType.BotCommand }
            ?.let { data.message.text?.subSequence(it.offset, it.length) }
            ?.toString()
            ?.lowercase()

        when (command) {
            "/wake" -> onWakeCommand(data)
            "/sleep" -> onSleepCommand(data)
            "/startmovie" -> onMovieStart(data)
            "/endmovie" -> onMovieEnd(data)
            "/pauselights" -> onPauseLights(data)
            "/resumelights" -> onResumeLights(data)
            "/announce" -> onAnnounce(data)
            else -> onUnknownCommand(data)
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

    private suspend fun onWakeCommand(update: Update.MessageUpdate) {
        telegram.sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = Icon.Wake.asSticker,
            message = "Good Morning!",
        )
        client.setFlag(Flags.SleepMode, false)
    }

    private suspend fun onSleepCommand(update: Update.MessageUpdate) {
        telegram.sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = Icon.Sleep.asSticker,
            message = "Good Night!",
        )
        client.setFlag(Flags.SleepMode, true)
    }

    private suspend fun onMovieStart(update: Update.MessageUpdate) {
        telegram.sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = Icon.Entertained.asSticker,
            message = "Enjoy the film!\nJust send /endmovie when it's done.",
        )
        client.setFlag(Flags.MovieMode, true)
    }

    private suspend fun onMovieEnd(update: Update.MessageUpdate) {
        telegram.sendMessage(MessageParameters(
            chatId = update.message.chat.id,
            text = "Turning the lights back on.",
        ))
        client.setFlag(Flags.MovieMode, false)
    }

    private suspend fun onPauseLights(update: Update.MessageUpdate) {
        telegram.sendMessage(MessageParameters(
            chatId = update.message.chat.id,
            text = "Okay. I'll stop changing any lights until this is resumed with /resumelights.",
        ))
        client.setFlag(Flags.MotionLights, false)
    }

    private suspend fun onResumeLights(update: Update.MessageUpdate) {
        telegram.sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = Icon.Bot.asSticker,
            message = "Turning the lights back on.",
        )
        client.setFlag(Flags.MotionLights, true)
    }

    private suspend fun onUnknownCommand(update: Update.MessageUpdate) {
        telegram.sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = Icon.Confused.asSticker,
            message = "Please Send a command with the menu",
        )
    }

    private suspend fun onAnnounce(update: Update.MessageUpdate) {
        client.alertAll(
            message = update.message.text?.substringAfter("/announce")?.trim().orEmpty(),
            level = Action.Alert.Level.Info,
        )
    }
}
