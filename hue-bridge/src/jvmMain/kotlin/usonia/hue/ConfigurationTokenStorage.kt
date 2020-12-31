package usonia.hue

import inkapplications.shade.auth.TokenStorage
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.state.ConfigurationAccess
import usonia.core.state.findBridgeByServiceTag
import usonia.kotlin.alsoIfNull

/**
 * Retrieves Hue access tokens from the latest site configuration.
 */
internal class ConfigurationTokenStorage(
    private val configurationAccess: ConfigurationAccess,
    private val logger: KimchiLogger = EmptyLogger,
): TokenStorage {
    override suspend fun getToken(): String? {
        return configurationAccess.findBridgeByServiceTag(HUE_SERVICE)
            ?.parameters
            ?.get(HUE_TOKEN)
            .alsoIfNull {
                logger.info("No Hue bridges configured. Configure a bridge of type `Hue` in site config.")
            }
    }

    override suspend fun setToken(token: String?) = throw NotImplementedError("Token is Fixed")
}
