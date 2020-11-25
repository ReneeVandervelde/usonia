package usonia.foundation

/**
 * Defines a room within a site that contains devices.
 *
 * @param id Universally unique identifier.
 * @param name User-friendly name for the room.
 * @param type What the room is used for. Used to determine device behavior.
 * @param adjacentRooms Rooms that neighbor this room
 */
data class Room(
    val id: Uuid,
    val name: String,
    val type: Type = Type.Generic,
    val adjacentRooms: Set<Uuid> = emptySet(),
    val devices: Set<Device> = emptySet(),
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
    }
}
