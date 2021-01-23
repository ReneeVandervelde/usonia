package usonia.cli

import com.github.ajalt.clikt.core.CliktCommand
import dagger.Component
import usonia.cli.client.ClientComponent
import usonia.cli.client.ClientModule
import usonia.cli.server.ServerComponent
import usonia.cli.server.ServerModule
import javax.inject.Singleton

/**
 * Root dependency graph for all CLI commands.
 */
@Singleton
@Component(
    modules = [
        CommandModule::class,
        UsoniaModule::class,
    ]
)
interface CliComponent {
    /**
     * Get all available CLI command classes.
     */
    @JvmSuppressWildcards
    fun commands(): Set<CliktCommand>

    /**
     * Create an HTTP Client Module.
     *
     * @param clientModule Module configured with runtime parameters.
     */
    fun clientComponent(clientModule: ClientModule): ClientComponent

    /**
     * Create an Application Server Module.
     *
     * @param serverModule Module configured with runtime parameters.
     */
    fun serverComponent(serverModule: ServerModule): ServerComponent
}
