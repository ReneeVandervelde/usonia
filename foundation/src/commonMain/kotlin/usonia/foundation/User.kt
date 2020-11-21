package usonia.foundation

/**
 * A real human occupant.
 */
data class User(
    val id: Uuid,
    val name: String,
    val parameters: ParameterBag
)
