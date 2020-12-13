package usonia.foundation

/**
 * Represents a location in which devices are controlled. Typically a home.
 *
 * @param id universally unique identifier.
 * @param name User friendly name for the site.
 * @param users People to notify of events in the home.
 */
data class Site(
    val id: Uuid,
    val name: String,
    val users: Set<User>,
    val rooms: Set<Room>,
    val bridges: Set<Bridge>,
    val parameters: ParameterBag,
)

fun Site.findRoomWithDevice(id: Uuid): Room {
    return rooms.single {
        it.devices.singleOrNull { it.id == id } != null
    }
}
