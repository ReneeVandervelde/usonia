package usonia.foundation

import kotlinx.serialization.Serializable

/**
 * Represents a location in which devices are controlled. Typically a home.
 *
 * @param id universally unique identifier.
 * @param name User friendly name for the site.
 * @param users People to notify of events in the home.
 */
@Serializable
data class Site(
    val id: Identifier,
    val name: String,
    val users: Set<User> = emptySet(),
    val rooms: Set<Room> = emptySet(),
    val bridges: Set<Bridge> = emptySet(),
    val parameters: ParameterBag = emptyMap(),
)


/**
 * Find a site-wide device that matches a [predicate]
 */
fun Site.findDevicesBy(predicate: (Device) -> Boolean): Set<Device> {
    return rooms
        .flatMap { it.devices }
        .filter(predicate)
        .toSet()
}

/**
 * Find a site-wide device by ID
 */
fun Site.getDevice(id: Identifier): Device {
    val results = rooms.flatMap { it.devices }.filter { it.id == id }
    return when (results.size) {
        0 -> throw IllegalArgumentException("Device not found: $id")
        1 -> results.single()
        else -> throw IllegalArgumentException("Duplicate devices with ID: $id")
    }
}

fun Site.findRoomWithDevice(id: Identifier): Room {
    return rooms.single {
        it.devices.singleOrNull { it.id == id } != null
    }
}

fun Site.findAssociatedBridge(device: Device): Bridge? = bridges
    .filter { it.id == device.parent?.context }
    .let {
        when (it.size) {
            1 -> it.single()
            0 -> null
            else -> throw IllegalArgumentException("Multiple bridges matched id: ${device.parent?.context}")
        }
    }


fun Site.findDeviceBy(predicate: (Device) -> Boolean): Device? = rooms
    .flatMap { it.devices }
    .find(predicate)
