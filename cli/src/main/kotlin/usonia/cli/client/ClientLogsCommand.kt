package usonia.cli.client

import kotlinx.coroutines.flow.collect
import usonia.cli.CliComponent
import javax.inject.Inject

class ClientLogsCommand @Inject constructor(
    component: CliComponent,
): ClientCommand(
    component = component,
    name = "logs",
    help = "Listen to log statements being recorded on the server."
) {
    override suspend fun execute() {
        client.logs.collect {
            echo(it)
        }
    }
}
