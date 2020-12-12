package usonia.cli

import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.multibindings.IntoSet
import kimchi.logger.KimchiLogger
import usonia.hubitat.HubitatPlugin
import usonia.core.Plugin
import usonia.core.state.*
import usonia.hue.HueBridgePlugin
import usonia.rules.RulesPlugin
import usonia.core.state.memory.InMemoryActionAccess
import usonia.core.state.memory.InMemoryEventAccess
import usonia.weather.WeatherPlugin
import usonia.web.WebPlugin
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
    fun webPlugin(
        config: ConfigurationAccess
    ): Plugin = WebPlugin(config)

    @Provides
    @Singleton
    fun weatherPlugin(
        config: ConfigurationAccess,
        logger: KimchiLogger,
    ) = WeatherPlugin(
        config = config,
        logger = logger,
    )

    @Provides
    @IntoSet
    fun weatherPluginBinding(
        weatherPlugin: WeatherPlugin,
    ): Plugin = weatherPlugin

    @Provides
    @Reusable
    @IntoSet
    fun rulesPlugin(
        config: ConfigurationAccess,
        events: EventAccess,
        actionPublisher: ActionPublisher,
        actionAccess: ActionAccess,
        logger: KimchiLogger
    ): Plugin = RulesPlugin(config, events, actionPublisher, actionAccess, logger)

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
    ): Plugin = HubitatPlugin(
        eventPublisher = eventPublisher,
        eventAccess = eventAccess,
        actionAccess = actionAccess,
        actionPublisher = actionPublisher,
        configurationAccess = config,
        logger = logger,
    )

    @Provides
    @Reusable
    @IntoSet
    fun huePlugin(
        config: ConfigurationAccess,
        actionAccess: ActionAccess,
        logger: KimchiLogger
    ): Plugin = HueBridgePlugin(
        actionAccess = actionAccess,
        configurationAccess = config,
        logger = logger,
    )
}
