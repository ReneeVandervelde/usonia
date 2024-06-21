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
 * Collection of all devices in all rooms of a site.
 */
val Site.devices: Set<Device> get() = rooms.flatMap { it.devices }.toSet()

/**
 * Filter a list of devices by those associated with an Entrypoint Fixture.
 */
val Iterable<Device>.entryPoints get() = filter { it.fixture == Fixture.EntryPoint }.toSet()

/**
 * Filter a list of devices by those with a latch capability.
 */
val Iterable<Device>.latchableDevices get() = filter { Event.Latch::class in it.capabilities.events }.toSet()

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
 * Find a single device by an arbitrary [predicate]
 */
fun Site.findDeviceBy(predicate: (Device) -> Boolean): Device? = rooms
    .flatMap { it.devices }
    .find(predicate)

/**
 * Get a site-wide device by ID
 */
fun Site.getDevice(id: Identifier): Device {
    val results = rooms.flatMap { it.devices }.filter { it.id == id }
    return when (results.size) {
        0 -> throw IllegalArgumentException("Device not found: $id")
        1 -> results.single()
        else -> throw IllegalArgumentException("Duplicate devices with ID: $id")
    }
}

/**
 * Find a device by its ID.
 */
fun Site.findDevice(id: Identifier): Device? {
    val results = rooms.flatMap { it.devices }.filter { it.id == id }
    return when (results.size) {
        0 -> null
        1 -> results.single()
        else -> throw IllegalArgumentException("Duplicate devices with ID: $id")
    }
}

/**
 * Find a room by its ID
 */
fun Site.findRoom(id: Identifier): Room? = rooms.find { it.id == id }

/**
 * Find a room by a device ID.
 *
 * @param id The ID of the device to search for.
 * @return The room that contains the device of [id]
 */
fun Site.findRoomContainingDevice(id: Identifier): Room? {
    return rooms.find { room ->
        id in room.devices.map { it.id }
    }
}

/**
 * Find a single bridge by service tag, if it exists.
 *
 * @throws IllegalStateException if more than one bridge with the service tag is configured.
 */
fun Site.findBridgeByServiceTag(service: String): Bridge? {
    val bridges = bridges.filter { it.service == service }
    return when (bridges.size) {
        0 -> null
        1 -> bridges.single()
        else -> throw IllegalStateException("Multiple bridges defined with service ID: $service")
    }
}

/**
 * Find the bridge associated with a device, if it exists.
 */
fun Site.findAssociatedBridge(device: Device): Bridge? = bridges
    .filter { it.id == device.parent?.context }
    .let {
        when (it.size) {
            1 -> it.single()
            0 -> null
            else -> throw IllegalArgumentException("Multiple bridges matched id: ${device.parent?.context}")
        }
    }

/**
 * Find a device by the bridge identifier.
 */
fun Site.findBridgeDevice(bridge: Identifier, device: Identifier): Device? {
    return findDeviceBy { it.parent?.context == bridge && it.parent.id == device }
}
