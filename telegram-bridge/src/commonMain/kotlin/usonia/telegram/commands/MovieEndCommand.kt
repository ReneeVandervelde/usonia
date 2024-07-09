package usonia.telegram.commands

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.MessageParameters
import com.inkapplications.telegram.structures.Update
import usonia.core.state.setFlag
import usonia.foundation.User
import usonia.rules.Flags
import usonia.server.client.BackendClient

internal class MovieEndCommand(
    private val telegram: TelegramBotClient,
    private val client: BackendClient,
): Command {
    override val id = "/endmovie"
    override val description = "End Movie Mode"

    override suspend fun onReceiveCommand(update: Update.MessageUpdate, user: User) {
        telegram.sendMessage(
            MessageParameters(
                chatId = update.message.chat.id,
                text = "Turning the lights back on.",
            )
        )
        client.setFlag(Flags.MovieMode, false)
    }
}
