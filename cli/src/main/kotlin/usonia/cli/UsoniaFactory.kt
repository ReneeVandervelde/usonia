package usonia.cli

import dagger.Reusable
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import usonia.client.HttpClient
import usonia.core.ServerPlugin
import usonia.core.Usonia
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
    ): Usonia {
        val server = KtorWebServer(port, logger)

        return Usonia(
            plugins = plugins,
            server = server,
            logger = logger,
        )
    }

    fun createClient(
        host: String,
        port: Int,
    ) = HttpClient(
        host = host,
        port = port,
        json = json,
        logger = logger,
    )
}
