package usonia.frontend

import kimchi.Kimchi
import kimchi.logger.defaultWriter
import kotlinx.browser.window
import usonia.client.UsoniaClient
import usonia.frontend.config.ConfigController
import usonia.frontend.logs.LogController

object FrontendModule {
    val logger = Kimchi.apply {
        addLog(defaultWriter)
    }

    val client = UsoniaClient(
        window.location.host,
        window.location.port.takeIf { it.isNotEmpty() }?.toInt() ?: 80
    )

    val controllers = listOf(
        ConfigController(client, logger),
        LogController(client),
    )
}
