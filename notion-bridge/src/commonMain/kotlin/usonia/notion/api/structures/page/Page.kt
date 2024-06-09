package usonia.notion.api.structures.page

import kotlinx.serialization.Serializable
import usonia.notion.api.structures.Parent
import usonia.notion.api.structures.property.Property
import usonia.notion.api.structures.property.PropertyName

@Serializable
internal data class Page(
    val id: PageId,
    val parent: Parent,
    val properties: Map<PropertyName, Property>,
)
