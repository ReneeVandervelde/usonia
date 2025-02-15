package usonia.cli.server

import com.inkapplications.datetime.ZonedClock
import kimchi.logger.CompositeLogWriter
import kimchi.logger.ConsolidatedLogger
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import usonia.celestials.CelestialAccess
import usonia.celestials.CelestialModule
import usonia.cli.ColorWriter
import usonia.core.state.memory.InMemoryActionAccess
import usonia.notion.NotionBridgePlugin
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
class CliServerModule(
    private val json: Json,
    private val clock: ZonedClock = ZonedClock.System,
) {
    val logger: KimchiLogger = setOf(
            LogSocket,
            ColorWriter,
            LogErrorAlerts,
            NotionBridgePlugin.logWriter,
        )
        .let(::CompositeLogWriter)
        .let(::ConsolidatedLogger)

    fun createCelestialsModule(
        client: BackendClient,
    ): CelestialModule {
        return CelestialModule(
            usoniaClient = client,
            clock = clock,
        )
    }

    fun createPluginsModule(
        client: BackendClient,
        celestialAccess: CelestialAccess,
    ) = PluginsModule(client, celestialAccess, logger, json, clock)

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
        val client = databaseBackendClient(databasePath, settingsFile)
        val celestialModule = createCelestialsModule(client)
        val pluginsModule = createPluginsModule(client, celestialModule.celestialAccess)
        val server = KtorWebServer(
            authorization = pluginsModule.serverAuthPlugin.auth,
            port = port,
            logger = logger
        )

        return UsoniaServer(
            plugins = pluginsModule.plugins,
            server = server,
            logger = logger,
        )
    }
}
