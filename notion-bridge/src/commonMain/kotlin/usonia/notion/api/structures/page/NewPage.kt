package usonia.notion.api.structures.page

import kotlinx.serialization.Serializable
import usonia.notion.api.structures.Parent
import usonia.notion.api.structures.property.PropertyArgument
import usonia.notion.api.structures.property.PropertyName

@Serializable
internal data class NewPage(
    val parent: Parent,
    val icon: PageIcon? = null,
    val properties: Map<PropertyName, PropertyArgument>,
)
