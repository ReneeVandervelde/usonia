package usonia.cli.server

import kimchi.logger.CompositeLogWriter
import kimchi.logger.ConsolidatedLogger
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import usonia.cli.ColorWriter
import usonia.core.state.memory.InMemoryActionAccess
import usonia.rules.alerts.LogErrorAlerts
import usonia.server.UsoniaServer
import usonia.server.client.BackendClient
import usonia.server.client.ComposedBackendClient
import usonia.server.ktor.KtorWebServer
import usonia.state.DatabaseModule
import usonia.web.LogSocket

/**
 * Create instances on the backend client based on runtime parameters.
 */
class ServerModule(
    private val json: Json,
) {
    val logger: KimchiLogger = setOf(
            LogSocket,
            ColorWriter,
            LogErrorAlerts,
        )
        .let(::CompositeLogWriter)
        .let(::ConsolidatedLogger)

    fun createPluginsModule(
        client: BackendClient,
    ) = PluginsModule(client, logger)

    fun databaseBackendClient(
        path: String?,
    ): BackendClient {
        val databaseModule = DatabaseModule(json, logger)
        val database = if (path == null) databaseModule.inMemoryDatabase() else databaseModule.database(path)
        val actions = InMemoryActionAccess()

        return ComposedBackendClient(
            actionAccess = actions,
            actionPublisher = actions,
            eventAccess = database,
            eventPublisher = database,
            configurationAccess = database,
        )
    }

    fun createServer(
        port: Int,
        databasePath: String?,
    ): UsoniaServer {
        val server = KtorWebServer(port, logger)
        val client = databaseBackendClient(databasePath)
        val pluginsModule = createPluginsModule(client)

        return UsoniaServer(
            plugins = pluginsModule.plugins,
            server = server,
            logger = logger,
        )
    }
}
