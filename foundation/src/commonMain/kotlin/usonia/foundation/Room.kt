package usonia.foundation

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Defines a room within a site that contains devices.
 *
 * @param id Universally unique identifier.
 * @param name User-friendly name for the room.
 * @param type What the room is used for. Used to determine device behavior.
 * @param adjacentRooms Rooms that neighbor this room
 */
@Serializable
data class Room(
    val id: Identifier,
    val name: String,
    val type: Type = Type.Generic,
    val adjacentRooms: Set<Identifier> = emptySet(),
    val devices: Set<@Contextual Device> = emptySet(),
) {
    enum class Type {
        Bathroom,
        Bedroom,
        Dining,
        Garage,
        Generic,
        Hallway,
        Kitchen,
        LivingRoom,
        Office,
        Storage,
        Utility,
        Greenhouse,
    }

    operator fun contains(id: Identifier): Boolean {
        return id in devices.map { it.id }
    }

    operator fun contains(device: Device): Boolean {
        return device in devices
    }
}
