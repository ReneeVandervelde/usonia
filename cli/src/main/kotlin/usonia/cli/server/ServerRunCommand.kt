package usonia.cli.server

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking
import java.io.File

class ServerRunCommand(
    private val serverModule: CliServerModule,
): CliktCommand(
    name = "run",
) {
    private val port by option().int().default(80)
    private val database by option().file(canBeDir = false)
    private val settings by option().file(canBeDir = false)
        .default(File("settings.db"))
    private val server get() = serverModule.createServer(
        port = port,
        databasePath = database?.absolutePath,
        settingsFile = settings,
    )

    override fun help(context: Context): String {
        return "Runs the main server application."
    }

    override fun run() = runBlocking {
        server.start()
    }
}
