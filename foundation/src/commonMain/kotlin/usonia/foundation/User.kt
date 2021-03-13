package usonia.foundation

import kotlinx.serialization.Serializable

/**
 * A real human occupant.
 */
@Serializable
data class User(
    val id: Identifier,
    val name: String,
    val alertLevel: Action.Alert.Level = Action.Alert.Level.Info,
    val parameters: ParameterBag = emptyMap(),
)
