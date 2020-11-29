package usonia.hue

import inkapplications.shade.Shade
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.collect
import usonia.core.Daemon
import usonia.foundation.Site
import usonia.kotlin.neverEnding
import usonia.state.ConfigurationAccess

private const val HUE_HOST = "hue.host"

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
        val host = site.parameters[HUE_HOST] ?: run {
            logger.warn("Hue host configuration missing. Configure `$HUE_HOST` in site parameters.")
            return
        }
        shade.setBaseUrl(host)
    }
}
