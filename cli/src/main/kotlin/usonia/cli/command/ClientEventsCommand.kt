package usonia.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import usonia.cli.UsoniaFactory
import javax.inject.Inject

class ClientEventsCommand @Inject constructor(
    private val factory: UsoniaFactory,
): CliktCommand(
    name = "client:events",
    help = "Listen to the pipeline of events occurring on the server."
) {
    private val host by option().default("localhost")
    private val port by option().int().default(80)

    override fun run() = runBlocking {
        val client = factory.createClient(
            host = host,
            port = port
        )

        client.events.collect {
            echo(it)
        }
    }
}
