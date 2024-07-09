package usonia.telegram.commands

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.Update
import kotlinx.datetime.Clock
import usonia.foundation.Action
import usonia.foundation.Event
import usonia.foundation.PresenceState
import usonia.foundation.User
import usonia.server.client.BackendClient
import usonia.telegram.asSticker
import usonia.telegram.sendStickerWithMessage

internal class AwayCommand(
    private val telegram: TelegramBotClient,
    private val client: BackendClient,
    private val clock: Clock = Clock.System,
): Command {
    override val id = "/away"
    override val description = "Set self as Away"
    override val visibility: Command.Visibility = Command.Visibility.Hidden

    override suspend fun onReceiveCommand(update: Update.MessageUpdate, user: User) {
        client.publishEvent(
            Event.Presence(
                source = user.id,
                timestamp = clock.now(),
                state = PresenceState.AWAY,
            )
        )
        telegram.sendStickerWithMessage(
            update.message.chat.id,
            sticker = Action.Alert.Icon.Wave.asSticker,
            message = "See you later!"
        )
    }
}
