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
import usonia.foundation.Status
import usonia.foundation.Statuses
import usonia.rules.Flags
import usonia.server.client.BackendClient
import usonia.server.http.*

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
            else -> onUnknownCommand(data)
        }

        return RestResponse(Statuses.SUCCESS)
    }

    private suspend fun onUnknownUserCommand(update: Update.MessageUpdate) {
        client.alertAll(
            message = "An unknown telegram user (@${update.message.from?.username}) Sent the following message:\n ${update.message.text}",
            level = Action.Alert.Level.Info
        )
        sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = "CAACAgEAAxkBAAEFUYli123oWYOwJkPILV-i-7lQjAfJQAACjwAD1guxBDOYraKGvkauKQQ",
            message = "You don't have permission to use this bot! I let the admin know.",
        )
    }

    private suspend fun onWakeCommand(update: Update.MessageUpdate) {
        sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = "CAACAgEAAxkBAAEFUYdi123JYJY3EtPnHz731Pyjk6290AACKwAD1guxBOSLzZg9iCiqKQQ",
            message = "Good Morning!",
        )
        client.setFlag(Flags.SleepMode, false)
    }

    private suspend fun onSleepCommand(update: Update.MessageUpdate) {
        sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = "CAACAgMAAxkBAAEFUZpi13Ia23J8vJehFoRmtDgE5O_e3gACFgEAAmL1YwSXaKYAAS3ha_EpBA",
            message = "Good Night!",
        )
        client.setFlag(Flags.SleepMode, true)
    }

    private suspend fun onMovieStart(update: Update.MessageUpdate) {
        sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = "CAACAgEAAxkBAAEFUYVi123Gaoef9EtimUJG32PgulHQuQACRwAD1guxBJjGVxH7DFlbKQQ",
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
        sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = "CAACAgEAAxkBAAEFU9Zi2Ijag5aMaiUN0LXzEOPcjhKJRgACHQEAAoTlaEYYrhoHPNOJzikE",
            message = "Turning the lights back on.",
        )
        client.setFlag(Flags.MotionLights, true)
    }

    private suspend fun onUnknownCommand(update: Update.MessageUpdate) {
        sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = "CAACAgEAAxkBAAEFUZxi13LNg36F6WaLBcbVSqHVPUd-JwACSwAD1guxBLtGTF8kC7YZKQQ",
            message = "Please Send a command with the menu",
        )
    }

    private suspend fun sendStickerWithMessage(
        chat: ChatReference,
        sticker: String,
        message: String,
    ) {
        telegram.sendSticker(StickerParameters(
            chatId = chat,
            sticker = InputFile.FileId(sticker),
        ))
        telegram.sendMessage(MessageParameters(
            chatId = chat,
            text = message,
        ))
    }
}
