package usonia.cli.server

import dagger.Module
import dagger.Provides
import dagger.Reusable
import kimchi.logger.KimchiLogger
import usonia.frontend.ControlPanelController
import usonia.frontend.LogSocket
import usonia.server.HttpController
import usonia.server.WebServer
import usonia.server.WebSocketController
import usonia.server.ktor.KtorWebServer
import javax.inject.Singleton

@Module
class ServerModule {
    @Provides
    @Singleton
    fun logSocket() = LogSocket()

    @Provides
    @Reusable
    fun controllers(): List<HttpController> = listOf(
        ControlPanelController,
        HelloController,
    )

    @Provides
    @Reusable
    fun sockets(
        logSocket: LogSocket
    ): List<WebSocketController> = listOf(
        logSocket,
    )

    @Provides
    @Singleton
    fun server(
        controllers: @JvmSuppressWildcards List<HttpController>,
        sockets: @JvmSuppressWildcards List<WebSocketController>,
        logger: KimchiLogger
    ): WebServer = KtorWebServer(
        httpControllers = controllers,
        socketControllers = sockets,
        staticResources = listOf(
            "frontend-controls.js"
        ),
        logger = logger
    )
}
