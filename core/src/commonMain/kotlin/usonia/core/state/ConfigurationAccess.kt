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
 * Get the latest data available for a device with a given [id].
 */
suspend fun ConfigurationAccess.getDeviceById(id: Uuid): Device? {
    return site.first()
        .rooms
        .flatMap { it.devices }
        .find { it.id == id }
}

/**
 * Get the latest data available for the configured site.
 */
suspend fun ConfigurationAccess.getSite(): Site {
    return site.first()
}

/**
 * Find a site-wide device that matches a [predicate]
 */
suspend fun Site.findDeviceBy(predicate: (Device) -> Boolean): Set<Device> {
    return rooms
        .flatMap { it.devices }
        .filter(predicate)
        .toSet()
}
