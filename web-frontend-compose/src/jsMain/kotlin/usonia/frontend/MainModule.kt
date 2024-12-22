package usonia.frontend

import kimchi.Kimchi
import kimchi.logger.defaultWriter
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.datetime.Clock
import regolith.init.InitRunner
import regolith.init.RegolithInitRunner
import usonia.auth.AuthModule
import usonia.client.HttpClient
import usonia.frontend.auth.WebAuthentication
import usonia.frontend.configuration.DeviceSection
import usonia.frontend.configuration.RoomSection
import usonia.frontend.flags.FlagsSection
import usonia.frontend.logs.LogsSection
import usonia.frontend.metrics.MetricsSection
import usonia.frontend.navigation.NavigationContainer
import usonia.kotlin.IoScope
import usonia.serialization.SerializationModule

class MainModule {
    val logger = Kimchi.apply {
        addLog(defaultWriter)
    }
    val authentication = WebAuthentication()
    val client = HttpClient(
        authenticationProvider = authentication,
        host = window.location.hostname,
        port = window.location.port.takeIf { it.isNotEmpty() }?.toInt() ?: 80,
        json = SerializationModule.json,
        clock = Clock.System,
        logger = logger,
    )

    val appScope = MainScope()
    private val networkScope = IoScope()
    private val mainController = MainController(listOf(
        FlagsSection(client, networkScope),
        LogsSection(client),
        MetricsSection(client, logger),
        RoomSection(client),
        DeviceSection(client),
    ))

    val navigationContainer: NavigationContainer = mainController

    val init: InitRunner = RegolithInitRunner(
        initializers = listOf(
            *AuthModule.initializers.toTypedArray(),
            AppRunner(
                authentication = authentication,
                navigationContainer = navigationContainer,
            )
        ),
    )
}
