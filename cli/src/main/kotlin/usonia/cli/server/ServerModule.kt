package usonia.cli.server

import kimchi.logger.CompositeLogWriter
import kimchi.logger.ConsolidatedLogger
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import usonia.cli.ColorWriter
import usonia.core.state.memory.InMemoryActionAccess
import usonia.kotlin.datetime.ZonedClock
import usonia.kotlin.datetime.ZonedSystemClock
import usonia.rules.alerts.LogErrorAlerts
import usonia.server.UsoniaServer
import usonia.server.client.BackendClient
import usonia.server.client.ComposedBackendClient
import usonia.server.ktor.KtorWebServer
import usonia.state.DatabaseModule
import usonia.web.LogSocket
import java.io.File

/**
 * Create instances on the backend client based on runtime parameters.
 */
class ServerModule(
    private val json: Json,
    private val clock: ZonedClock = ZonedSystemClock,
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
    ) = PluginsModule(client, logger, json, clock)

    fun databaseBackendClient(
        databasePath: String?,
        settingsFile: File,
    ): BackendClient {
        val databaseModule = DatabaseModule(settingsFile, json, logger)
        val database = if (databasePath == null) databaseModule.inMemoryDatabase() else databaseModule.database(databasePath)
        val actions = InMemoryActionAccess()

        return ComposedBackendClient(
            actionAccess = actions,
            actionPublisher = actions,
            eventAccess = database,
            eventPublisher = database,
            configurationAccess = database,
            fullSecurityAccess = database,
        )
    }

    fun createServer(
        port: Int,
        settingsFile: File,
        databasePath: String?,
    ): UsoniaServer {
        val server = KtorWebServer(port, logger)
        val client = databaseBackendClient(databasePath, settingsFile)
        val pluginsModule = createPluginsModule(client)

        return UsoniaServer(
            plugins = pluginsModule.plugins,
            server = server,
            logger = logger,
        )
    }
}
