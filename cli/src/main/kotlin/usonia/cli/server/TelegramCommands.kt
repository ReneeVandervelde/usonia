package usonia.cli.server

import com.github.ajalt.clikt.core.CliktCommand
import kotlinx.coroutines.runBlocking
import usonia.server.DummyClient
import usonia.telegram.TelegramBridgePlugin
import usonia.telegram.commands.Command.Visibility.Public

class TelegramCommands(): CliktCommand(
    name = "telegram-commands",
    help = "Commands for the Telegram Bridge."
) {
    override fun run() = runBlocking {
        TelegramBridgePlugin(DummyClient)
            .allCommands
            .filter { it.visibility == Public }
            .sortedBy { it.id }
            .forEach {
                echo("${it.id.trimStart('/')} - ${it.description}")
            }
    }
}
