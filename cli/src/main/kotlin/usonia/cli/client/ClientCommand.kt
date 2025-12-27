package usonia.cli.client

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.runBlocking

/**
 * CLI Command with access to a parameter configurable HTTP client.
 *
 * All command names will be prefixed with `client:`
 * Default Client can be configured with the `--host` and `--port` options.
 */
abstract class ClientCommand(
    private val clientModule: ClientModule,
    private val help: String,
): CliktCommand() {
    private val host by option().default("localhost")
    private val port by option().int().default(80)

    override fun help(context: Context): String {
        return help
    }

    /**
     * Usonia HTTP Client.
     */
    protected val client by lazy { clientModule.createClient(host, port) }

    protected val logger: KimchiLogger by lazy { clientModule.logger }

    final override fun run() = runBlocking {
        clientModule.initializer.initialize().join()
        execute()
    }

    abstract suspend fun execute()
}
