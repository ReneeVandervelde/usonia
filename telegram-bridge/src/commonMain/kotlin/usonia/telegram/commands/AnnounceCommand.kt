package usonia.telegram.commands

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.Update
import usonia.core.client.alertAll
import usonia.foundation.Action
import usonia.foundation.User
import usonia.server.client.BackendClient

internal class AnnounceCommand(
    private val telegram: TelegramBotClient,
    private val client: BackendClient,
): Command {
    override val id = "/announce"
    override val description = "Send Announcement"
    override val visibility: Command.Visibility = Command.Visibility.Hidden

    override suspend fun onReceiveCommand(update: Update.MessageUpdate, user: User) {
        client.alertAll(
            message = update.message.text?.substringAfter("/announce")?.trim().orEmpty(),
            level = Action.Alert.Level.Info,
        )
    }
}
