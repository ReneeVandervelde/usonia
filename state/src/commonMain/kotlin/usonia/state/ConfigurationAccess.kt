package usonia.state

import kotlinx.coroutines.flow.Flow
import usonia.foundation.Device
import usonia.foundation.Room
import usonia.foundation.Site

/**
 * Provides access to the configuration of the application.
 */
interface ConfigurationAccess {
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
