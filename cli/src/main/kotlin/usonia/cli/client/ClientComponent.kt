package usonia.cli.client

import dagger.Subcomponent
import usonia.client.FrontendClient

/**
 * Dependency graph for a configured HTTP Client.
 *
 * This remains a subcomponent so that it can be constructed separately
 * from the command graph, based on the command's provided runtime configuration.
 */
@Subcomponent(
    modules = [
        ClientModule::class,
    ]
)
@ClientScope
interface ClientComponent {
    fun client(): FrontendClient
}
