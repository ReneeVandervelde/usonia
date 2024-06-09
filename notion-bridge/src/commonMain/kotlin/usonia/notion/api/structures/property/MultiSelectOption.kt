package usonia.notion.api.structures.property

import kotlinx.serialization.Serializable

@Serializable
internal data class MultiSelectOption(
    val id: String,
    val name: String,
)
