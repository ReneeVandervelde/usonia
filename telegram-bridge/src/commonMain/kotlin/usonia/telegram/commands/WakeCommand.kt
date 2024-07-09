package usonia.telegram.commands

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.Update
import usonia.core.state.setFlag
import usonia.foundation.Action.Alert.Icon
import usonia.foundation.User
import usonia.rules.Flags
import usonia.server.client.BackendClient
import usonia.telegram.asSticker
import usonia.telegram.sendStickerWithMessage

internal class WakeCommand(
    private val telegram: TelegramBotClient,
    private val client: BackendClient,
): Command {
    override val id = "/wake"
    override val description = "Exit Sleep Mode"

    override suspend fun onReceiveCommand(update: Update.MessageUpdate, user: User) {
        telegram.sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = Icon.Wake.asSticker,
            message = "Good Morning!",
        )
        client.setFlag(Flags.SleepMode, false)
    }
}

