package usonia.cli.server

import dagger.Module
import dagger.Provides
import dagger.Reusable
import kimchi.logger.KimchiLogger
import usonia.frontend.ControlPanelController
import usonia.server.HttpController
import usonia.server.WebServer
import usonia.server.ktor.KtorWebServer
import javax.inject.Singleton

@Module
class ServerModule {
    @Provides
    @Reusable
    fun controllers(): List<HttpController> = listOf(
        ControlPanelController,
        HelloController
    )

    @Provides
    @Singleton
    fun server(
        controllers: @JvmSuppressWildcards List<HttpController>,
        logger: KimchiLogger
    ): WebServer = KtorWebServer(
        httpControllers = controllers,
        logger = logger
    )
}
