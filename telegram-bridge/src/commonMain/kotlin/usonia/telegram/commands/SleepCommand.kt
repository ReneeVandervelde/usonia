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

internal class SleepCommand(
    private val telegram: TelegramBotClient,
    private val client: BackendClient,
): Command {
    override val id = "/sleep"
    override val description = "Enter Sleep Mode"

    override suspend fun onReceiveCommand(update: Update.MessageUpdate, user: User) {
        telegram.sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = Action.Alert.Icon.Sleep.asSticker,
            message = "Good Night!",
        )
        client.setFlag(Flags.SleepMode, true)
    }
}
