package usonia.cli.client

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional

class FlagSetCommand(
    module: ClientModule,
): ClientCommand(
    clientModule = module,
    help = "Set a configuration flag on a server"
) {
    private val key by argument()
    private val value by argument().optional()

    override suspend fun execute() {
        client.setFlag(key, value)
    }
}
