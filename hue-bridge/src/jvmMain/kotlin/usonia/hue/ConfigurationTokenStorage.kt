package usonia.hue

import inkapplications.shade.auth.TokenStorage
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.state.ConfigurationAccess
import usonia.state.getSite

private const val HUE_TOKEN = "hue.token"

/**
 * Retrieves Hue access tokens from the latest site configuration.
 */
internal class ConfigurationTokenStorage(
    private val configurationAccess: ConfigurationAccess,
    private val logger: KimchiLogger = EmptyLogger,
): TokenStorage {
    override suspend fun getToken() = configurationAccess.getSite()
        .parameters[HUE_TOKEN]
        .also { if (it == null) logger.warn("Hue Token Configuration Missing. Configure `$HUE_TOKEN` in site parameters.") }

    override suspend fun setToken(token: String?) = throw NotImplementedError("Token is Fixed")
}
