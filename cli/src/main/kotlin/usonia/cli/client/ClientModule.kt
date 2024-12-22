package usonia.cli.client

import kimchi.logger.CompositeLogWriter
import kimchi.logger.ConsolidatedLogger
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import regolith.init.InitRunnerCallbacks
import regolith.init.RegolithInitRunner
import usonia.auth.AuthModule
import usonia.cli.ColorWriter
import usonia.client.FrontendClient
import usonia.client.HttpClient

/**
 * Dependencies configured for the HTTP Remote Client
 */
class ClientModule(
    private val json: Json,
    private val clock: Clock = Clock.System,
) {
    val logger = setOf(ColorWriter)
        .let(::CompositeLogWriter)
        .let(::ConsolidatedLogger)

    val initializer = RegolithInitRunner(
        initializers = listOf(
            *AuthModule.initializers.toTypedArray(),
        ),
        callbacks = InitRunnerCallbacks.Empty,
    )

    fun createClient(
        host: String,
        port: Int,
    ): FrontendClient = HttpClient(
        host = host,
        port = port,
        json = json,
        clock = clock,
        authenticationProvider = PropertiesAuthProvider,
        logger = logger,
    )
}
