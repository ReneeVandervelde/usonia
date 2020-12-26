package usonia.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking
import usonia.cli.UsoniaFactory
import javax.inject.Inject

class RunCommand @Inject constructor(
    private val usoniaFactory: UsoniaFactory
): CliktCommand(
    name = "run",
    help = "Runs the main server application."
) {
    private val port by option().int().default(80)

    override fun run() = runBlocking {
        val usonia = usoniaFactory.createServer(port)

        usonia.start()
    }
}
