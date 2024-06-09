package usonia.notion.api.structures.property

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
internal value class PropertyType(val value: String) {
    companion object {
        val MultiSelect = PropertyType("multi_select")
        val Title = PropertyType("title")
        val RichText = PropertyType("rich_text")
    }
}
