package usonia.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kimchi.logger.CompositeLogWriter
import kimchi.logger.ConsolidatedLogger
import kotlinx.coroutines.runBlocking
import usonia.cli.ColorWriter
import usonia.core.CorePlugin
import usonia.core.LogSocket
import usonia.core.Usonia
import usonia.server.ktor.KtorServerPlugin

class RunCommand: CliktCommand(
    name = "run"
) {
    private val port by option().int().default(80)

    override fun run() = runBlocking {
        val logger = ConsolidatedLogger(
            CompositeLogWriter(setOf(
                LogSocket,
                ColorWriter
            ))
        )

        val usonia = Usonia(
            plugins = setOf(
                CorePlugin,
                KtorServerPlugin(
                    port = port,
                    logger = logger
                )
            ),
            logger = logger
        )

        usonia.start()
    }
}
