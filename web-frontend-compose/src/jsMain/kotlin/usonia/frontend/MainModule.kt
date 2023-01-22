package usonia.frontend

import kimchi.Kimchi
import kimchi.logger.defaultWriter
import kotlinx.browser.window
import usonia.client.HttpClient
import usonia.frontend.flags.FlagsSection
import usonia.frontend.logs.LogsSection
import usonia.frontend.metrics.MetricsSection
import usonia.frontend.navigation.NavigationContainer
import usonia.frontend.navigation.NavigationSection
import usonia.kotlin.DefaultScope
import usonia.kotlin.IoScope
import usonia.serialization.SerializationModule

class MainModule {
    val logger = Kimchi.apply {
        addLog(defaultWriter)
    }

    val client = HttpClient(
        host = window.location.hostname,
        port = window.location.port.takeIf { it.isNotEmpty() }?.toInt() ?: 80,
        json = SerializationModule.json,
        logger = logger,
    )

    private val networkScope = IoScope()
    private val renderScope = DefaultScope()

    val sections: List<NavigationSection> = listOf(
        FlagsSection(client, networkScope),
        LogsSection(client),
        MetricsSection(client, logger)
    )

    val mainController = MainController(
        sections = sections,
    )

    val navigationContainer: NavigationContainer = mainController
}
