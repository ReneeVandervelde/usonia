package usonia.telegram.commands

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.ChatReference
import com.inkapplications.telegram.structures.MessageParameters
import com.inkapplications.telegram.structures.Update
import usonia.foundation.User

class WelcomeCommand(
    private val telegram: TelegramBotClient,
    private val commands: Set<Command>,
): Command {
    override val id: String = "/welcome"
    override val description: String = "Welcome Message"
    override val visibility: Command.Visibility = Command.Visibility.Hidden

    override suspend fun onReceiveCommand(update: Update.MessageUpdate, user: User) {
        val chatId = update.message.text
            ?.substringAfter("/welcome")
            ?.trim()
            ?.takeUnless { ' ' in it }
            ?.takeUnless { it.startsWith('@') }
            ?.toLongOrNull()
            ?.let { ChatReference.Id(it) }
            ?: run {
                telegram.sendMessage(
                    MessageParameters(
                        chatId = update.message.chat.id,
                        text = "Invalid Chat ID to send welcome message to!"
                    )
                )
                return
            }

        val commandList = commands
            .filter { it.visibility == Command.Visibility.Public }
            .joinToString("\n") { "${it.id} - ${it.description}" }
            .trim()

        telegram.sendMessage(MessageParameters(
            chatId = chatId,
            text = """
Your account is now set up!
I'll send you notifications and alerts as they happen.

Here's a list of commands you can use:

$commandList

These commands are also available in the menu.
You can also see them again by typing /help
            """.trimIndent(),
        ))
    }
}
