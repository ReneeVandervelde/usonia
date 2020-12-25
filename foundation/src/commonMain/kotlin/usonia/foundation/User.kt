package usonia.foundation

/**
 * A real human occupant.
 */
data class User(
    val id: Identifier,
    val name: String,
    val parameters: ParameterBag
)
