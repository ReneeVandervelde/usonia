package usonia.notion.api.structures.property

import kotlinx.serialization.Serializable

@Serializable
internal data class MultiSelectArgument(
    val id: String? = null,
    val name: String? = null,
)
