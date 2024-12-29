package usonia.cli.client

import com.inkapplications.coroutines.ongoing.collect

class EventsListenCommand(
    module: ClientModule,
): ClientCommand(
    clientModule = module,
    help = "Listen to the pipeline of events occurring on the server."
) {

    override suspend fun execute() {
        client.events.collect {
            echo(it)
        }
    }
}
