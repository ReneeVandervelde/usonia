package usonia.frontend

import kimchi.Kimchi
import kimchi.logger.defaultWriter
import kotlinx.browser.window
import usonia.client.HttpClient
import usonia.frontend.config.ConfigController
import usonia.frontend.flags.FlagController
import usonia.frontend.logs.LogController
import usonia.frontend.metrics.EventMetricsController
import usonia.frontend.metrics.TemperatureMetricsController
import usonia.frontend.users.UserListController
import usonia.serialization.SerializationModule

object FrontendModule {
    val logger = Kimchi.apply {
        addLog(defaultWriter)
    }

    val client = HttpClient(
        host = window.location.hostname,
        port = window.location.port.takeIf { it.isNotEmpty() }?.toInt() ?: 80,
        json = SerializationModule.json,
        logger = logger,
    )

    val controllers = listOf(
        ConfigController(client, logger),
        LogController(client, logger),
        UserListController(client, logger),
        EventMetricsController(client, logger),
        FlagController(client, logger),
        TemperatureMetricsController(client, logger),
    )
}
