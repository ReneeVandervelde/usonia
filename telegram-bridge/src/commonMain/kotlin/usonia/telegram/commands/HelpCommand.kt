package usonia.telegram.commands

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.ChatReference
import com.inkapplications.telegram.structures.MessageParameters
import com.inkapplications.telegram.structures.Update
import usonia.foundation.User

class HelpCommand(
    private val telegram: TelegramBotClient,
    private val commands: Set<Command>,
): Command {
    override val id: String = "/help"
    override val description: String = "List out commands."

    override suspend fun onReceiveCommand(update: Update.MessageUpdate, user: User) {
        val commandList = commands
            .filter { it.visibility == Command.Visibility.Public }
            .joinToString("\r\n") { "${it.id} - ${it.description}" }
            .trim()

        telegram.sendMessage(MessageParameters(
            chatId = update.message.chat.id,
            text = """
Here are the available commands:

$commandList
            """.trimIndent(),
        ))
    }
}
