package usonia.server.ktor

import regolith.init.Initializer
import usonia.server.ServerPlugin

object KtorServerPlugin: ServerPlugin {
    override val initializers: List<Initializer> = listOf(LibSodiumInitializer)
}
