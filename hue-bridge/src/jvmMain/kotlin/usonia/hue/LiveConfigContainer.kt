package usonia.hue

import com.inkapplications.coroutines.ongoing.map
import inkapplications.shade.structures.AuthToken
import inkapplications.shade.structures.HueConfigurationContainer
import inkapplications.shade.structures.SecurityStrategy
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import usonia.core.state.ConfigurationAccess
import usonia.kotlin.alsoIfNull

/**
 * Loads Hue configurations from the live data of the site configuration.
 */
internal class LiveConfigContainer(
    configurationAccess: ConfigurationAccess,
    scope: CoroutineScope,
    private val logger: KimchiLogger = EmptyLogger,
): HueConfigurationContainer {
    private val bridge = configurationAccess.site
        .map { it.bridges.single { it.service == HUE_SERVICE } }

    override val authToken: StateFlow<AuthToken?> = bridge
        .map { it.parameters[HUE_TOKEN] }
        .map {
            it.alsoIfNull {
                logger.info("No Hue bridges configured. Configure a bridge of type `Hue` in site config.")
            }?.let {
                AuthToken(applicationKey = it)
            }
        }
        .asFlow()
        .stateIn(scope, SharingStarted.Eagerly, null)

    override val hostname: StateFlow<String?> = bridge
        .map {
            it.parameters[HUE_URL].alsoIfNull {
                logger.info("No Hue bridges configured. Configure a bridge of type `Hue` in site config.")
            }
        }
        .asFlow()
        .stateIn(scope, SharingStarted.Eagerly, null)

    override val securityStrategy: StateFlow<SecurityStrategy> = bridge
        .map {
            it.parameters[HUE_URL].alsoIfNull {
                logger.info("No Hue bridges configured. Configure a bridge of type `Hue` in site config.")
            }?.let {
                SecurityStrategy.Insecure(it)
            } ?: SecurityStrategy.PlatformTrust
        }
        .asFlow()
        .stateIn(scope, SharingStarted.Eagerly, SecurityStrategy.PlatformTrust)

    override suspend fun setAuthToken(token: AuthToken?) {
        throw NotImplementedError("Modifying Auth Token Not implemented")
    }

    override suspend fun setHostname(hostname: String?) {
        throw NotImplementedError("Modifying hostname Not implemented")
    }

    override suspend fun setSecurityStrategy(securityStrategy: SecurityStrategy) {
        throw NotImplementedError("Modifying Security Not implemented")
    }
}
