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
    val users: List<User>
)
