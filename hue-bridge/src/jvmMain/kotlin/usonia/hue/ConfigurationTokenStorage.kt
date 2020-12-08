package usonia.hue

import inkapplications.shade.auth.TokenStorage
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.foundation.Bridge
import usonia.state.ConfigurationAccess
import usonia.state.getSite

/**
 * Retrieves Hue access tokens from the latest site configuration.
 */
internal class ConfigurationTokenStorage(
    private val configurationAccess: ConfigurationAccess,
    private val logger: KimchiLogger = EmptyLogger,
): TokenStorage {
    override suspend fun getToken(): String? {
        val bridges = configurationAccess.getSite()
            .bridges
            .filterIsInstance<Bridge.Hue>()

        return when (bridges.size) {
            1 -> bridges.single().token
            0 -> null.also {
                logger.info("No Hue bridges configured. Configure a bridge of type `Hue` in site config.")
            }
            else -> bridges.first()
                .also {
                    logger.warn("Only one Hue bridge can be configured. Using first configuration: <${it.id}>")
                }
                .token
        }
    }

    override suspend fun setToken(token: String?) = throw NotImplementedError("Token is Fixed")
}