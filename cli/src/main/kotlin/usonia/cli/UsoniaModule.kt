package usonia.cli

import dagger.Module
import dagger.Provides
import dagger.Reusable
import usonia.serialization.SerializationModule

/**
 * Shared configuration for both the http client and application server.
 */
@Module
object UsoniaModule {
    @Provides
    @Reusable
    fun json() = SerializationModule.json
}
