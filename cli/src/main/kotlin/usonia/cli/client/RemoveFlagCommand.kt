package usonia.cli.client

import com.github.ajalt.clikt.parameters.arguments.argument
import usonia.cli.CliComponent
import javax.inject.Inject

class RemoveFlagCommand @Inject constructor(
    component: CliComponent,
): ClientCommand(
    component = component,
    name = "flags:remove",
    help = "Remove a configuration flag on a server"
) {
    private val key by argument()

    override suspend fun execute() {
        client.removeFlag(key)
    }
}
