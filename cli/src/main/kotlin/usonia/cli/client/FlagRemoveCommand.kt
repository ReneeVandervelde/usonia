package usonia.cli.client

import com.github.ajalt.clikt.parameters.arguments.argument

class FlagRemoveCommand(
    module: ClientModule,
): ClientCommand(
    clientModule = module,
    help = "Remove a configuration flag on a server"
) {
    private val key by argument()

    override suspend fun execute() {
        client.removeFlag(key)
    }
}
