package usonia.state

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
    val parameters: Flow<ParameterBag>

    /**
     * Changes in configuration for the site.
     */
    val site: Flow<Site>

    /**
     * Changes in configuration for the rooms on site.
     */
    val rooms: Flow<Set<Room>>

    /**
     * Changes in configuration for the devices.
     */
    val devices: Flow<Set<Device>>
}

/**
 * Get the latest data available for a device with a given [id].
 */
suspend fun ConfigurationAccess.getDeviceById(id: Uuid): Device? {
    return devices.first().find { it.id == id }
}

/**
 * Get the latest data available for the configured site.
 */
suspend fun ConfigurationAccess.getSite(): Site {
    return site.first()
}
