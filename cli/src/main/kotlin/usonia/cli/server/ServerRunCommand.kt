package usonia.cli.server

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking

class ServerRunCommand(
    private val serverModule: ServerModule,
): CliktCommand(
    name = "run",
    help = "Runs the main server application."
) {
    private val port by option().int().default(80)
    private val database by option().file(canBeDir = false)
    private val server get() = serverModule.createServer(
        port = port,
        databasePath = database?.absolutePath,
    )

    override fun run() = runBlocking {
        server.start()
    }
}
