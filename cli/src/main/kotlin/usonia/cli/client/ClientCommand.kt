package usonia.cli.client

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking
import usonia.cli.CliComponent

/**
 * CLI Command with access to a parameter configurable HTTP client.
 *
 * All command names will be prefixed with `client:`
 * Default Client can be configured with the `--host` and `--port` options.
 */
abstract class ClientCommand(
    private val component: CliComponent,
    name: String,
    help: String,
): CliktCommand(
    name = "client:$name",
    help = help,
) {
    private val host by option().default("localhost")
    private val port by option().int().default(80)
    private val module by lazy {
        ClientModule(
            host = host,
            port = port,
        )
    }
    private val clientComponent by lazy { component.clientComponent(module) }

    /**
     * Usonia HTTP Client.
     */
    protected val client by lazy { clientComponent.client() }

    final override fun run() = runBlocking {
        execute()
    }

    abstract suspend fun execute()
}
