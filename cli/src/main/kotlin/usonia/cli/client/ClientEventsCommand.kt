package usonia.cli.client

import kotlinx.coroutines.flow.collect
import usonia.cli.CliComponent
import javax.inject.Inject

class ClientEventsCommand @Inject constructor(
    component: CliComponent,
): ClientCommand(
    component = component,
    name = "events",
    help = "Listen to the pipeline of events occurring on the server."
) {

    override suspend fun execute() {
        client.events.collect {
            echo(it)
        }
    }
}
