package usonia.cli

import dagger.Reusable
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import usonia.client.FrontendClient
import usonia.client.HttpClient
import usonia.server.ServerPlugin
import usonia.server.UsoniaServer
import usonia.server.ktor.KtorWebServer
import javax.inject.Inject

@Reusable
@JvmSuppressWildcards
class UsoniaFactory @Inject constructor(
    private val logger: KimchiLogger,
    private val plugins: Set<ServerPlugin>,
    private val json: Json,
) {
    fun createServer(
        port: Int
    ): UsoniaServer {
        val server = KtorWebServer(port, logger)

        return UsoniaServer(
            plugins = plugins,
            server = server,
            logger = logger,
        )
    }

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
