package usonia.cli.client

import dagger.Module
import dagger.Provides
import dagger.Reusable
import kimchi.logger.CompositeLogWriter
import kimchi.logger.ConsolidatedLogger
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import usonia.cli.ColorWriter
import usonia.client.FrontendClient
import usonia.client.HttpClient

/**
 * Dependencies configured for the HTTP Remote Client
 */
@Module
class ClientModule(
    private val host: String,
    private val port: Int,
) {
    @Provides
    @Reusable
    fun createClient(
        json: Json,
        logger: KimchiLogger,
    ): FrontendClient = HttpClient(
        host = host,
        port = port,
        json = json,
        logger = logger,
    )

    @Provides
    @Reusable
    fun clientLogger(): KimchiLogger {
        return setOf(ColorWriter)
            .let(::CompositeLogWriter)
            .let(::ConsolidatedLogger)
    }
}
