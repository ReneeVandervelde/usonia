package usonia.cli.client

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import usonia.cli.CliComponent
import javax.inject.Inject

class FlagSetCommand @Inject constructor(
    component: CliComponent,
): ClientCommand(
    component = component,
    help = "Set a configuration flag on a server"
) {
    private val key by argument()
    private val value by argument().optional()

    override suspend fun execute() {
        client.setFlag(key, value)
    }
}
