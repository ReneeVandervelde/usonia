package usonia.server.ktor

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.Daemon
import usonia.core.Plugin

class KtorServerPlugin(
    port: Int = 80,
    logger: KimchiLogger = EmptyLogger
): Plugin {
    private val server = KtorWebServer(
        port = port,
        logger = logger
    )

    override val daemons: List<Daemon> = listOf(
        server
    )
}
