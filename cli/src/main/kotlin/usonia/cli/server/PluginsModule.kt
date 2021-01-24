package usonia.cli.server

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.multibindings.IntoSet
import kimchi.logger.KimchiLogger
import usonia.hubitat.HubitatPlugin
import usonia.hue.HueBridgePlugin
import usonia.rules.RulesPlugin
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient
import usonia.telegram.TelegramBridgePlugin
import usonia.todoist.TodoistBridgePlugin
import usonia.weather.WeatherAccess
import usonia.weather.WeatherPlugin
import usonia.web.WebPlugin

/**
 * Server-side plugins for the Usonia Application.
 */
@Module(includes = [PluginBindings::class, ServerModule::class])
object PluginsModule {
    @Provides
    @Reusable
    @IntoSet
    fun webPlugin(
        client: BackendClient,
        logger: KimchiLogger,
    ): ServerPlugin = WebPlugin(client, logger)

    @Provides
    @ServerScope
    fun weatherPlugin(
        client: BackendClient,
        logger: KimchiLogger,
    ) = WeatherPlugin(
        client = client,
        logger = logger,
    )

    @Provides
    @ServerScope
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
    @Reusable
    @IntoSet
    fun telegram(
        client: BackendClient,
        logger: KimchiLogger,
    ): ServerPlugin = TelegramBridgePlugin(client, logger)

    @Provides
    fun weatherAccess(source: WeatherPlugin) = source.weatherAccess
}

@Module
private interface PluginBindings {
    @Binds
    @IntoSet
    fun weatherPlugin(source: WeatherPlugin): ServerPlugin
}
