package usonia.cli

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.multibindings.IntoSet
import kimchi.logger.KimchiLogger
import usonia.core.state.*
import usonia.hubitat.HubitatPlugin
import usonia.hue.HueBridgePlugin
import usonia.rules.RulesPlugin
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient
import usonia.todoist.TodoistBridgePlugin
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
        client: BackendClient,
        logger: KimchiLogger,
    ): ServerPlugin = WebPlugin(client, logger)

    @Provides
    @Singleton
    fun weatherPlugin(
        client: BackendClient,
        logger: KimchiLogger,
    ) = WeatherPlugin(
        client = client,
        logger = logger,
    )

    @Provides
    @Singleton
    @IntoSet
    fun todoistPlugin(
        client: BackendClient,
        logger: KimchiLogger
    ): ServerPlugin = TodoistBridgePlugin(client, logger)

    @Provides
    @Reusable
    @IntoSet
    fun rulesPlugin(
        client: BackendClient,
        weather: WeatherAccess,
        logger: KimchiLogger,
    ): ServerPlugin = RulesPlugin(client, weather, logger)

    @Provides
    @Reusable
    @IntoSet
    fun bridgePlugin(
        client: BackendClient,
        logger: KimchiLogger
    ): ServerPlugin = HubitatPlugin(
        client = client,
        logger = logger,
    )

    @Provides
    @Reusable
    @IntoSet
    fun huePlugin(
        client: BackendClient,
        logger: KimchiLogger
    ): ServerPlugin = HueBridgePlugin(
        client = client,
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
