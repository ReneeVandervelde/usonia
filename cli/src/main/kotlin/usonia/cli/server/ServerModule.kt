package usonia.cli.server

import dagger.Module
import dagger.Provides
import dagger.Reusable
import usonia.server.HttpController
import usonia.server.WebServer
import javax.inject.Singleton

@Module
class ServerModule {
    @Provides
    @Reusable
    fun controllers(): List<HttpController> = listOf(
        HelloController
    )

    @Provides
    @Singleton
    fun server(
        controllers: @JvmSuppressWildcards List<HttpController>
    ) = WebServer(
        httpControllers = controllers,
    )
}
