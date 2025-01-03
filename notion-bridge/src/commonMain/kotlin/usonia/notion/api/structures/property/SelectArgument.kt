package usonia.notion.api.structures.property

import kotlinx.serialization.Serializable

@Serializable
internal data class SelectArgument(
    val id: String? = null,
    val name: String? = null,
)
