package usonia.telegram.commands

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.MessageParameters
import com.inkapplications.telegram.structures.Update
import usonia.core.state.setFlag
import usonia.foundation.User
import usonia.rules.Flags
import usonia.server.client.BackendClient

internal class DisableAlertsCommand(
    private val telegram: TelegramBotClient,
    private val client: BackendClient,
): Command {
    override val id = "/disablealerts"
    override val description = "Disable all log Alerts"

    override suspend fun onReceiveCommand(update: Update.MessageUpdate, user: User) {
        client.setFlag(Flags.LogAlerts, false)
        telegram.sendMessage(
            MessageParameters(
                update.message.chat.id,
                text = "Log Alerts are now disabled. This can be re-enabled with /enablealerts",
            )
        )
    }
}
