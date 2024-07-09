package usonia.telegram.commands

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.MessageParameters
import com.inkapplications.telegram.structures.Update
import usonia.core.state.setFlag
import usonia.foundation.User
import usonia.rules.Flags
import usonia.server.client.BackendClient

internal class PauseLightsCommand(
    private val telegram: TelegramBotClient,
    private val client: BackendClient,
): Command {
    override val id = "/pauselights"
    override val description = "Disable ALL lighting control"
    override val visibility: Command.Visibility = Command.Visibility.Hidden

    override suspend fun onReceiveCommand(update: Update.MessageUpdate, user: User) {
        telegram.sendMessage(
            MessageParameters(
                chatId = update.message.chat.id,
                text = "Okay. I'll stop changing any lights until this is resumed with /resumelights.",
            )
        )
        client.setFlag(Flags.MotionLights, false)
    }
}
