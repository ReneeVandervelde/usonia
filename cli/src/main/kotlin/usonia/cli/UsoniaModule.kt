package usonia.cli

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import kotlinx.serialization.json.Json
import usonia.core.state.*
import usonia.core.state.memory.InMemoryActionAccess
import usonia.core.state.memory.InMemoryEventAccess
import usonia.serialization.SerializationModule
import javax.inject.Singleton

@Module(includes = [UsoniaBindings::class])
object UsoniaModule {
    @Provides
    @Reusable
    fun json() = SerializationModule.json

    @Provides
    @Reusable
    fun configurationAccess(
        json: Json,
    ): ConfigurationAccess {
        return FileConfigAccess(json)
    }

    @Provides
    @Singleton
    fun inMemoryEvents() = InMemoryEventAccess()

    @Provides
    @Singleton
    fun inMemoryActions() = InMemoryActionAccess()
}

@Module
private interface UsoniaBindings {
    @Binds
    fun eventAccess(source: InMemoryEventAccess): EventAccess

    @Binds
    fun eventPublisher(source: InMemoryEventAccess): EventPublisher

    @Binds
    fun actionPublisher(source: InMemoryActionAccess): ActionPublisher

    @Binds
    fun actionAccess(source: InMemoryActionAccess): ActionAccess
}
