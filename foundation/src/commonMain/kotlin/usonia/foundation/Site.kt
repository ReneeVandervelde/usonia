package usonia.foundation

/**
 * Represents a location in which devices are controlled. Typically a home.
 *
 * @param id universally unique identifier.
 * @param name User friendly name for the site.
 */
data class Site(
    val id: Uuid,
    val name: String
)
