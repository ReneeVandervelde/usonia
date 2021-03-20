package usonia.core.state

import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.first

/**
 * Provides access to the configuration of the application.
 */
interface ConfigurationAccess {
    /**
     * Site-level device and api configuration for the application.
     */
    val site: OngoingFlow<Site>

    /**
     * Arbitrary key/value settings.
     */
    val flags: OngoingFlow<Map<String, String?>>

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
suspend fun ConfigurationAccess.findDevice(id: Identifier): Device? = getSite().findDevice(id)

/**
 * Associate all of the adjacent room ID's in a room with their Room object.
 */
suspend fun ConfigurationAccess.findAdjacentRooms(room: Room): Set<Room> {
    val site = getSite()

    return room.adjacentRooms
        .map { site.findRoom(it) }
        .filterNotNull()
        .toSet()
}

/**
 * Look up whether a room has a neighboring room of a specified type.
 *
 * @param room The room to search the adjacent rooms of.
 * @param type The adjacent room type to look for.
 * @return whether [room] has an adjacent room of [type]
 */
suspend fun ConfigurationAccess.hasAdjacentType(room: Room, type: Room.Type): Boolean {
    return type in findAdjacentRooms(room).map { it.type }
}

/**
 * @see [Site.findDevicesBy]
 */
suspend fun ConfigurationAccess.findDevicesBy(predicate: (Device) -> Boolean) = getSite().findDevicesBy(predicate)

/**
 * @see [Site.findBridgeByServiceTag]
 */
suspend fun ConfigurationAccess.findBridgeByServiceTag(service: String): Bridge? = getSite().findBridgeByServiceTag(service)

suspend fun ConfigurationAccess.getFlag(key: String): String? = flags.first()[key]
suspend fun ConfigurationAccess.getBooleanFlag(
    key: String,
    default: Boolean = false
): Boolean = flags.first()[key]?.toBoolean() ?: default
suspend fun ConfigurationAccess.setFlag(key: String, value: Boolean) = setFlag(key, value.toString())
