package usonia.telegram.commands

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.Update
import usonia.foundation.Action
import usonia.foundation.User
import usonia.server.client.BackendClient
import usonia.telegram.asSticker
import usonia.telegram.sendStickerWithMessage

internal class ArmCommand(
    private val telegram: TelegramBotClient,
    private val client: BackendClient,
): Command {
    override val id = "/arm"
    override val description = "Arm Security System"

    override suspend fun onReceiveCommand(update: Update.MessageUpdate, user: User) {
        telegram.sendStickerWithMessage(
            chat = update.message.chat.id,
            sticker = Action.Alert.Icon.Bot.asSticker,
            message = "Security system armed. You can disarm with /disarm",
        )
        client.armSecurity()
    }
}
