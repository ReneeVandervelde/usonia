package usonia.cli.client

import kimchi.logger.CompositeLogWriter
import kimchi.logger.ConsolidatedLogger
import kotlinx.serialization.json.Json
import usonia.cli.ColorWriter
import usonia.client.FrontendClient
import usonia.client.HttpClient

/**
 * Dependencies configured for the HTTP Remote Client
 */
class ClientModule(
    private val json: Json,
) {
    val logger = setOf(ColorWriter)
        .let(::CompositeLogWriter)
        .let(::ConsolidatedLogger)

    fun createClient(
        host: String,
        port: Int,
    ): FrontendClient = HttpClient(
        host = host,
        port = port,
        json = json,
        logger = logger,
    )
}
