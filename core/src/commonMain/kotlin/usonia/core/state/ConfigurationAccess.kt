package usonia.core.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import usonia.foundation.*

/**
 * Provides access to the configuration of the application.
 */
interface ConfigurationAccess {
    /**
     * Configuration key/value pairs for the application.
     */
    val site: Flow<Site>
}

/**
 * Get the latest data available for the configured site.
 */
suspend fun ConfigurationAccess.getSite(): Site = site.first()

/**
 * @see [Site.findDevice]
 */
suspend fun ConfigurationAccess.findDevice(id: Identifier): Device? = site.first().findDevice(id)

/**
 * @see [Site.findDevicesBy]
 */
suspend fun ConfigurationAccess.findDevicesBy(predicate: (Device) -> Boolean) = getSite().findDevicesBy(predicate)

/**
 * @see [Site.findBridgeByServiceTag]
 */
suspend fun ConfigurationAccess.findBridgeByServiceTag(service: String): Bridge? = getSite().findBridgeByServiceTag(service)
