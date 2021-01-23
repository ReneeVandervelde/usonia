package usonia.cli.server

import dagger.Module
import dagger.Provides
import dagger.Reusable
import kimchi.logger.CompositeLogWriter
import kimchi.logger.ConsolidatedLogger
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import usonia.cli.ColorWriter
import usonia.core.state.memory.InMemoryActionAccess
import usonia.core.state.memory.InMemoryEventAccess
import usonia.server.ServerPlugin
import usonia.server.UsoniaServer
import usonia.server.client.BackendClient
import usonia.server.client.ComposedBackendClient
import usonia.server.ktor.KtorWebServer
import usonia.state.DatabaseModule
import usonia.web.LogSocket

/**
 * Create instances on the backend client based on runtime parameters.
 */
@Module
class ServerModule(
    private val path: String?,
    private val port: Int,
) {
    @Provides
    @Reusable
    fun databaseBackendClient(
        json: Json,
    ): BackendClient {
        val databaseModule = DatabaseModule(json)
        val events = if (path == null) {
            InMemoryEventAccess()
        } else {
            databaseModule.database(path)
        }
        val actions = InMemoryActionAccess()
        val config = FileConfigAccess(json)

        return ComposedBackendClient(
            actionAccess = actions,
            actionPublisher = actions,
            eventAccess = events,
            eventPublisher = events,
            configurationAccess = config,
        )
    }

    @Provides
    @Reusable
    fun createServer(
        logger: KimchiLogger,
        plugins: @JvmSuppressWildcards Set<ServerPlugin>
    ): UsoniaServer {
        val server = KtorWebServer(port, logger)

        return UsoniaServer(
            plugins = plugins,
            server = server,
            logger = logger,
        )
    }

    @Provides
    @Reusable
    fun logger(): KimchiLogger {
        return setOf(LogSocket, ColorWriter)
            .let(::CompositeLogWriter)
            .let(::ConsolidatedLogger)
    }
}
