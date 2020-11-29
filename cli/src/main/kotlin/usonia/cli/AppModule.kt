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
import usonia.state.*
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
    fun inMemoryActions() = InMemoryActionAccess()

    @Provides
    @Singleton
    fun actionPublisher(
        actions: InMemoryActionAccess
    ): ActionPublisher = actions

    @Provides
    @Singleton
    fun actionAccess(
        actions: InMemoryActionAccess
    ): ActionAccess = actions

    @Provides
    @Reusable
    @IntoSet
    fun corePlugin(
        config: ConfigurationAccess
    ): Plugin = CorePlugin(config)

    @Provides
    @Reusable
    @IntoSet
    fun appPlugin(
        config: ConfigurationAccess,
        events: EventAccess,
        actionPublisher: ActionPublisher,
        actionAccess: ActionAccess,
        logger: KimchiLogger
    ): Plugin = AppPlugin(config, events, actionPublisher, actionAccess, logger)

    @Provides
    @Reusable
    @IntoSet
    fun bridgePlugin(
        eventAccess: EventAccess,
        eventPublisher: EventPublisher,
        config: ConfigurationAccess,
        actionAccess: ActionAccess,
        actionPublisher: ActionPublisher,
        logger: KimchiLogger
    ): Plugin = BridgePlugin(
        eventPublisher = eventPublisher,
        eventAccess = eventAccess,
        actionAccess = actionAccess,
        actionPublisher = actionPublisher,
        configurationAccess = config,
        logger = logger,
    )
}
