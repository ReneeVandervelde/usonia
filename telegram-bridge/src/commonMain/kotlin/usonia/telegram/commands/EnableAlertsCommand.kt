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

internal class EnableAlertsCommand(
    private val telegram: TelegramBotClient,
    private val client: BackendClient,
): Command {
    override val id = "/enablealerts"
    override val description = "Enable log Alerts"
    override val visibility: Command.Visibility = Command.Visibility.Hidden

    override suspend fun onReceiveCommand(update: Update.MessageUpdate, user: User) {
        client.setFlag(Flags.LogAlerts, true)
        telegram.sendStickerWithMessage(
            update.message.chat.id,
            sticker = Action.Alert.Icon.Bot.asSticker,
            message = "Log Alerts are now enabled!",
        )
    }
}
