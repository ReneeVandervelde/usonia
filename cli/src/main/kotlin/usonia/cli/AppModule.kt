package usonia.cli

import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.multibindings.IntoSet
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.Flow
import usonia.app.AppPlugin
import usonia.core.CorePlugin
import usonia.core.Plugin
import usonia.foundation.*
import usonia.state.ActionPublisher
import usonia.state.ConfigurationAccess
import usonia.state.EventAccess
import usonia.state.memory.InMemoryEventAccess
import javax.inject.Singleton

@Module
object AppModule {
    @Provides
    fun configurationAccess(): ConfigurationAccess {
        return object: ConfigurationAccess {
            override val parameters: Flow<ParameterBag> get() = TODO()
            override val site: Flow<Site> get() = TODO()
            override val rooms: Flow<Set<Room>> get() = TODO()
            override val devices: Flow<Set<Device>> get() = TODO()
        }
    }

    @Provides
    @Singleton
    fun eventAccess(
        config: ConfigurationAccess
    ): EventAccess {
        return InMemoryEventAccess(config)
    }

    @Provides
    fun actionPublisher(): ActionPublisher {
        return object: ActionPublisher {
            override suspend fun publishAction(action: Action) = TODO()
        }
    }

    @Provides
    @Reusable
    @IntoSet
    fun corePlugin(): Plugin = CorePlugin

    @Provides
    @Reusable
    @IntoSet
    fun appPlugin(
        config: ConfigurationAccess,
        events: EventAccess,
        actions: ActionPublisher,
        logger: KimchiLogger
    ): Plugin = AppPlugin(config, events, actions, logger)
}
