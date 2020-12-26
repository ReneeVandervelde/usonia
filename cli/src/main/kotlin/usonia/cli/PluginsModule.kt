package usonia.cli

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.multibindings.IntoSet
import kimchi.logger.KimchiLogger
import usonia.core.ServerPlugin
import usonia.core.state.*
import usonia.hubitat.HubitatPlugin
import usonia.hue.HueBridgePlugin
import usonia.rules.RulesPlugin
import usonia.weather.WeatherAccess
import usonia.weather.WeatherPlugin
import usonia.web.WebPlugin
import javax.inject.Singleton

@Module(includes = [PluginBindings::class])
object PluginsModule {
    @Provides
    @Reusable
    @IntoSet
    fun webPlugin(
        config: ConfigurationAccess,
        eventPublisher: EventPublisher,
        eventAccess: EventAccess,
        actionPublisher: ActionPublisher,
        logger: KimchiLogger,
    ): ServerPlugin = WebPlugin(config, eventPublisher, eventAccess, actionPublisher, logger)

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
    @Reusable
    @IntoSet
    fun rulesPlugin(
        config: ConfigurationAccess,
        events: EventAccess,
        actionPublisher: ActionPublisher,
        actionAccess: ActionAccess,
        weather: WeatherAccess,
        logger: KimchiLogger,
    ): ServerPlugin = RulesPlugin(config, events, actionPublisher, actionAccess, weather, logger)

    @Provides
    @Reusable
    @IntoSet
    fun bridgePlugin(
        config: ConfigurationAccess,
        actionAccess: ActionAccess,
        logger: KimchiLogger
    ): ServerPlugin = HubitatPlugin(
        actionAccess = actionAccess,
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
    ): ServerPlugin = HueBridgePlugin(
        actionAccess = actionAccess,
        configurationAccess = config,
        logger = logger,
    )

    @Provides
    fun weatherAccess(source: WeatherPlugin) = source.weatherAccess
}

@Module
private interface PluginBindings {
    @Binds
    @IntoSet
    fun weatherPlugin(source: WeatherPlugin): ServerPlugin
}
