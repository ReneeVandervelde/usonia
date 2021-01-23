package usonia.cli.server

import dagger.Subcomponent
import usonia.server.UsoniaServer

/**
 * Dependency graph for a configured application server.
 *
 * This remains a subcomponent so that it can be constructed separately
 * from the command graph, based on the command's provided runtime configuration.
 */
@ServerScope
@Subcomponent(
    modules = [
        ServerModule::class,
        PluginsModule::class,
    ]
)
interface ServerComponent {
    fun server(): UsoniaServer
}


