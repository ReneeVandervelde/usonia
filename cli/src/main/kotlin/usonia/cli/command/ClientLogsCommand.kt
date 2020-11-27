package usonia.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import usonia.client.UsoniaClient
import javax.inject.Inject

class ClientLogsCommand @Inject constructor(): CliktCommand(
    name = "client:logs",
    help = "Listen to log statements being recorded on the server."
) {
    private val host by option().default("localhost")
    private val port by option().int().default(80)

    override fun run() = runBlocking {
        val client = UsoniaClient(
            host = host,
            port = port
        )

        client.logs.collect {
            echo(it)
        }
    }
}
