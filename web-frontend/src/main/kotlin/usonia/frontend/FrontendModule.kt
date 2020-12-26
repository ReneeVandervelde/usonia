package usonia.frontend

import kimchi.Kimchi
import kimchi.logger.defaultWriter
import kotlinx.browser.window
import usonia.client.UsoniaClient
import usonia.frontend.config.ConfigController
import usonia.frontend.logs.LogController
import usonia.serialization.SerializationModule

object FrontendModule {
    val logger = Kimchi.apply {
        addLog(defaultWriter)
    }

    val client = UsoniaClient(
        host = window.location.host,
        port = window.location.port.takeIf { it.isNotEmpty() }?.toInt() ?: 80,
        json = SerializationModule.json,
        logger = logger,
    )

    val controllers = listOf(
        ConfigController(client, logger),
        LogController(client),
    )
}
