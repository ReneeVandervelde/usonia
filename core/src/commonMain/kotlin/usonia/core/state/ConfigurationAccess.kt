package usonia.core.state

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.first
import com.inkapplications.coroutines.ongoing.map
import usonia.foundation.*

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
     * The alarm/security state for the site.
     */
    val securityState: OngoingFlow<SecurityState>

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

    /**
     * Set the security state to [SecurityState.Armed]
     */
    suspend fun armSecurity()
}

val ConfigurationAccess.booleanFlags: OngoingFlow<Map<String, Boolean>> get() = flags
    .map {
        it.filter {
            it.value.equals("true", ignoreCase = true) || it.value.equals("false", ignoreCase = true)
        }.mapValues { it.value!!.toBooleanStrict() }
    }

/**
 * Rooms configured on the site.
 */
val ConfigurationAccess.rooms get() = site.map { it.rooms.toList() }

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

/**
 * Get a bridge config by its ID.
 */
suspend fun ConfigurationAccess.findBridgeById(id: Identifier): Bridge? = getSite().bridges.find { it.id == id }

/**
 * Get the current value of a flag, if set.
 */
suspend fun ConfigurationAccess.getFlag(key: String): String? = flags.first()[key]

/**
 * Get the current security state of the site.
 */
suspend fun ConfigurationAccess.getSecurityState(): SecurityState = securityState.first()

/**
 * Get the current state of a flag, cast as a boolean.
 */
suspend fun ConfigurationAccess.getBooleanFlag(
    key: String,
    default: Boolean = false
): Boolean = flags.first()[key]?.toBoolean() ?: default

/**
 * Change the state of a boolean flag to the opposite of its current state.
 *
 * If no state is set, [default] is used. Flag values are treated as false-y
 * if a non-boolean.
 */
suspend fun ConfigurationAccess.toggleBooleanFlag(
    key: String,
    default: Boolean = false,
) = setFlag(key, !getBooleanFlag(key, default))

/**
 * Set the value of a boolean flag.
 */
suspend fun ConfigurationAccess.setFlag(key: String, value: Boolean) = setFlag(key, value.toString())

suspend fun ConfigurationAccess.findBridgeAuthById(
    id: Identifier
): ServerAuthPsk? = getSite().bridges.find { it.id == id }?.serverToken
