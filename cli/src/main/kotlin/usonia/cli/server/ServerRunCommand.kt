package usonia.cli.server

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking
import usonia.cli.CliComponent
import javax.inject.Inject

class ServerRunCommand @Inject constructor(
    private val component: CliComponent
): CliktCommand(
    name = "run",
    help = "Runs the main server application."
) {
    private val port by option().int().default(80)
    private val database by option().file(canBeDir = false)
    private val backendModule get() = ServerModule(
        path = database?.absolutePath,
        port = port,
    )
    private val backendComponent get() = component.serverComponent(backendModule)
    private val server get() = backendComponent.server()

    override fun run() = runBlocking {
        server.start()
    }
}
