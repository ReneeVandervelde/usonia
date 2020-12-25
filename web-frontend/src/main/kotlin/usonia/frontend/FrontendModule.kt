package usonia.frontend

import kimchi.Kimchi
import kimchi.logger.defaultWriter
import kotlinx.browser.window
import usonia.client.UsoniaClient
import usonia.frontend.config.ConfigController
import usonia.frontend.logs.LogController
import usonia.hue.HueArchetypes
import usonia.schlage.SchlageArchetypes
import usonia.serialization.SiteSerializer
import usonia.smartthings.SmartThingsArchetypes

object FrontendModule {
    val logger = Kimchi.apply {
        addLog(defaultWriter)
    }

    val archetypes = setOf(
        *SmartThingsArchetypes.ALL.toTypedArray(),
        HueArchetypes.group,
        SchlageArchetypes.connectLock,
    )

    val client = UsoniaClient(
        host = window.location.host,
        port = window.location.port.takeIf { it.isNotEmpty() }?.toInt() ?: 80,
        siteSerializer = SiteSerializer(archetypes),
        logger = logger,
    )

    val controllers = listOf(
        ConfigController(client, logger),
        LogController(client),
    )
}
