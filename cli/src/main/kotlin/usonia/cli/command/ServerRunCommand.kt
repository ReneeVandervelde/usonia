package usonia.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import kotlinx.coroutines.runBlocking
import usonia.server.HttpServer
import javax.inject.Inject

class ServerRunCommand @Inject constructor(
    private val server: HttpServer
): CliktCommand(
    name = "server:run"
) {
    override fun run() = runBlocking {
        server.run()
    }
}
