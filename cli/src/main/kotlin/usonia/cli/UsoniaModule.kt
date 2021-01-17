package usonia.cli

import dagger.Module
import dagger.Provides
import dagger.Reusable
import kotlinx.serialization.json.Json
import usonia.core.state.memory.InMemoryActionAccess
import usonia.serialization.SerializationModule
import usonia.server.client.BackendClient
import usonia.server.client.ComposedBackendClient
import usonia.state.MongoModule
import javax.inject.Singleton

@Module
object UsoniaModule {
    @Provides
    @Reusable
    fun json() = SerializationModule.json

    @Provides
    @Singleton
    fun backendClient(
        json: Json,
    ): BackendClient {
        val events = MongoModule().stateAccess()
        val actions = InMemoryActionAccess()
        val config = FileConfigAccess(json)

        return ComposedBackendClient(
            actionAccess = actions,
            actionPublisher = actions,
            eventAccess = events,
            eventPublisher = events,
            configurationAccess = config,
        )
    }
}
