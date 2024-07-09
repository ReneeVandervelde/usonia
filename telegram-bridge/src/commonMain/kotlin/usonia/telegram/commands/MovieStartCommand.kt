package usonia.telegram.commands

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.Update
import usonia.core.state.setFlag
import usonia.foundation.Action
import usonia.foundation.User
import usonia.rules.Flags
import usonia.server.client.BackendClient
import usonia.telegram.asSticker
import usonia.telegram.sendStickerWithMessage

internal class MovieStartCommand(
    private val telegram: TelegramBotClient,
    private val client: BackendClient,
): Command {
    override val id = "/startmovie"
    override val description = "Start Movie Mode"

    override suspend fun onReceiveCommand(update: Update.MessageUpdate, user: User) {
        telegram.sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = Action.Alert.Icon.Entertained.asSticker,
            message = "Enjoy the film!\nJust send /endmovie when it's done.",
        )
        client.setFlag(Flags.MovieMode, true)
    }
}
