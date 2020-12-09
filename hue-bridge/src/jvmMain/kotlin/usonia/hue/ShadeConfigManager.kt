package usonia.hue

import inkapplications.shade.Shade
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.collect
import usonia.core.Daemon
import usonia.foundation.Site
import usonia.kotlin.neverEnding
import usonia.state.ConfigurationAccess

/**
 * Updates the Hue baseurl based on site configuration.
 */
internal class ShadeConfigManager(
    private val configurationAccess: ConfigurationAccess,
    private val shade: Shade,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    override suspend fun start() = neverEnding {
        configurationAccess.site.collect { configure(it) }
    }

    private fun configure(site: Site) {
        val bridges = site.bridges.filter { it.service == HUE_SERVICE }

        val host = when (bridges.size) {
            1 -> bridges.single().parameters[HUE_URL]
            0 -> {
                logger.info("No Hue bridges configured. Configure a bridge of type `Hue` in site config.")
                return
            }
            else -> bridges.first()
                .also {
                    logger.warn("Only one Hue bridge can be configured. Using first configuration: <${it.id}>")
                }
                .parameters[HUE_URL]
        } ?: run {
            logger.error("Host not configured in hue parameters. Set it with <${HUE_URL}>")
            return
        }

        shade.setBaseUrl(host)
    }
}
