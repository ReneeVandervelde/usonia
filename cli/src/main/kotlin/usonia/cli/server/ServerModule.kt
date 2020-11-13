package usonia.cli.server

import dagger.Module
import dagger.Provides
import usonia.server.HttpController
import usonia.server.HttpServer
import javax.inject.Singleton

@Module
class ServerModule {
    @Provides
    @Singleton
    fun controllers():  List<HttpController> = listOf(
        HelloController
    )

    @Provides
    @Singleton
    fun server(controllers: @JvmSuppressWildcards List<HttpController>) = HttpServer(controllers)
}
