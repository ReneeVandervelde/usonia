package usonia.cli

import dagger.Reusable
import kimchi.logger.KimchiLogger
import usonia.core.Plugin
import usonia.core.Usonia
import usonia.server.ktor.KtorWebServer
import javax.inject.Inject

@Reusable
@JvmSuppressWildcards
class UsoniaFactory @Inject constructor(
    private val logger: KimchiLogger,
    private val plugins: Set<Plugin>,
) {
    fun create(
        port: Int
    ): Usonia {
        val server = KtorWebServer(port, logger)

        return Usonia(
            plugins = plugins,
            server = server,
            logger = logger,
        )
    }
}
