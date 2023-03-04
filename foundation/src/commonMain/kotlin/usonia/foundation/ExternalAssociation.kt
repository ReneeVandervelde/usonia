package usonia.foundation

import kotlinx.serialization.Serializable

@Serializable
data class ExternalAssociation(
    val context: Identifier,
    val id: Identifier,
) {
    override fun toString(): String = "$id@$context"
}
