package usonia.core.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import usonia.foundation.*

/**
 * Provides access to the configuration of the application.
 */
interface ConfigurationAccess {
    /**
     * Site-level device and api configuration for the application.
     */
    val site: Flow<Site>

    /**
     * Arbitrary key/value settings.
     */
    val flags: Flow<Map<String, String?>>

    /**
     * Update site configuration.
     */
    suspend fun updateSite(site: Site)

    /**
     * Set an arbitrary key/value setting.
     */
    suspend fun setFlag(key: String, value: String?)

    /**
     * Remove a key completely from settings.
     */
    suspend fun removeFlag(key: String)
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

suspend fun ConfigurationAccess.getFlag(key: String): String? = flags.firstOrNull()?.get(key)
suspend fun ConfigurationAccess.getBooleanFlag(
    key: String,
    default: Boolean = false
): Boolean = flags.firstOrNull()?.get(key)?.toBoolean() ?: default
