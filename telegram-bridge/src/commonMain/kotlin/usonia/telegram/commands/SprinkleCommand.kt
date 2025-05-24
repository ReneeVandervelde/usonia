package usonia.telegram.commands

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.Update
import usonia.foundation.Action
import usonia.foundation.User
import usonia.server.client.BackendClient
import usonia.telegram.asSticker
import usonia.telegram.sendStickerWithMessage

internal class SprinkleCommand(
    private val telegram: TelegramBotClient,
    private val client: BackendClient,
): Command {
    override val id = "/sprinkle"
    override val description = "Run plant irrigation"

    override suspend fun onReceiveCommand(update: Update.MessageUpdate, user: User) {
        client.publishAction(
            Action.Intent(
                target = user.id,
                action = "usonia.rules.greenhouse.SprinklerControl.sprinkle",
            )
        )
        telegram.sendStickerWithMessage(
            update.message.chat.id,
            sticker = Action.Alert.Icon.Bot.asSticker,
            message = "Watering the plants! I'll let you know when it's done"
        )
    }
}
