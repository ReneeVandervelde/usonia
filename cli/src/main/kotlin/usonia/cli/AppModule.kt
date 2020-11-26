package usonia.cli

import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.multibindings.IntoSet
import kimchi.logger.KimchiLogger
import usonia.app.AppPlugin
import usonia.bridge.BridgePlugin
import usonia.core.CorePlugin
import usonia.core.Plugin
import usonia.state.ActionPublisher
import usonia.state.ConfigurationAccess
import usonia.state.EventAccess
import usonia.state.EventPublisher
import usonia.state.memory.InMemoryActionAccess
import usonia.state.memory.InMemoryEventAccess
import javax.inject.Singleton

@Module
object AppModule {
    @Provides
    fun configurationAccess(): ConfigurationAccess {
        return FileConfigAccess()
    }

    @Provides
    @Singleton
    fun inMemoryEvents(
        config: ConfigurationAccess
    ) = InMemoryEventAccess(config)

    @Provides
    @Singleton
    fun eventAccess(
        events: InMemoryEventAccess
    ): EventAccess = events

    @Provides
    @Singleton
    fun eventPublisher(
        events: InMemoryEventAccess
    ): EventPublisher = events

    @Provides
    @Singleton
    fun actionPublisher(): ActionPublisher {
        return InMemoryActionAccess()
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

    @Provides
    @Reusable
    @IntoSet
    fun bridgePlugin(
        eventAccess: EventAccess,
        eventPublisher: EventPublisher,
        logger: KimchiLogger
    ): Plugin = BridgePlugin(
        eventPublisher,
        eventAccess,
        logger
    )
}
